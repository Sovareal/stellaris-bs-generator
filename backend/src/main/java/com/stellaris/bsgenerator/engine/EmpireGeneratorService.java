package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.*;
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
    private static final double GESTALT_CHANCE = 0.20;
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
            Map.entry("origin_cosmic_dawn", "pc_volcanic")
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

        // 3. Pick compatible civics (2)
        List<Civic> civics = pickCivics(state, CIVIC_COUNT);
        state = state.withCivics(toIdSet(civics));

        // 4. Pick compatible origin
        Origin origin = pickOrigin(state);
        state = state.withOrigin(origin.id());

        // 5. Pick species archetype and species class
        SpeciesArchetype archetype = pickArchetype(state);
        state = state.withSpeciesArchetype(archetype.id());
        String speciesClass = pickSpeciesClass(archetype);
        state = state.withSpeciesClass(speciesClass);

        // 5b. Post-validate civics against archetype/species class
        // Some civics have species_archetype requirements that couldn't be checked earlier
        if (!civicsStillValid(civics, state)) {
            civics = pickCivics(state, CIVIC_COUNT);
            state = state.withCivics(toIdSet(civics));
        }

        // 6. Pick compatible traits within budget (filtered by species class)
        // Exclude origin enforced trait IDs from the random pool
        List<SpeciesTrait> traits = pickTraits(archetype, state, origin.enforcedTraitIds());

        // 6b. Prepend origin enforced species traits (free, not from normal pool)
        traits = prependEnforcedTraits(origin, traits);

        int pointsUsed = traits.stream().mapToInt(SpeciesTrait::cost).sum();

        // 7. Pick homeworld planet (or use origin-fixed, constrained by traits + species class)
        PlanetClass homeworld = pickHomeworld(origin, traits, speciesClass);

        // 8. Pick random shipset
        GraphicalCulture shipset = pickShipset();

        // 9. Pick leader class and starting trait(s)
        String leaderClass = pickLeaderClass();
        List<StartingRulerTrait> leaderTraits = pickLeaderTraits(leaderClass, state);

        // 10. Generate secondary species if origin/civic requires one
        SecondarySpecies secondarySpecies = generateSecondarySpecies(origin, civics, speciesClass);

        log.info("Generated empire: ethics={}, authority={}, civics={}, origin={}, archetype={}, speciesClass={}, traits={} ({}/{}pts), homeworld={}, shipset={}, leader={}/{}, secondarySpecies={}",
                ethics.stream().map(Ethic::id).toList(),
                authority.id(),
                civics.stream().map(Civic::id).toList(),
                origin.id(),
                archetype.id(), speciesClass,
                traits.stream().map(SpeciesTrait::id).toList(),
                pointsUsed, archetype.traitPoints(),
                homeworld.id(), shipset.id(),
                leaderClass, leaderTraits.stream().map(StartingRulerTrait::id).toList(),
                secondarySpecies != null ? secondarySpecies.speciesClass() : "none");

        return new GeneratedEmpire(ethics, authority, civics, origin,
                archetype, speciesClass, traits, pointsUsed, archetype.traitPoints(),
                homeworld, shipset, leaderClass, leaderTraits, secondarySpecies);
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

        // Resolve enforced traits
        List<SpeciesTrait> enforcedTraits = config.enforcedTraitIds().stream()
                .map(traitId -> new SpeciesTrait(
                        traitId,
                        ENFORCED_TRAIT_COSTS.getOrDefault(traitId, 0),
                        List.of("BIOLOGICAL"), List.of(), List.of(), List.of(),
                        true, false, null, List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
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

    private Origin pickOrigin(EmpireState state) {
        var compatible = filterService.getCompatibleOrigins(state);
        if (compatible.isEmpty()) {
            throw new GenerationException("No compatible origins for current state");
        }
        // Uniform random: all compatible origins get equal chance
        return compatible.get(random.nextInt(compatible.size()));
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

    private String pickSpeciesClass(SpeciesArchetype archetype) {
        var classes = filterService.getSpeciesClassesForArchetype(archetype.id());
        if (classes.isEmpty()) {
            // Fallback: use archetype id as species class (e.g., MACHINE archetype → MACHINE class)
            return archetype.id();
        }
        return classes.get(random.nextInt(classes.size())).id();
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
    private List<SpeciesTrait> prependEnforcedTraits(Origin origin, List<SpeciesTrait> pickedTraits) {
        if (origin.enforcedTraitIds().isEmpty()) return pickedTraits;

        List<SpeciesTrait> result = new ArrayList<>();
        for (var traitId : origin.enforcedTraitIds()) {
            int cost = ORIGIN_ENFORCED_TRAIT_COSTS.getOrDefault(traitId, 0);
            result.add(new SpeciesTrait(
                    traitId, cost,
                    List.of(), List.of(), List.of(), List.of(),
                    true, false, null, List.of(),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));
        }
        result.addAll(pickedTraits);
        return result;
    }

    /** Cold planet types that Infernal species cannot inhabit. */
    private static final Set<String> INF_REMOVED_PLANETS = Set.of("pc_arctic", "pc_alpine", "pc_tundra");

    private PlanetClass pickHomeworld(Origin origin, List<SpeciesTrait> traits, String speciesClass) {
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
     * Collect the intersection of allowed_planet_classes from all traits that have restrictions.
     * If multiple traits restrict planet classes, the homeworld must satisfy ALL of them.
     */
    private Set<String> collectTraitPlanetClasses(List<SpeciesTrait> traits) {
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
