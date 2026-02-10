package com.stellaris.bsgenerator.model.requirement;

import java.util.List;
import java.util.Map;

/**
 * A parsed potential/possible block from a Clausewitz entity.
 * Contains requirements grouped by category, where conditions within
 * each category are implicitly ANDed, and categories are also ANDed.
 */
public record RequirementBlock(Map<RequirementCategory, List<Requirement>> categories) {

    public List<Requirement> get(RequirementCategory category) {
        return categories.getOrDefault(category, List.of());
    }

    public boolean hasCategory(RequirementCategory category) {
        return categories.containsKey(category);
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }
}
