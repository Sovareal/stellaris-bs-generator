package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Handles rerolling individual categories of a generated empire.
 * Only one reroll is allowed per generation session.
 * The rerolled component must be compatible with all locked (non-rerolled) selections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RerollService {

    private static final int MAX_REROLL_ATTEMPTS = 50;

    private final CompatibilityFilterService filterService;
    private final RequirementEvaluator evaluator;
    private final EmpireGeneratorService generatorService;

    private final Random random = new Random();

    /**
     * Reroll a specific category, returning an updated empire.
     *
     * @throws GenerationException if no valid replacement can be found
     * @throws IllegalStateException if the reroll has already been used
     */
    public GeneratedEmpire reroll(GenerationSession session, RerollCategory category) {
        if (!session.canReroll()) {
            throw new IllegalStateException("Reroll already used for this generation");
        }

        var empire = session.getEmpire();
        var updated = switch (category) {
            case ETHICS -> rerollEthics(empire);
            case AUTHORITY -> rerollAuthority(empire);
            case CIVIC1 -> rerollCivic(empire, 0);
            case CIVIC2 -> rerollCivic(empire, 1);
            case ORIGIN -> rerollOrigin(empire);
            case TRAITS -> rerollTraits(empire);
            case TRAIT_SINGLE -> throw new IllegalArgumentException("Use rerollSingleTrait() for TRAIT_SINGLE");
            case HOMEWORLD -> rerollHomeworld(empire);
            case SHIPSET -> rerollShipset(empire);
            case LEADER -> rerollLeader(empire);
            case SECONDARY_SPECIES -> rerollSecondarySpecies(empire);
        };

        session.markRerolled();
        session.setEmpire(updated);

        log.info("Rerolled {}: {}", category, describeChange(empire, updated, category));
        return updated;
    }

    private GeneratedEmpire rerollEthics(GeneratedEmpire empire) {
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            var newEmpire = generatorService.generate();
            var candidateEthics = newEmpire.ethics();

            var state = EmpireState.empty()
                    .withEthics(toEthicIds(candidateEthics))
                    .withSpeciesArchetype(empire.speciesArchetype().id())
                    .withSpeciesClass(empire.speciesClass());
            if (!evaluator.evaluateBoth(empire.authority().potential(), empire.authority().possible(), state)) {
                continue;
            }

            state = state.withAuthority(empire.authority().id());
            boolean civicsCompatible = true;
            for (var civic : empire.civics()) {
                if (!evaluator.evaluateBoth(civic.potential(), civic.possible(), state)) {
                    civicsCompatible = false;
                    break;
                }
            }
            if (!civicsCompatible) continue;

            state = state.withCivics(toCivicIds(empire.civics()));
            if (!evaluator.evaluateBoth(empire.origin().potential(), empire.origin().possible(), state)) {
                continue;
            }

            if (toEthicIds(candidateEthics).equals(toEthicIds(empire.ethics()))) continue;

            return copyWith(empire, b -> b.ethics = candidateEthics);
        }
        // GC empires: no non-GC ethics can coexist with gestalt authorities.
        // Perform a full political regime change: keep species/traits/homeworld, replace political layer.
        if (empire.ethics().stream().anyMatch(Ethic::isGestalt)) {
            return performRegimeChange(empire);
        }
        throw new GenerationException("Could not find compatible ethics for reroll");
    }

    /**
     * Full political regime change for GC empires rerolling ethics.
     * Generates a fresh non-GC empire and returns it (uses reroll slot).
     */
    private GeneratedEmpire performRegimeChange(GeneratedEmpire empire) {
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            var candidate = generatorService.generate();
            if (candidate.ethics().stream().noneMatch(Ethic::isGestalt)) {
                return candidate;
            }
        }
        throw new GenerationException("Could not perform regime change from Gestalt Consciousness");
    }

    private GeneratedEmpire rerollAuthority(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass());
        var compatible = filterService.getCompatibleAuthorities(state).stream()
                .filter(a -> !a.id().equals(empire.authority().id()))
                .filter(a -> {
                    var withAuth = state.withAuthority(a.id());
                    for (var civic : empire.civics()) {
                        if (!evaluator.evaluateBoth(civic.potential(), civic.possible(), withAuth)) return false;
                    }
                    var withCivics = withAuth.withCivics(toCivicIds(empire.civics()));
                    return evaluator.evaluateBoth(empire.origin().potential(), empire.origin().possible(), withCivics);
                }).toList();

        if (compatible.isEmpty()) {
            // Gestalt empires: switching hive↔machine requires archetype change.
            // Generate a fresh empire with the other gestalt authority type.
            if (empire.authority().isGestalt()) {
                return performGestaltSwitch(empire);
            }
            throw new GenerationException("No alternative authorities compatible with current empire");
        }

        var newAuth = WeightedRandom.select(compatible, Authority::randomWeight, random);
        return copyWith(empire, b -> b.authority = newAuth);
    }

    /**
     * Switches gestalt type (hive mind ↔ machine intelligence).
     * Since this requires a different species archetype, generates a full fresh empire of the target type.
     */
    private GeneratedEmpire performGestaltSwitch(GeneratedEmpire empire) {
        String targetAuthId = "auth_hive_mind".equals(empire.authority().id())
                ? "auth_machine_intelligence"
                : "auth_hive_mind";
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            var candidate = generatorService.generate();
            if (targetAuthId.equals(candidate.authority().id())) {
                return candidate;
            }
        }
        throw new GenerationException("Could not switch gestalt type to " + targetAuthId);
    }

    private GeneratedEmpire rerollCivic(GeneratedEmpire empire, int index) {
        var otherCivicId = empire.civics().get(1 - index).id();
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass())
                .withCivics(Set.of(otherCivicId));

        var compatible = filterService.getCompatibleCivics(state).stream()
                .filter(c -> !c.id().equals(empire.civics().get(index).id()))
                .toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative civics compatible with current empire");
        }

        var newCivic = WeightedRandom.select(compatible, Civic::randomWeight, random);
        var newCivics = new ArrayList<>(empire.civics());
        newCivics.set(index, newCivic);
        var civicsList = List.copyOf(newCivics);
        // Re-generate secondary species when civics change (may gain or lose secondary species from civic)
        var newSecondary = generatorService.generateSecondarySpecies(empire.origin(), civicsList, empire.speciesClass());
        return copyWith(empire, b -> {
            b.civics = civicsList;
            b.secondarySpecies = newSecondary;
        });
    }

    private GeneratedEmpire rerollOrigin(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass())
                .withCivics(toCivicIds(empire.civics()));

        var compatible = filterService.getCompatibleOrigins(state).stream()
                .filter(o -> !o.id().equals(empire.origin().id()))
                .toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative origins compatible with current empire");
        }

        // Use uniform random — matches pickOrigin() (Phase 11.3 fix, now applied to reroll too)
        var newOrigin = compatible.get(random.nextInt(compatible.size()));
        var stateWithOrigin = state.withOrigin(newOrigin.id());

        // Re-generate secondary species when origin changes
        var newSecondary = generatorService.generateSecondarySpecies(newOrigin, empire.civics(), empire.speciesClass());

        // Regenerate species traits: drop old origin's enforced traits, add new origin's enforced traits
        var newTraits = generatorService.buildSpeciesTraits(empire.speciesArchetype(), stateWithOrigin, newOrigin, empire.civics());
        // Budget: only non-enforced traits count (enforced display real cost but are free)
        var newEnforcedIds = new HashSet<>(generatorService.collectEnforcedTraitIds(newOrigin, empire.civics()));
        int newPointsUsed = newTraits.stream().filter(t -> !newEnforcedIds.contains(t.id())).mapToInt(SpeciesTrait::cost).sum();

        // Regenerate leader traits: origin change may affect valid trait pool (e.g., Treasure Hunters → other)
        var newLeaderTraits = generatorService.pickLeaderTraits(empire.leaderClass(), stateWithOrigin);

        // Regenerate homeworld and hab pref: origin change affects fixed planets (e.g., Void Dwellers → Habitat)
        var newHomeworld = generatorService.pickHomeworld(newOrigin, newTraits, empire.speciesClass());
        var newHabPref = generatorService.pickHabitabilityPreference(newOrigin, newHomeworld);

        return copyWith(empire, b -> {
            b.origin = newOrigin;
            b.secondarySpecies = newSecondary;
            b.speciesTraits = newTraits;
            b.traitPointsUsed = newPointsUsed;
            b.traitPointsBudget = empire.speciesArchetype().traitPoints();
            b.leaderTraits = newLeaderTraits;
            b.homeworld = newHomeworld;
            b.habitabilityPreference = newHabPref;
        });
    }

    private GeneratedEmpire rerollSecondarySpecies(GeneratedEmpire empire) {
        if (empire.secondarySpecies() == null) {
            throw new GenerationException("No secondary species to reroll");
        }
        var newSecondary = generatorService.generateSecondarySpecies(empire.origin(), empire.civics(), empire.speciesClass());
        if (newSecondary == null) {
            throw new GenerationException("Failed to generate secondary species");
        }
        return copyWith(empire, b -> b.secondarySpecies = newSecondary);
    }

    /**
     * Reroll a single non-enforced species trait, replacing it with a compatible alternative.
     * Respects the remaining trait budget and the opposites of kept traits.
     */
    public GeneratedEmpire rerollSingleTrait(GenerationSession session, String targetTraitId) {
        if (!session.canReroll()) {
            throw new IllegalStateException("Reroll already used for this generation");
        }
        var empire = session.getEmpire();

        // Enforced traits (from origin + civics) cannot be individually rerolled
        var enforcedIds = new HashSet<>(generatorService.collectEnforcedTraitIds(empire.origin(), empire.civics()));
        if (enforcedIds.contains(targetTraitId)) {
            throw new GenerationException("Cannot reroll an enforced trait: " + targetTraitId);
        }

        var currentTraits = empire.speciesTraits();
        if (currentTraits.stream().noneMatch(t -> t.id().equals(targetTraitId))) {
            throw new GenerationException("Trait not found in empire: " + targetTraitId);
        }

        // Traits that will be kept (everything except the target)
        var remainingTraits = currentTraits.stream()
                .filter(t -> !t.id().equals(targetTraitId))
                .toList();

        // Exclusion set: kept trait IDs + their opposites + the target itself (prevent no-op)
        var excludedIds = new HashSet<String>();
        excludedIds.add(targetTraitId);
        for (var t : remainingTraits) {
            excludedIds.add(t.id());
            excludedIds.addAll(t.opposites());
        }

        // Remaining budget = total budget minus the cost of kept non-enforced traits
        int budget = empire.traitPointsBudget();
        int spentByKept = remainingTraits.stream()
                .filter(t -> !enforcedIds.contains(t.id()))
                .mapToInt(SpeciesTrait::cost)
                .sum();
        int availableBudget = budget - spentByKept;

        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id())
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass());

        var candidates = filterService.getCompatibleTraits(empire.speciesArchetype().id(), state).stream()
                .filter(t -> !excludedIds.contains(t.id()))
                .filter(t -> t.cost() <= availableBudget)
                .toList();

        if (candidates.isEmpty()) {
            throw new GenerationException("No compatible replacement trait found for: " + targetTraitId);
        }

        var replacement = candidates.get(random.nextInt(candidates.size()));

        // Replace the target in-place to preserve trait order
        var newTraits = new ArrayList<>(currentTraits);
        for (int i = 0; i < newTraits.size(); i++) {
            if (newTraits.get(i).id().equals(targetTraitId)) {
                newTraits.set(i, replacement);
                break;
            }
        }
        var newTraitList = List.copyOf(newTraits);

        // Points used = sum of non-enforced trait costs
        int newPointsUsed = newTraitList.stream()
                .filter(t -> !enforcedIds.contains(t.id()))
                .mapToInt(SpeciesTrait::cost)
                .sum();

        // Re-derive homeworld if trait planet constraints changed (e.g., Aquatic added/removed)
        var newPlanetConstraint = generatorService.collectTraitPlanetClasses(newTraitList);
        var oldPlanetConstraint = generatorService.collectTraitPlanetClasses(currentTraits);
        PlanetClass newHomeworld = empire.homeworld();
        PlanetClass newHabPref = empire.habitabilityPreference();
        if (!newPlanetConstraint.equals(oldPlanetConstraint)) {
            newHomeworld = generatorService.pickHomeworld(empire.origin(), newTraitList, empire.speciesClass());
            newHabPref = generatorService.pickHabitabilityPreference(empire.origin(), newHomeworld);
        }
        final var finalHomeworld = newHomeworld;
        final var finalHabPref = newHabPref;
        final int finalPointsUsed = newPointsUsed;

        var updated = copyWith(empire, b -> {
            b.speciesTraits = newTraitList;
            b.traitPointsUsed = finalPointsUsed;
            b.homeworld = finalHomeworld;
            b.habitabilityPreference = finalHabPref;
        });

        session.markRerolled();
        session.setEmpire(updated);

        log.info("Single-trait reroll: {} → {}", targetTraitId, replacement.id());
        return updated;
    }

    private GeneratedEmpire rerollTraits(GeneratedEmpire empire) {
        var archetype = empire.speciesArchetype();
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id())
                .withSpeciesArchetype(archetype.id())
                .withSpeciesClass(empire.speciesClass());
        var available = filterService.getCompatibleTraits(archetype.id(), state);
        int budget = archetype.traitPoints();
        int maxTraits = archetype.maxTraits();

        // Preserve origin + civic enforced traits
        var enforcedIds = new HashSet<>(empire.origin().enforcedTraitIds());
        for (var civic : empire.civics()) {
            enforcedIds.addAll(civic.enforcedTraitIds());
        }
        List<SpeciesTrait> enforced = empire.speciesTraits().stream()
                .filter(t -> enforcedIds.contains(t.id()))
                .toList();

        List<SpeciesTrait> picked = new ArrayList<>(enforced);
        Set<String> pickedIds = new HashSet<>(enforcedIds);
        Set<String> excludedByOpposites = new HashSet<>();
        int pointsSpent = 0; // Enforced traits are free (cost 0)

        var shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, random);

        for (var trait : shuffled) {
            if (picked.size() >= maxTraits) break;
            if (pickedIds.contains(trait.id())) continue;
            if (excludedByOpposites.contains(trait.id())) continue;

            int newTotal = pointsSpent + trait.cost();
            if (newTotal > budget) continue;
            if (newTotal < 0) continue;

            picked.add(trait);
            pickedIds.add(trait.id());
            pointsSpent = newTotal;
            excludedByOpposites.addAll(trait.opposites());
        }

        int finalPointsSpent = pointsSpent;
        var newTraitList = List.copyOf(picked);

        // Re-derive homeworld if trait planet constraints changed (e.g., Aquatic added/removed)
        var newPlanetConstraint = generatorService.collectTraitPlanetClasses(newTraitList);
        var oldPlanetConstraint = generatorService.collectTraitPlanetClasses(empire.speciesTraits());
        PlanetClass newHomeworld = empire.homeworld();
        PlanetClass newHabPref = empire.habitabilityPreference();
        if (!newPlanetConstraint.equals(oldPlanetConstraint)) {
            newHomeworld = generatorService.pickHomeworld(empire.origin(), newTraitList, empire.speciesClass());
            newHabPref = generatorService.pickHabitabilityPreference(empire.origin(), newHomeworld);
        }
        final var finalHomeworld = newHomeworld;
        final var finalHabPref = newHabPref;

        return copyWith(empire, b -> {
            b.speciesTraits = newTraitList;
            b.traitPointsUsed = finalPointsSpent;
            b.traitPointsBudget = budget;
            b.homeworld = finalHomeworld;
            b.habitabilityPreference = finalHabPref;
        });
    }

    /** Cold planet types that Infernal species cannot inhabit. */
    private static final Set<String> INF_REMOVED_PLANETS = Set.of("pc_arctic", "pc_alpine", "pc_tundra");

    private GeneratedEmpire rerollHomeworld(GeneratedEmpire empire) {
        var planets = new ArrayList<>(filterService.getHabitablePlanetClasses().stream()
                .filter(p -> !p.id().equals(empire.homeworld().id()))
                .toList());

        // Infernal species: add volcanic, remove cold worlds
        if ("INF".equals(empire.speciesClass())) {
            if (planets.stream().noneMatch(p -> "pc_volcanic".equals(p.id()))) {
                planets.add(new PlanetClass("pc_volcanic", "volcanic"));
            }
            planets.removeIf(p -> INF_REMOVED_PLANETS.contains(p.id()));
        }

        // Constrain by trait allowed_planet_classes (e.g., Aquatic → pc_ocean only)
        Set<String> traitPlanetRestriction = generatorService.collectTraitPlanetClasses(empire.speciesTraits());
        if (!traitPlanetRestriction.isEmpty()) {
            planets = new ArrayList<>(planets.stream()
                    .filter(p -> traitPlanetRestriction.contains(p.id()))
                    .toList());
        }

        if (planets.isEmpty()) {
            throw new GenerationException("No alternative homeworld planets available");
        }

        var newPlanet = planets.get(random.nextInt(planets.size()));
        // Hab pref follows homeworld unless origin fixes it
        var newHabPref = empire.origin().habitabilityPreference() != null
                ? empire.habitabilityPreference()
                : newPlanet;
        return copyWith(empire, b -> {
            b.homeworld = newPlanet;
            b.habitabilityPreference = newHabPref;
        });
    }

    private GeneratedEmpire rerollShipset(GeneratedEmpire empire) {
        var shipsets = filterService.getSelectableShipsets().stream()
                .filter(s -> !s.id().equals(empire.shipset().id()))
                .toList();

        if (shipsets.isEmpty()) {
            throw new GenerationException("No alternative shipsets available");
        }

        var newShipset = shipsets.get(random.nextInt(shipsets.size()));
        return copyWith(empire, b -> b.shipset = newShipset);
    }

    private GeneratedEmpire rerollLeader(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id());

        // Pick a potentially different leader class
        var leaderClasses = List.of("official", "commander", "scientist");
        var newClass = leaderClasses.get(random.nextInt(leaderClasses.size()));

        List<StartingRulerTrait> newTraits = generatorService.pickLeaderTraits(newClass, state);
        return copyWith(empire, b -> {
            b.leaderClass = newClass;
            b.leaderTraits = newTraits;
        });
    }

    // --- Helper to build empire copies with selective changes ---

    private static class EmpireBuilder {
        List<Ethic> ethics;
        Authority authority;
        List<Civic> civics;
        Origin origin;
        SpeciesArchetype speciesArchetype;
        String speciesClass;
        List<SpeciesTrait> speciesTraits;
        int traitPointsUsed;
        int traitPointsBudget;
        PlanetClass homeworld;
        PlanetClass habitabilityPreference;
        GraphicalCulture shipset;
        String leaderClass;
        List<StartingRulerTrait> leaderTraits;
        SecondarySpecies secondarySpecies;

        EmpireBuilder(GeneratedEmpire e) {
            this.ethics = e.ethics();
            this.authority = e.authority();
            this.civics = e.civics();
            this.origin = e.origin();
            this.speciesArchetype = e.speciesArchetype();
            this.speciesClass = e.speciesClass();
            this.speciesTraits = e.speciesTraits();
            this.traitPointsUsed = e.traitPointsUsed();
            this.traitPointsBudget = e.traitPointsBudget();
            this.homeworld = e.homeworld();
            this.habitabilityPreference = e.habitabilityPreference();
            this.shipset = e.shipset();
            this.leaderClass = e.leaderClass();
            this.leaderTraits = e.leaderTraits();
            this.secondarySpecies = e.secondarySpecies();
        }

        GeneratedEmpire build() {
            return new GeneratedEmpire(ethics, authority, civics, origin,
                    speciesArchetype, speciesClass, speciesTraits, traitPointsUsed, traitPointsBudget,
                    homeworld, habitabilityPreference, shipset, leaderClass, leaderTraits, secondarySpecies);
        }
    }

    private GeneratedEmpire copyWith(GeneratedEmpire empire, java.util.function.Consumer<EmpireBuilder> mutator) {
        var builder = new EmpireBuilder(empire);
        mutator.accept(builder);
        return builder.build();
    }

    // --- ID extraction helpers ---

    private Set<String> toEthicIds(List<Ethic> ethics) {
        var set = new HashSet<String>();
        for (var e : ethics) set.add(e.id());
        return set;
    }

    private Set<String> toCivicIds(List<Civic> civics) {
        var set = new HashSet<String>();
        for (var c : civics) set.add(c.id());
        return set;
    }

    private String describeChange(GeneratedEmpire old, GeneratedEmpire updated, RerollCategory cat) {
        return switch (cat) {
            case ETHICS -> old.ethics().stream().map(Ethic::id).toList() + " → " + updated.ethics().stream().map(Ethic::id).toList();
            case AUTHORITY -> old.authority().id() + " → " + updated.authority().id();
            case CIVIC1 -> old.civics().get(0).id() + " → " + updated.civics().get(0).id();
            case CIVIC2 -> old.civics().get(1).id() + " → " + updated.civics().get(1).id();
            case ORIGIN -> old.origin().id() + " → " + updated.origin().id();
            case TRAITS -> old.speciesTraits().stream().map(SpeciesTrait::id).toList() + " → " + updated.speciesTraits().stream().map(SpeciesTrait::id).toList();
            case HOMEWORLD -> old.homeworld().id() + " → " + updated.homeworld().id();
            case SHIPSET -> old.shipset().id() + " → " + updated.shipset().id();
            case LEADER -> (old.leaderClass() + "/" + old.leaderTraits().stream().map(StartingRulerTrait::id).toList())
                    + " → " + (updated.leaderClass() + "/" + updated.leaderTraits().stream().map(StartingRulerTrait::id).toList());
            case SECONDARY_SPECIES -> (old.secondarySpecies() != null ? old.secondarySpecies().speciesClass() : "none")
                    + " → " + (updated.secondarySpecies() != null ? updated.secondarySpecies().speciesClass() : "none");
            case TRAIT_SINGLE -> old.speciesTraits().stream().map(SpeciesTrait::id).toList()
                    + " → " + updated.speciesTraits().stream().map(SpeciesTrait::id).toList();
        };
    }
}
