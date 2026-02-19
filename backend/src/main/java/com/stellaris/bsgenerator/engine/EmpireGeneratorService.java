package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Generates random valid Stellaris empires by progressively selecting
 * compatible components using weighted randomness.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmpireGeneratorService {

    private static final int ETHICS_BUDGET = 3;
    private static final int CIVIC_COUNT = 2;
    private static final double GESTALT_CHANCE = 0.30;
    private static final List<String> LEADER_CLASSES = List.of("official", "commander", "scientist");

    private static final int SECONDARY_SPECIES_BUDGET = 2;
    private static final int SECONDARY_SPECIES_MAX_PICKS = 5;
    private static final Map<String, Integer> ENFORCED_TRAIT_COSTS = Map.of(
            "trait_syncretic_proles", 1,
            "trait_cybernetic", 0,
            "trait_hive_mind", 0
    );

    /** Enforced species traits from origins — these are free (cost 0) and don't consume the trait budget. */
    private static final Map<String, Integer> ORIGIN_ENFORCED_TRAIT_COSTS = Map.of(
            "trait_perfected_genes", 0,
            "trait_necrophage", 0,
            "trait_malleable_genes", 0
    );

    /** Luminary leader trait budget and max picks for origin_legendary_leader. */
    private static final int LUMINARY_BUDGET = 1;
    private static final int LUMINARY_MAX_PICKS = 3;

    /** Origins that fix the homeworld planet type (skip random selection). */
    private static final Map<String, String> ORIGIN_FIXED_PLANETS = Map.ofEntries(
            Map.entry("origin_life_seeded", "pc_gaia"),
            Map.entry("origin_void_dwellers", "pc_habitat"),
            Map.entry("origin_post_apocalyptic", "pc_nuked"),
            Map.entry("origin_machine", "pc_machine"),
            Map.entry("origin_remnants", "pc_relic"),
            Map.entry("origin_shattered_ring", "pc_ringworld_habitable"),
            Map.entry("origin_ocean_paradise", "pc_ocean"),
            Map.entry("origin_red_giant", "pc_volcanic"),
            Map.entry("origin_cosmic_dawn", "pc_volcanic"),
            Map.entry("origin_void_machines", "pc_habitat")
    );

    private final CompatibilityFilterService filterService;
    private final RequirementEvaluator evaluator;

    private final Random random = new Random();

    /**
     * Generate a complete random empire.
     *
     * @return a valid GeneratedEmpire
     * @throws GenerationException if no valid combination can be found
     */
    public GeneratedEmpire generate() {
        // 1. Pick ethics (total cost = 3)
        List<Ethic> ethics = pickEthics();
        var state = EmpireState.empty()
                .withEthics(toIdSet(ethics));

        // 2. Pick compatible authority
        Authority authority = pickAuthority(state);
        state = state.withAuthority(authority.id());

        // 3. Pick species archetype and species class (before origin/civics so they can validate)
        SpeciesArchetype archetype = pickArchetype(state);
        state = state.withSpeciesArchetype(archetype.id());
        String speciesClass = pickSpeciesClass(archetype);
        state = state.withSpeciesClass(speciesClass);

        // 4. Pick compatible civics (now with archetype in state)
        List<Civic> civics = pickCivics(state, CIVIC_COUNT);
        state = state.withCivics(toIdSet(civics));

        // 5. Pick compatible origin (now with archetype + species class in state)
        Origin origin = pickOrigin(state);
        state = state.withOrigin(origin.id());

        // 5b. Post-validate civics against origin (some origins restrict civics)
        if (!civicsStillValid(civics, state)) {
            civics = pickCivics(state, CIVIC_COUNT);
            state = state.withCivics(toIdSet(civics));
        }

        // 6. Collect all enforced trait IDs (origin + civics)
        var allEnforcedTraitIds = new ArrayList<>(origin.enforcedTraitIds());
        for (var civic : civics) {
            for (var traitId : civic.enforcedTraitIds()) {
                if (!allEnforcedTraitIds.contains(traitId)) {
                    allEnforcedTraitIds.add(traitId);
                }
            }
        }

        // Pick compatible traits within budget, excluding enforced trait IDs from the random pool
        List<SpeciesTrait> traits = pickTraits(archetype, state, allEnforcedTraitIds);

        // 6b. Prepend enforced species traits (display their real cost; budget excludes them)
        traits = prependEnforcedTraits(allEnforcedTraitIds, traits);

        // Budget: only count non-enforced traits (enforced traits are free regardless of displayed cost)
        var enforcedSet = new HashSet<>(allEnforcedTraitIds);
        int pointsUsed = traits.stream().filter(t -> !enforcedSet.contains(t.id())).mapToInt(SpeciesTrait::cost).sum();

        // 7. Pick homeworld planet (or use origin-fixed, constrained by traits + species class)
        PlanetClass homeworld = pickHomeworld(origin, traits, speciesClass);

        // 7b. Determine habitability preference
        PlanetClass habPref = pickHabitabilityPreference(origin, homeworld);

        // 8. Pick random shipset
        GraphicalCulture shipset = pickShipset();

        // 9. Pick leader class and starting trait(s)
        String leaderClass = pickLeaderClass();
        List<StartingRulerTrait> leaderTraits = pickLeaderTraits(leaderClass, state);

        // 10. Generate secondary species if origin/civic requires one
        SecondarySpecies secondarySpecies = generateSecondarySpecies(origin, civics, speciesClass);

        log.info("Generated empire: ethics={}, authority={}, civics={}, origin={}, archetype={}, speciesClass={}, traits={} ({}/{}pts), homeworld={}, habPref={}, shipset={}, leader={}/{}, secondarySpecies={}",
                ethics.stream().map(Ethic::id).toList(),
                authority.id(),
                civics.stream().map(Civic::id).toList(),
                origin.id(),
                archetype.id(), speciesClass,
                traits.stream().map(SpeciesTrait::id).toList(),
                pointsUsed, archetype.traitPoints(),
                homeworld.id(), habPref.id(), shipset.id(),
                leaderClass, leaderTraits.stream().map(StartingRulerTrait::id).toList(),
                secondarySpecies != null ? secondarySpecies.speciesClass() : "none");

        return new GeneratedEmpire(ethics, authority, civics, origin,
                archetype, speciesClass, traits, pointsUsed, archetype.traitPoints(),
                homeworld, habPref, shipset, leaderClass, leaderTraits, secondarySpecies);
    }

    /**
     * Generate a secondary species if the origin or any civic requires one.
     * Origin is checked first, then civics (first match wins).
     */
    SecondarySpecies generateSecondarySpecies(Origin origin, List<Civic> civics, String primarySpeciesClass) {
        // Find the first secondary species config: origin first, then civics
        SecondarySpeciesConfig config = origin.secondarySpecies();
        if (config == null) {
            config = civics.stream()
                    .map(Civic::secondarySpecies)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (config == null) return null;

        // Pick a biological species class different from primary
        var bioClasses = filterService.getSpeciesClassesForArchetype("BIOLOGICAL");
        var candidates = bioClasses.stream()
                .filter(sc -> !sc.id().equals(primarySpeciesClass))
                .toList();
        if (candidates.isEmpty()) candidates = bioClasses;
        String secondaryClass = candidates.get(random.nextInt(candidates.size())).id();

        // Resolve enforced traits — use real trait data when available to get correct opposites and iconPath
        List<SpeciesTrait> enforcedTraits = config.enforcedTraitIds().stream()
                .map(traitId -> {
                    int costOverride = ENFORCED_TRAIT_COSTS.getOrDefault(traitId, 0);
                    var real = filterService.findTraitById(traitId);
                    if (real != null) {
                        return new SpeciesTrait(
                                real.id(), costOverride,
                                real.allowedArchetypes(), real.allowedSpeciesClasses(),
                                real.allowedPlanetClasses(), real.opposites(),
                                true, false, real.dlcRequirement(), real.tags(),
                                real.allowedOrigins(), real.forbiddenOrigins(),
                                real.allowedCivics(), real.forbiddenCivics(),
                                real.allowedEthics(), real.forbiddenEthics(),
                                real.iconPath());
                    }
                    // Fallback stub (trait not in creation pool)
                    return new SpeciesTrait(
                            traitId, costOverride,
                            List.of("BIOLOGICAL"), List.of(), List.of(), List.of(),
                            true, false, null, List.of(),
                            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null);
                })
                .toList();

        int enforcedCost = enforcedTraits.stream().mapToInt(SpeciesTrait::cost).sum();
        int remainingBudget = SECONDARY_SPECIES_BUDGET - enforcedCost;
        int remainingPicks = SECONDARY_SPECIES_MAX_PICKS - enforcedTraits.size();

        // Build a minimal state for trait filtering (secondary species is always biological)
        var secondaryState = EmpireState.empty()
                .withSpeciesArchetype("BIOLOGICAL")
                .withSpeciesClass(secondaryClass);

        // Pick additional traits from compatible pool
        var available = filterService.getCompatibleTraits("BIOLOGICAL", secondaryState);
        // Remove enforced traits from available pool
        var enforcedIds = new HashSet<>(config.enforcedTraitIds());
        available = available.stream()
                .filter(t -> !enforcedIds.contains(t.id()))
                .toList();

        List<SpeciesTrait> additionalTraits = new ArrayList<>();
        Set<String> pickedIds = new HashSet<>(enforcedIds);
        Set<String> excludedByOpposites = new HashSet<>();
        // Collect opposites from enforced traits
        for (var enforced : enforcedTraits) {
            excludedByOpposites.addAll(enforced.opposites());
        }
        int pointsSpent = enforcedCost;

        var shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, random);

        for (var trait : shuffled) {
            if (additionalTraits.size() >= remainingPicks) break;
            if (pickedIds.contains(trait.id())) continue;
            if (excludedByOpposites.contains(trait.id())) continue;

            int newTotal = pointsSpent + trait.cost();
            if (newTotal > SECONDARY_SPECIES_BUDGET) continue;
            if (newTotal < 0) continue;

            additionalTraits.add(trait);
            pickedIds.add(trait.id());
            pointsSpent = newTotal;
            excludedByOpposites.addAll(trait.opposites());
        }

        return new SecondarySpecies(
                config.title(),
                secondaryClass,
                enforcedTraits,
                additionalTraits,
                pointsSpent,
                SECONDARY_SPECIES_BUDGET,
                SECONDARY_SPECIES_MAX_PICKS
        );
    }

    private List<Ethic> pickEthics() {
        // ~15% chance to generate a gestalt consciousness empire
        if (random.nextDouble() < GESTALT_CHANCE) {
            var gestalt = filterService.getGestaltEthic();
            if (gestalt != null) {
                return List.of(gestalt);
            }
        }

        var regularEthics = filterService.getRegularEthics();

        // Split into fanatic (cost 2) and regular (cost 1)
        var fanaticEthics = regularEthics.stream().filter(Ethic::isFanatic).toList();
        var normalEthics = regularEthics.stream().filter(e -> !e.isFanatic()).toList();

        // 50/50 chance: fanatic+regular or 3x regular
        if (!fanaticEthics.isEmpty() && random.nextBoolean()) {
            // Fanatic (cost 2) + regular (cost 1)
            var fanatic = WeightedRandom.select(fanaticEthics, Ethic::randomWeight, random);
            // Regular must be from a different axis than the fanatic
            var compatible = normalEthics.stream()
                    .filter(e -> !isSameAxis(e, fanatic))
                    .toList();
            if (!compatible.isEmpty()) {
                var regular = WeightedRandom.select(compatible, Ethic::randomWeight, random);
                return List.of(fanatic, regular);
            }
        }

        // Three regular ethics (cost 1 each), all from different axes
        return pickThreeRegularEthics(normalEthics);
    }

    private List<Ethic> pickThreeRegularEthics(List<Ethic> normalEthics) {
        List<Ethic> picked = new ArrayList<>();
        List<Ethic> remaining = new ArrayList<>(normalEthics);

        for (int i = 0; i < 3 && !remaining.isEmpty(); i++) {
            var choice = WeightedRandom.select(remaining, Ethic::randomWeight, random);
            picked.add(choice);
            // Remove same-axis ethics (the chosen one and its fanatic variant share an axis)
            remaining.removeIf(e -> isSameAxis(e, choice));
        }

        if (picked.size() < 3) {
            throw new GenerationException("Could not pick 3 compatible ethics");
        }
        return picked;
    }

    /**
     * Check if two ethics are on the same axis (e.g., authoritarian/egalitarian).
     * Two ethics are on the same axis if:
     * - They share the same category (e.g., "col" for authoritarian/egalitarian), or
     * - One is a variant of the other (fanatic/regular link)
     */
    private boolean isSameAxis(Ethic a, Ethic b) {
        if (a.id().equals(b.id())) return true;
        // Fanatic/regular variant link
        if (a.id().equals(b.regularVariant()) || a.id().equals(b.fanaticVariant())) return true;
        if (b.id().equals(a.regularVariant()) || b.id().equals(a.fanaticVariant())) return true;
        // Same category = same axis (authoritarian/egalitarian both have category "col")
        if (a.category() != null && a.category().equals(b.category())) return true;
        return false;
    }

    private Authority pickAuthority(EmpireState state) {
        var compatible = filterService.getCompatibleAuthorities(state);
        if (compatible.isEmpty()) {
            throw new GenerationException("No compatible authorities for ethics: " + state.ethics());
        }
        return WeightedRandom.select(compatible, Authority::randomWeight, random);
    }

    private List<Civic> pickCivics(EmpireState state, int count) {
        List<Civic> picked = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            var compatible = filterService.getCompatibleCivics(state);
            if (compatible.isEmpty()) {
                throw new GenerationException("No compatible civics for current state (picked " + picked.size() + "/" + count + ")");
            }
            var civic = WeightedRandom.select(compatible, Civic::randomWeight, random);
            picked.add(civic);
            // Update state with newly selected civic
            var newCivics = new HashSet<>(state.civics());
            newCivics.add(civic.id());
            state = state.withCivics(newCivics);
        }

        return picked;
    }

    /**
     * Stratified promotion chances used in {@link #pickOrigin}.
     *
     * <p>Three promotion tiers, checked sequentially. Once a tier fires the selection is complete.
     * <ul>
     *   <li>Tier 1 — class-restricted: origins requiring a specific species class (cosmic_dawn, mindwardens, fruitful).</li>
     *   <li>Tier 2 — authority + graphical-culture: hive + biogenesis origins (wilderness). Only fires in hive empire pools.</li>
     *   <li>Tier 3 — gestalt-exclusive: machine/hive-pool-only origins. Only fires when such origins are in the compatible pool,
     *       which only happens in machine (~15%) or hive (~15%) empire configs.</li>
     * </ul>
     */
    private static final double CLASS_RESTRICTED_PROMO    = 0.25; // cosmic_dawn / fruitful / mindwardens
    private static final double AUTH_GRAPHIC_PROMO        = 0.45; // wilderness (hive + biogenesis)
    private static final double GESTALT_EXCLUSIVE_PROMO   = 0.35; // machine/hive-pool-only origins

    /**
     * Per-origin weight overrides for origins that are chronically under-threshold but resist
     * automated detection (no single detectable positive requirement, or too many civic exclusions).
     * Checked first in {@link #originRarityWeight} before any category-based logic.
     */
    private static final Map<String, Integer> ORIGIN_WEIGHT_OVERRIDES = Map.of(
            "origin_cybernetic_creed",    6, // spiritualist-only + heavy civic exclusions (3x not enough)
            "origin_legendary_leader",    6, // auth_dictatorial-only in small pool (4x not enough)
            "origin_hegemon",             2, // no positive requirements, excluded from xenophobe+egalitarian
            "origin_synthetic_fertility", 2  // no positive requirements, excluded from spiritualist+many civics
    );

    /**
     * Origins that are exclusively available in machine intelligence (~15%) or hive mind (~15%) empire pools.
     * These need a dedicated promotion tier because they compete with many unrestricted origins in a small pool.
     */
    private static final Set<String> GESTALT_EXCLUSIVE_ORIGINS = Set.of(
            "origin_machine",
            "origin_void_machines",
            "origin_ocean_machines",
            "origin_subterranean_machines",
            "origin_post_apocalyptic_machines",
            "origin_arc_welders",
            "origin_progenitor_hive",
            "origin_tree_of_life"
    );

    private Origin pickOrigin(EmpireState state) {
        var compatible = filterService.getCompatibleOrigins(state);
        if (compatible.isEmpty()) {
            throw new GenerationException("No compatible origins for current state");
        }

        // Tier 1 — class-restricted: origins requiring a specific species class (cosmic_dawn → INF,
        // mindwardens → MINDWARDEN, fruitful → FUN/PLANT). Species class is chosen before origin,
        // so the compatible pool is already filtered to only origins matching the current class.
        // Only fires in biological empire configs where such class-restricted origins exist.
        var classRestricted = compatible.stream()
                .filter(o -> collectPossibleCategories(o.possible()).contains(RequirementCategory.SPECIES_CLASS))
                .toList();
        if (!classRestricted.isEmpty() && random.nextDouble() < CLASS_RESTRICTED_PROMO) {
            return classRestricted.get(random.nextInt(classRestricted.size()));
        }

        // Tier 2 — authority + graphical-culture restricted (e.g., wilderness = hive + biogenesis).
        // Uses POSITIVE requirement detection: only fires when both authority AND graphical_culture
        // are hard requirements (must-have), not just negative exclusions.
        var authGraphicRestricted = compatible.stream()
                .filter(o -> hasPositiveRequirement(o.possible(), RequirementCategory.AUTHORITY)
                        && hasPositiveRequirement(o.possible(), RequirementCategory.GRAPHICAL_CULTURE))
                .toList();
        if (!authGraphicRestricted.isEmpty() && random.nextDouble() < AUTH_GRAPHIC_PROMO) {
            return authGraphicRestricted.get(random.nextInt(authGraphicRestricted.size()));
        }

        // Tier 3 — gestalt-exclusive: origins that only exist in machine or hive empire pools.
        // Only fires when such origins are in the compatible pool (i.e., only in machine/hive empires).
        // This ensures machine variants and hive-specific origins aren't buried by the large unrestricted pool.
        var gestaltExclusive = compatible.stream()
                .filter(o -> GESTALT_EXCLUSIVE_ORIGINS.contains(o.id()))
                .toList();
        if (!gestaltExclusive.isEmpty() && random.nextDouble() < GESTALT_EXCLUSIVE_PROMO) {
            return gestaltExclusive.get(random.nextInt(gestaltExclusive.size()));
        }

        // General pool: rarity-weighted by restriction breadth.
        return WeightedRandom.select(compatible, this::originRarityWeight, random);
    }

    /**
     * Computes a rarity weight for the general-pool path in {@link #pickOrigin}.
     * Only POSITIVE requirements (must-have restrictions) are used for boosting —
     * negative boilerplate exclusions (NOT hive_mind, NOT MACHINE, etc.) are ignored to
     * prevent pool inflation from common boilerplate.
     *
     * <p>Weight table (highest precedence first):
     * <ul>
     *   <li>5x — gestalt-exclusive set: machine/hive-pool-only origins (tier 3 promotion also covers these)</li>
     *   <li>4x — species-class required (positive): cosmic_dawn→INF, fruitful→FUN/PLANT</li>
     *   <li>4x — authority + graphical-culture required (positive): wilderness (hive+biogenesis); tier 2 also covers</li>
     *   <li>4x — authority required (positive): legendary_leader→dictatorial</li>
     *   <li>3x — ethics required (positive): cybernetic_creed→spiritualist, mechanists→materialist, shroudwalker/unplugged→spiritualist/militarist</li>
     *   <li>2x — archetype required (positive, non-machine): origin_lithoid (LITHOID archetype) — in ~50% regular pools</li>
     *   <li>1x — no positive requirement (unrestricted or only negative exclusions)</li>
     * </ul>
     */
    private int originRarityWeight(Origin origin) {
        boolean classRestricted   = collectPossibleCategories(origin.possible()).contains(RequirementCategory.SPECIES_CLASS);
        boolean positiveArchetype = hasPositiveRequirement(origin.possible(), RequirementCategory.SPECIES_ARCHETYPE);
        boolean positiveAuthority = hasPositiveRequirement(origin.possible(), RequirementCategory.AUTHORITY);
        boolean positiveGraphic   = hasPositiveRequirement(origin.possible(), RequirementCategory.GRAPHICAL_CULTURE);
        boolean ethicsRestricted  = hasPositiveRequirement(origin.possible(), RequirementCategory.ETHICS);

        // Per-origin overrides for chronically under-threshold origins.
        var override = ORIGIN_WEIGHT_OVERRIDES.get(origin.id());
        if (override != null) return override;

        // Gestalt-exclusive origins appear only in machine (~15%) or hive (~15%) pools.
        // Tier 3 also boosts these; the 5x fallback covers the 65% of cases tier 3 doesn't fire.
        if (GESTALT_EXCLUSIVE_ORIGINS.contains(origin.id()))  return 5;
        if (classRestricted)                                   return 4;
        if (positiveAuthority && positiveGraphic)              return 4;
        if (positiveAuthority)                                 return 4; // dictatorial/machine/hive required
        if (ethicsRestricted)                                  return 3; // spiritualist/materialist/militarist required
        if (positiveArchetype)                                 return 2; // LITHOID required — in 50% of regular pools
        return 1;
    }

    /**
     * Returns true if the given category in the block has at least one POSITIVE requirement —
     * a {@link Requirement.Value} (exact match) or {@link Requirement.Or} (any-of match).
     * Pure NOT/NOR exclusions (boilerplate like "NOT hive_mind", "NOT MACHINE") return false.
     */
    private boolean hasPositiveRequirement(RequirementBlock block, RequirementCategory category) {
        if (block == null) return false;
        var reqs = block.categories().getOrDefault(category, List.of());
        return reqs.stream().anyMatch(r -> r instanceof Requirement.Value || r instanceof Requirement.Or);
    }

    /** Collects all RequirementCategory keys present in a possible/potential block (including cross-category OR branches). */
    private Set<RequirementCategory> collectPossibleCategories(RequirementBlock block) {
        if (block == null) return Set.of();
        var cats = new HashSet<>(block.categories().keySet());
        for (var orBranch : block.crossCategoryOrs()) {
            cats.addAll(orBranch.keySet());
        }
        return cats;
    }

    private SpeciesArchetype pickArchetype(EmpireState state) {
        var archetypes = filterService.getSelectableArchetypes();

        // For gestalt empires, filter by authority requirements
        if (state.ethics().contains("ethic_gestalt_consciousness")) {
            if ("auth_machine_intelligence".equals(state.authority())) {
                // Machine intelligence needs MACHINE or ROBOT archetype
                archetypes = archetypes.stream().filter(SpeciesArchetype::robotic).toList();
            } else {
                // Hive mind needs non-robotic archetype
                archetypes = archetypes.stream().filter(a -> !a.robotic()).toList();
            }
        } else {
            // Regular empires use non-robotic archetypes
            archetypes = archetypes.stream().filter(a -> !a.robotic()).toList();
        }

        if (archetypes.isEmpty()) {
            throw new GenerationException("No compatible species archetypes");
        }

        // Uniform random for archetypes (no weight field)
        return archetypes.get(random.nextInt(archetypes.size()));
    }

    // Per-class weights for species classes that gate exclusively restricted origins.
    // Classes not listed default to 1x weight.
    private static final Map<String, Integer> SPECIES_CLASS_WEIGHTS = Map.of(
            "INF",           8,  // Infernals  — sole class for origin_cosmic_dawn
            "MINDWARDEN",    8,  // The Shroud — sole class for origin_mindwardens / progenitor_hive
            "FUN",           7,  // Fungoid    — gates origin_fruitful
            "PLANT",         7,  // Plantoid   — gates origin_fruitful, origin_tree_of_life
            "BIOGENESIS_01", 4   // BioGenesis — gates biogenesis-restricted origins
    );

    private String pickSpeciesClass(SpeciesArchetype archetype) {
        var classes = filterService.getSpeciesClassesForArchetype(archetype.id());
        if (classes.isEmpty()) {
            // Fallback: use archetype id as species class (e.g., MACHINE archetype → MACHINE class)
            return archetype.id();
        }
        return WeightedRandom.select(classes,
                sc -> SPECIES_CLASS_WEIGHTS.getOrDefault(sc.id(), 1),
                random).id();
    }

    private List<SpeciesTrait> pickTraits(SpeciesArchetype archetype, EmpireState state, List<String> excludeIds) {
        var available = filterService.getCompatibleTraits(archetype.id(), state);
        int budget = archetype.traitPoints();
        int maxTraits = archetype.maxTraits();

        // Exclude origin enforced trait IDs from the random pool
        var excludeSet = new HashSet<>(excludeIds);

        List<SpeciesTrait> picked = new ArrayList<>();
        Set<String> pickedIds = new HashSet<>();
        Set<String> excludedByOpposites = new HashSet<>();
        int pointsSpent = 0;

        // Shuffle to add randomness (traits don't have random_weight)
        var shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, random);

        for (var trait : shuffled) {
            if (picked.size() >= maxTraits) break;

            // Skip origin enforced traits (they're added separately)
            if (excludeSet.contains(trait.id())) continue;

            // Skip if already picked or excluded by opposites
            if (pickedIds.contains(trait.id())) continue;
            if (excludedByOpposites.contains(trait.id())) continue;

            // Check budget: positive traits cost points, negative traits give points back
            int newTotal = pointsSpent + trait.cost();
            if (newTotal > budget) continue; // Can't afford
            if (newTotal < 0) continue; // Too many negative traits

            picked.add(trait);
            pickedIds.add(trait.id());
            pointsSpent = newTotal;

            // Exclude opposites
            excludedByOpposites.addAll(trait.opposites());
        }

        return picked;
    }

    /**
     * Prepend origin enforced species traits to the picked traits list.
     * Enforced traits are free (cost 0) and created as stubs since they have initial=no.
     */
    /** Costs for civic-enforced traits (e.g., trait_aquatic from civic_anglers is free). */
    private static final Map<String, Integer> CIVIC_ENFORCED_TRAIT_COSTS = Map.of(
            "trait_aquatic", 0,
            "trait_robot_aquatic", 0,
            "trait_storm_touched", 0,
            "trait_tankbound", 0,
            "trait_stargazer", 0
    );

    private List<SpeciesTrait> prependEnforcedTraits(List<String> enforcedTraitIds, List<SpeciesTrait> pickedTraits) {
        if (enforcedTraitIds.isEmpty()) return pickedTraits;

        List<SpeciesTrait> result = new ArrayList<>();
        for (var traitId : enforcedTraitIds) {
            var realTrait = filterService.findTraitById(traitId);
            if (realTrait != null) {
                // Use real trait data including real cost (for display) — budget calc excludes enforced traits separately
                result.add(new SpeciesTrait(
                        realTrait.id(), realTrait.cost(),
                        realTrait.allowedArchetypes(), realTrait.allowedSpeciesClasses(),
                        realTrait.allowedPlanetClasses(), realTrait.opposites(),
                        true, false, realTrait.dlcRequirement(), realTrait.tags(),
                        realTrait.allowedOrigins(), realTrait.forbiddenOrigins(),
                        realTrait.allowedCivics(), realTrait.forbiddenCivics(),
                        realTrait.allowedEthics(), realTrait.forbiddenEthics(),
                        realTrait.iconPath()));
            } else {
                // Stub for initial=no traits (necrophage, perfected genes, etc.) — cost is 0 for display too
                result.add(new SpeciesTrait(
                        traitId, 0,
                        List.of(), List.of(), List.of(), List.of(),
                        true, false, null, List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null));
            }
        }
        result.addAll(pickedTraits);
        return result;
    }

    /** Cold planet types that Infernal species cannot inhabit. */
    private static final Set<String> INF_REMOVED_PLANETS = Set.of("pc_arctic", "pc_alpine", "pc_tundra");

    PlanetClass pickHomeworld(Origin origin, List<SpeciesTrait> traits, String speciesClass) {
        String fixedPlanet = ORIGIN_FIXED_PLANETS.get(origin.id());
        if (fixedPlanet != null) {
            return new PlanetClass(fixedPlanet, "fixed");
        }

        var planets = new ArrayList<>(filterService.getHabitablePlanetClasses());

        // Infernal species: add volcanic, remove cold worlds
        if ("INF".equals(speciesClass)) {
            if (planets.stream().noneMatch(p -> "pc_volcanic".equals(p.id()))) {
                planets.add(new PlanetClass("pc_volcanic", "volcanic"));
            }
            planets.removeIf(p -> INF_REMOVED_PLANETS.contains(p.id()));
        }

        // Constrain by trait allowed_planet_classes (e.g., Aquatic → pc_ocean only)
        Set<String> traitPlanetRestriction = collectTraitPlanetClasses(traits);
        if (!traitPlanetRestriction.isEmpty()) {
            planets = new ArrayList<>(planets.stream()
                    .filter(p -> traitPlanetRestriction.contains(p.id()))
                    .toList());
        }

        if (planets.isEmpty()) {
            throw new GenerationException("No habitable planet classes available");
        }
        return planets.get(random.nextInt(planets.size()));
    }

    /**
     * Determine habitability preference for the species.
     * - If origin explicitly sets habitability_preference → fixed to that
     * - If origin has a fixed homeworld but no hab pref (Remnants, Post-Apocalyptic) → random standard type
     * - Otherwise → same as homeworld
     */
    PlanetClass pickHabitabilityPreference(Origin origin, PlanetClass homeworld) {
        // Origin explicitly defines habitability preference
        if (origin.habitabilityPreference() != null) {
            String habPrefId = origin.habitabilityPreference();
            return filterService.getHabitablePlanetClasses().stream()
                    .filter(p -> p.id().equals(habPrefId))
                    .findFirst()
                    .orElse(new PlanetClass(habPrefId, "fixed"));
        }

        // Origin has fixed homeworld but no hab pref → random from standard types
        if (ORIGIN_FIXED_PLANETS.containsKey(origin.id())) {
            var standard = filterService.getHabitablePlanetClasses();
            if (!standard.isEmpty()) {
                return standard.get(random.nextInt(standard.size()));
            }
        }

        // Default: same as homeworld
        return homeworld;
    }

    /**
     * Collect the intersection of allowed_planet_classes from all traits that have restrictions.
     * If multiple traits restrict planet classes, the homeworld must satisfy ALL of them.
     */
    Set<String> collectTraitPlanetClasses(List<SpeciesTrait> traits) {
        Set<String> result = null;
        for (var trait : traits) {
            if (!trait.allowedPlanetClasses().isEmpty()) {
                if (result == null) {
                    result = new HashSet<>(trait.allowedPlanetClasses());
                } else {
                    result.retainAll(trait.allowedPlanetClasses());
                }
            }
        }
        return result != null ? result : Set.of();
    }

    /**
     * Build the full species trait list (enforced + random) for the given state.
     * Package-private so RerollService can call it when regenerating traits after an origin reroll.
     */
    List<SpeciesTrait> buildSpeciesTraits(SpeciesArchetype archetype, EmpireState state, Origin origin, List<Civic> civics) {
        var allEnforcedTraitIds = collectEnforcedTraitIds(origin, civics);
        List<SpeciesTrait> traits = pickTraits(archetype, state, allEnforcedTraitIds);
        return prependEnforcedTraits(allEnforcedTraitIds, traits);
    }

    List<String> collectEnforcedTraitIds(Origin origin, List<Civic> civics) {
        var result = new ArrayList<>(origin.enforcedTraitIds());
        for (var civic : civics) {
            for (var traitId : civic.enforcedTraitIds()) {
                if (!result.contains(traitId)) result.add(traitId);
            }
        }
        return result;
    }

    private GraphicalCulture pickShipset() {
        var shipsets = filterService.getSelectableShipsets();
        if (shipsets.isEmpty()) {
            throw new GenerationException("No selectable shipsets available");
        }
        return shipsets.get(random.nextInt(shipsets.size()));
    }

    private String pickLeaderClass() {
        return LEADER_CLASSES.get(random.nextInt(LEADER_CLASSES.size()));
    }

    /**
     * Pick leader traits. For origin_legendary_leader (luminary mode), picks multiple traits
     * within a point budget. For regular origins, picks 0 or 1 trait.
     */
    List<StartingRulerTrait> pickLeaderTraits(String leaderClass, EmpireState state) {
        var compatible = filterService.getCompatibleRulerTraits(leaderClass, state);
        if (compatible.isEmpty()) {
            return List.of();
        }

        boolean isLuminary = "origin_legendary_leader".equals(state.origin());
        if (isLuminary) {
            return pickLuminaryTraits(compatible);
        }

        // Regular: pick 0 or 1 trait
        var trait = compatible.get(random.nextInt(compatible.size()));
        return List.of(trait);
    }

    /**
     * Pick luminary leader traits: budget=1, max 3 picks (up to 2 positive + 1 negative).
     * Respects opposites and ethics-based filtering (already done by filterService).
     */
    private List<StartingRulerTrait> pickLuminaryTraits(List<StartingRulerTrait> compatible) {
        var positive = compatible.stream().filter(t -> t.cost() > 0).toList();
        var negative = compatible.stream().filter(t -> t.cost() < 0).toList();

        List<StartingRulerTrait> picked = new ArrayList<>();
        Set<String> pickedIds = new HashSet<>();
        Set<String> excludedByOpposites = new HashSet<>();
        int pointsSpent = 0;

        // Shuffle both pools
        var shuffledPositive = new ArrayList<>(positive);
        var shuffledNegative = new ArrayList<>(negative);
        Collections.shuffle(shuffledPositive, random);
        Collections.shuffle(shuffledNegative, random);

        // Try to pick positive traits first, then optionally a negative
        var allShuffled = new ArrayList<StartingRulerTrait>();
        allShuffled.addAll(shuffledPositive);
        allShuffled.addAll(shuffledNegative);

        for (var trait : allShuffled) {
            if (picked.size() >= LUMINARY_MAX_PICKS) break;
            if (pickedIds.contains(trait.id())) continue;
            if (excludedByOpposites.contains(trait.id())) continue;

            int newTotal = pointsSpent + trait.cost();
            if (newTotal > LUMINARY_BUDGET) continue;
            if (newTotal < 0) continue;

            picked.add(trait);
            pickedIds.add(trait.id());
            pointsSpent = newTotal;
            excludedByOpposites.addAll(trait.opposites());
        }

        return picked;
    }

    private boolean civicsStillValid(List<Civic> civics, EmpireState state) {
        for (var civic : civics) {
            if (!evaluator.evaluateBoth(civic.potential(), civic.possible(), state)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> toIdSet(List<? extends Record> entities) {
        var set = new HashSet<String>();
        for (var entity : entities) {
            if (entity instanceof Ethic e) set.add(e.id());
            else if (entity instanceof Civic c) set.add(c.id());
        }
        return set;
    }
}
