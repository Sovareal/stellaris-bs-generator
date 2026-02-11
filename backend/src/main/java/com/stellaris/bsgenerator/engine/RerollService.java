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
        return copyWith(empire, b -> b.civics = List.copyOf(newCivics));
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
        return copyWith(empire, b -> b.origin = newOrigin);
    }

    private GeneratedEmpire rerollTraits(GeneratedEmpire empire) {
        var archetype = empire.speciesArchetype();
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id())
                .withSpeciesArchetype(archetype.id());
        var available = filterService.getCompatibleTraits(archetype.id(), state);
        int budget = archetype.traitPoints();
        int maxTraits = archetype.maxTraits();

        List<SpeciesTrait> picked = new ArrayList<>();
        Set<String> pickedIds = new HashSet<>();
        Set<String> excludedByOpposites = new HashSet<>();
        int pointsSpent = 0;

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

    private GeneratedEmpire rerollHomeworld(GeneratedEmpire empire) {
        var planets = filterService.getHabitablePlanetClasses().stream()
                .filter(p -> !p.id().equals(empire.homeworld().id()))
                .toList();

        if (planets.isEmpty()) {
            throw new GenerationException("No alternative homeworld planets available");
        }

        var newPlanet = planets.get(random.nextInt(planets.size()));
        return copyWith(empire, b -> b.homeworld = newPlanet);
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
                .withOrigin(empire.origin().id());

        // Pick a potentially different leader class
        var leaderClasses = List.of("official", "commander", "scientist");
        var newClass = leaderClasses.get(random.nextInt(leaderClasses.size()));

        var compatible = filterService.getCompatibleRulerTraits(newClass, state);
        // Try to exclude the current trait if same class
        if (newClass.equals(empire.leaderClass()) && empire.leaderTrait() != null) {
            var filtered = compatible.stream()
                    .filter(t -> !t.id().equals(empire.leaderTrait().id()))
                    .toList();
            if (!filtered.isEmpty()) {
                compatible = filtered;
            }
        }

        StartingRulerTrait newTrait = compatible.isEmpty() ? null :
                compatible.get(random.nextInt(compatible.size()));
        return copyWith(empire, b -> {
            b.leaderClass = newClass;
            b.leaderTrait = newTrait;
        });
    }

    // --- Helper to build empire copies with selective changes ---

    private static class EmpireBuilder {
        List<Ethic> ethics;
        Authority authority;
        List<Civic> civics;
        Origin origin;
        SpeciesArchetype speciesArchetype;
        List<SpeciesTrait> speciesTraits;
        int traitPointsUsed;
        int traitPointsBudget;
        PlanetClass homeworld;
        GraphicalCulture shipset;
        String leaderClass;
        StartingRulerTrait leaderTrait;

        EmpireBuilder(GeneratedEmpire e) {
            this.ethics = e.ethics();
            this.authority = e.authority();
            this.civics = e.civics();
            this.origin = e.origin();
            this.speciesArchetype = e.speciesArchetype();
            this.speciesTraits = e.speciesTraits();
            this.traitPointsUsed = e.traitPointsUsed();
            this.traitPointsBudget = e.traitPointsBudget();
            this.homeworld = e.homeworld();
            this.shipset = e.shipset();
            this.leaderClass = e.leaderClass();
            this.leaderTrait = e.leaderTrait();
        }

        GeneratedEmpire build() {
            return new GeneratedEmpire(ethics, authority, civics, origin,
                    speciesArchetype, speciesTraits, traitPointsUsed, traitPointsBudget,
                    homeworld, shipset, leaderClass, leaderTrait);
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
            case LEADER -> (old.leaderClass() + "/" + (old.leaderTrait() != null ? old.leaderTrait().id() : "none"))
                    + " → " + (updated.leaderClass() + "/" + (updated.leaderTrait() != null ? updated.leaderTrait().id() : "none"));
        };
    }
}
