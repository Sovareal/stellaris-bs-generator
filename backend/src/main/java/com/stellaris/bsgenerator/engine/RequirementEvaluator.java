package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Evaluates RequirementBlocks (potential/possible) against an EmpireState.
 * <p>
 * Evaluation rules:
 * - Categories within a block are implicitly ANDed
 * - Requirements within a category are implicitly ANDed
 * - If a category hasn't been selected yet in the empire state, requirements
 *   against it are skipped (treated as satisfied) — they'll be checked later
 *   when that category is actually picked
 */
@Service
public class RequirementEvaluator {

    /**
     * Evaluate a requirement block against the current empire state.
     *
     * @param block the requirement block (potential or possible), may be null
     * @param state the current empire state
     * @return true if the block is satisfied (or null/empty), false if any requirement fails
     */
    public boolean evaluate(RequirementBlock block, EmpireState state) {
        if (block == null || block.isEmpty()) {
            return true;
        }

        // All categories must be satisfied (implicit AND)
        for (var entry : block.categories().entrySet()) {
            RequirementCategory category = entry.getKey();
            List<Requirement> requirements = entry.getValue();

            // Skip categories not yet selected — can't evaluate what isn't chosen
            if (!state.hasCategory(category)) {
                continue;
            }

            Set<String> stateValues = state.valuesForCategory(category);

            // All requirements within this category must be satisfied (implicit AND)
            for (var req : requirements) {
                if (!evaluateRequirement(req, stateValues)) {
                    return false;
                }
            }
        }

        // Evaluate cross-category OR blocks (e.g., OR { authority = {...} civics = {...} })
        for (var orGroup : block.crossCategoryOrs()) {
            boolean anyBranchPasses = false;
            for (var branch : orGroup.entrySet()) {
                RequirementCategory category = branch.getKey();
                List<Requirement> requirements = branch.getValue();

                // If this category isn't selected yet, treat as possible (defer)
                if (!state.hasCategory(category)) {
                    anyBranchPasses = true;
                    break;
                }

                Set<String> stateValues = state.valuesForCategory(category);
                boolean allSatisfied = true;
                for (var req : requirements) {
                    if (!evaluateRequirement(req, stateValues)) {
                        allSatisfied = false;
                        break;
                    }
                }
                if (allSatisfied) {
                    anyBranchPasses = true;
                    break;
                }
            }
            if (!anyBranchPasses) return false;
        }

        return true;
    }

    /**
     * Evaluate both potential and possible blocks. Both must pass.
     */
    public boolean evaluateBoth(RequirementBlock potential, RequirementBlock possible, EmpireState state) {
        return evaluate(potential, state) && evaluate(possible, state);
    }

    private boolean evaluateRequirement(Requirement req, Set<String> stateValues) {
        return switch (req) {
            case Requirement.Value v ->
                // Empire must contain this value
                stateValues.contains(v.value());

            case Requirement.Not not ->
                // Empire must NOT contain this value
                !stateValues.contains(not.value());

            case Requirement.Nor nor ->
                // Empire must contain NONE of these values
                nor.values().stream().noneMatch(stateValues::contains);

            case Requirement.Or or ->
                // Empire must contain at least ONE of these values
                or.values().stream().anyMatch(stateValues::contains);
        };
    }
}
