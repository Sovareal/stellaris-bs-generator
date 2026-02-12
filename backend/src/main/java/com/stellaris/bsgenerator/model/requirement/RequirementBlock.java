package com.stellaris.bsgenerator.model.requirement;

import java.util.List;
import java.util.Map;

/**
 * A parsed potential/possible block from a Clausewitz entity.
 * Contains requirements grouped by category, where conditions within
 * each category are implicitly ANDed, and categories are also ANDed.
 * <p>
 * Cross-category OR blocks handle cases like:
 * <pre>
 * potential = {
 *     OR = {
 *         authority = { value = auth_corporate }
 *         civics = { value = civic_galactic_sovereign_megacorp }
 *     }
 * }
 * </pre>
 * Each entry in {@code crossCategoryOrs} is a disjunction: at least one
 * category branch must be fully satisfied.
 */
public record RequirementBlock(
        Map<RequirementCategory, List<Requirement>> categories,
        List<Map<RequirementCategory, List<Requirement>>> crossCategoryOrs
) {
    public RequirementBlock(Map<RequirementCategory, List<Requirement>> categories) {
        this(categories, List.of());
    }

    public List<Requirement> get(RequirementCategory category) {
        return categories.getOrDefault(category, List.of());
    }

    public boolean hasCategory(RequirementCategory category) {
        return categories.containsKey(category);
    }

    public boolean isEmpty() {
        return categories.isEmpty() && crossCategoryOrs.isEmpty();
    }
}
