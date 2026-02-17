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
        for (int attempt = 0; attempt < 50; attempt++) {
            var newEmpire = generatorService.generate();
            var candidateEthics = newEmpire.ethics();

            var state = EmpireState.empty()
                    .withEthics(toEthicIds(candidateEthics));
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
        throw new GenerationException("Could not find compatible ethics for reroll");
    }

    private GeneratedEmpire rerollAuthority(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()));
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
            throw new GenerationException("No alternative authorities compatible with current empire");
        }

        var newAuth = WeightedRandom.select(compatible, Authority::randomWeight, random);
        return copyWith(empire, b -> b.authority = newAuth);
    }

    private GeneratedEmpire rerollCivic(GeneratedEmpire empire, int index) {
        var otherCivicId = empire.civics().get(1 - index).id();
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
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
                .withCivics(toCivicIds(empire.civics()));

        var compatible = filterService.getCompatibleOrigins(state).stream()
                .filter(o -> !o.id().equals(empire.origin().id()))
                .toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative origins compatible with current empire");
        }

        var newOrigin = WeightedRandom.select(compatible, Origin::randomWeight, random);
        // Re-generate secondary species when origin changes (may gain or lose secondary species)
        var newSecondary = generatorService.generateSecondarySpecies(newOrigin, empire.civics(), empire.speciesClass());
        return copyWith(empire, b -> {
            b.origin = newOrigin;
            b.secondarySpecies = newSecondary;
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

        // Preserve origin enforced traits
        var enforcedIds = new HashSet<>(empire.origin().enforcedTraitIds());
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
        return copyWith(empire, b -> {
            b.speciesTraits = List.copyOf(picked);
            b.traitPointsUsed = finalPointsSpent;
            b.traitPointsBudget = budget;
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
        Set<String> traitPlanetRestriction = collectTraitPlanetClasses(empire.speciesTraits());
        if (!traitPlanetRestriction.isEmpty()) {
            planets = new ArrayList<>(planets.stream()
                    .filter(p -> traitPlanetRestriction.contains(p.id()))
                    .toList());
        }

        if (planets.isEmpty()) {
            throw new GenerationException("No alternative homeworld planets available");
        }

        var newPlanet = planets.get(random.nextInt(planets.size()));
        return copyWith(empire, b -> b.homeworld = newPlanet);
    }

    private Set<String> collectTraitPlanetClasses(java.util.List<com.stellaris.bsgenerator.model.SpeciesTrait> traits) {
        Set<String> result = null;
        for (var trait : traits) {
            if (!trait.allowedPlanetClasses().isEmpty()) {
                if (result == null) {
                    result = new java.util.HashSet<>(trait.allowedPlanetClasses());
                } else {
                    result.retainAll(trait.allowedPlanetClasses());
                }
            }
        }
        return result != null ? result : Set.of();
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
            this.shipset = e.shipset();
            this.leaderClass = e.leaderClass();
            this.leaderTraits = e.leaderTraits();
            this.secondarySpecies = e.secondarySpecies();
        }

        GeneratedEmpire build() {
            return new GeneratedEmpire(ethics, authority, civics, origin,
                    speciesArchetype, speciesClass, speciesTraits, traitPointsUsed, traitPointsBudget,
                    homeworld, shipset, leaderClass, leaderTraits, secondarySpecies);
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
        };
    }
}
