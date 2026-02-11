package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Handles rerolling individual categories of a generated empire.
 * Each category can only be rerolled once per session.
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
     * @throws IllegalStateException if the category has already been rerolled
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
        };

        session.markRerolled();
        session.setEmpire(updated);

        log.info("Rerolled {}: {}", category, describeChange(empire, updated, category));
        return updated;
    }

    private GeneratedEmpire rerollEthics(GeneratedEmpire empire) {
        // Keep authority, civics, origin locked. Pick new ethics that are compatible.
        // Try multiple times to find ethics compatible with existing authority + civics
        for (int attempt = 0; attempt < 50; attempt++) {
            var newEmpire = generatorService.generate();
            var candidateEthics = newEmpire.ethics();

            // Check if new ethics are compatible with locked authority
            var state = EmpireState.empty()
                    .withEthics(toEthicIds(candidateEthics));
            if (!evaluator.evaluateBoth(empire.authority().potential(), empire.authority().possible(), state)) {
                continue;
            }

            // Check if new ethics are compatible with locked civics
            state = state.withAuthority(empire.authority().id());
            boolean civicsCompatible = true;
            for (var civic : empire.civics()) {
                if (!evaluator.evaluateBoth(civic.potential(), civic.possible(), state)) {
                    civicsCompatible = false;
                    break;
                }
            }
            if (!civicsCompatible) continue;

            // Check origin compatibility
            state = state.withCivics(toIdSet(empire.civics()));
            if (!evaluator.evaluateBoth(empire.origin().potential(), empire.origin().possible(), state)) {
                continue;
            }

            // Found compatible ethics — don't reuse the same set
            if (toEthicIds(candidateEthics).equals(toEthicIds(empire.ethics()))) continue;

            return new GeneratedEmpire(candidateEthics, empire.authority(), empire.civics(),
                    empire.origin(), empire.speciesArchetype(), empire.speciesTraits(),
                    empire.traitPointsUsed(), empire.traitPointsBudget());
        }
        throw new GenerationException("Could not find compatible ethics for reroll");
    }

    private GeneratedEmpire rerollAuthority(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()));
        var compatible = filterService.getCompatibleAuthorities(state);
        // Exclude current authority
        compatible = compatible.stream()
                .filter(a -> !a.id().equals(empire.authority().id()))
                .toList();

        // Also check compatibility with locked civics and origin
        compatible = compatible.stream().filter(a -> {
            var withAuth = state.withAuthority(a.id());
            for (var civic : empire.civics()) {
                if (!evaluator.evaluateBoth(civic.potential(), civic.possible(), withAuth)) return false;
            }
            var withCivics = withAuth.withCivics(toIdSet(empire.civics()));
            return evaluator.evaluateBoth(empire.origin().potential(), empire.origin().possible(), withCivics);
        }).toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative authorities compatible with current empire");
        }

        var newAuth = WeightedRandom.select(compatible, Authority::randomWeight, random);
        return new GeneratedEmpire(empire.ethics(), newAuth, empire.civics(),
                empire.origin(), empire.speciesArchetype(), empire.speciesTraits(),
                empire.traitPointsUsed(), empire.traitPointsBudget());
    }

    private GeneratedEmpire rerollCivic(GeneratedEmpire empire, int index) {
        var otherCivicId = empire.civics().get(1 - index).id();

        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(Set.of(otherCivicId)); // Only the other civic is locked

        var compatible = filterService.getCompatibleCivics(state);
        // Exclude current civic at this index
        compatible = compatible.stream()
                .filter(c -> !c.id().equals(empire.civics().get(index).id()))
                .toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative civics compatible with current empire");
        }

        var newCivic = WeightedRandom.select(compatible, Civic::randomWeight, random);
        var newCivics = new ArrayList<>(empire.civics());
        newCivics.set(index, newCivic);

        return new GeneratedEmpire(empire.ethics(), empire.authority(), List.copyOf(newCivics),
                empire.origin(), empire.speciesArchetype(), empire.speciesTraits(),
                empire.traitPointsUsed(), empire.traitPointsBudget());
    }

    private GeneratedEmpire rerollOrigin(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toIdSet(empire.civics()));

        var compatible = filterService.getCompatibleOrigins(state);
        // Exclude current origin
        compatible = compatible.stream()
                .filter(o -> !o.id().equals(empire.origin().id()))
                .toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative origins compatible with current empire");
        }

        var newOrigin = WeightedRandom.select(compatible, Origin::randomWeight, random);
        return new GeneratedEmpire(empire.ethics(), empire.authority(), empire.civics(),
                newOrigin, empire.speciesArchetype(), empire.speciesTraits(),
                empire.traitPointsUsed(), empire.traitPointsBudget());
    }

    private GeneratedEmpire rerollTraits(GeneratedEmpire empire) {
        var archetype = empire.speciesArchetype();
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toIdSet(empire.civics()))
                .withOrigin(empire.origin().id())
                .withSpeciesArchetype(archetype.id());
        var available = filterService.getCompatibleTraits(archetype.id(), state);
        int budget = archetype.traitPoints();
        int maxTraits = archetype.maxTraits();

        // Re-pick traits with new random seed
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

        return new GeneratedEmpire(empire.ethics(), empire.authority(), empire.civics(),
                empire.origin(), empire.speciesArchetype(), List.copyOf(picked),
                pointsSpent, budget);
    }

    private Set<String> toEthicIds(List<Ethic> ethics) {
        var set = new HashSet<String>();
        for (var e : ethics) set.add(e.id());
        return set;
    }

    private Set<String> toIdSet(List<Civic> civics) {
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
        };
    }
}
