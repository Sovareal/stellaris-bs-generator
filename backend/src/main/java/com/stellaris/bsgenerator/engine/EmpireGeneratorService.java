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

        // 5. Pick species archetype
        SpeciesArchetype archetype = pickArchetype(state);
        state = state.withSpeciesArchetype(archetype.id());

        // 6. Pick compatible traits within budget
        List<SpeciesTrait> traits = pickTraits(archetype);

        int pointsUsed = traits.stream().mapToInt(SpeciesTrait::cost).sum();

        log.info("Generated empire: ethics={}, authority={}, civics={}, origin={}, archetype={}, traits={} ({}/{}pts)",
                ethics.stream().map(Ethic::id).toList(),
                authority.id(),
                civics.stream().map(Civic::id).toList(),
                origin.id(),
                archetype.id(),
                traits.stream().map(SpeciesTrait::id).toList(),
                pointsUsed, archetype.traitPoints());

        return new GeneratedEmpire(ethics, authority, civics, origin,
                archetype, traits, pointsUsed, archetype.traitPoints());
    }

    private List<Ethic> pickEthics() {
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
        return WeightedRandom.select(compatible, Origin::randomWeight, random);
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

    private List<SpeciesTrait> pickTraits(SpeciesArchetype archetype) {
        var available = filterService.getCompatibleTraits(archetype.id());
        int budget = archetype.traitPoints();
        int maxTraits = archetype.maxTraits();

        List<SpeciesTrait> picked = new ArrayList<>();
        Set<String> pickedIds = new HashSet<>();
        Set<String> excludedByOpposites = new HashSet<>();
        int pointsSpent = 0;

        // Shuffle to add randomness (traits don't have random_weight)
        var shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, random);

        for (var trait : shuffled) {
            if (picked.size() >= maxTraits) break;

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

    private Set<String> toIdSet(List<? extends Record> entities) {
        var set = new HashSet<String>();
        for (var entity : entities) {
            if (entity instanceof Ethic e) set.add(e.id());
            else if (entity instanceof Civic c) set.add(c.id());
        }
        return set;
    }
}
