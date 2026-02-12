package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a Clausewitz potential/possible node into a typed RequirementBlock.
 * <p>
 * Expected structure:
 * <pre>
 * possible = {
 *     ethics = {
 *         value = ethic_X
 *         NOT = { value = ethic_Y }
 *         NOR = { value = A  value = B }
 *         OR  = { value = C  value = D }
 *     }
 *     authority = { value = auth_Z }
 * }
 * </pre>
 * <p>
 * Skips: {@code text = ...} (tooltips), {@code always = yes/no} (no-ops for our purposes),
 * and any unrecognized category keys.
 */
public final class RequirementBlockParser {

    private RequirementBlockParser() {}

    /**
     * Parse a potential/possible node into a RequirementBlock.
     *
     * @param node the potential or possible node (its children are category blocks)
     * @return a RequirementBlock, or null if the block is empty/has no meaningful requirements
     */
    public static RequirementBlock parse(ClausewitzNode node) {
        if (node == null || node.children().isEmpty()) {
            return null;
        }

        Map<RequirementCategory, List<Requirement>> categories = new EnumMap<>(RequirementCategory.class);
        List<Map<RequirementCategory, List<Requirement>>> crossCategoryOrs = new ArrayList<>();

        for (var child : node.children()) {
            if (child.key() == null) continue;

            // Skip tooltip text and always = yes/no
            if (child.key().equals("text") || child.key().equals("always")) continue;

            // Handle top-level OR blocks that span multiple categories
            if (child.key().equals("OR") && child.isBlock()) {
                var orGroup = parseCrossCategoryOr(child);
                if (!orGroup.isEmpty()) {
                    crossCategoryOrs.add(orGroup);
                }
                continue;
            }

            RequirementCategory category = RequirementCategory.fromKey(child.key());
            if (category == null) continue; // Unrecognized key

            if (child.isBlock()) {
                List<Requirement> reqs = parseCategoryBlock(child);
                if (!reqs.isEmpty()) {
                    categories.computeIfAbsent(category, _ -> new ArrayList<>()).addAll(reqs);
                }
            }
        }

        if (categories.isEmpty() && crossCategoryOrs.isEmpty()) return null;
        return new RequirementBlock(Map.copyOf(categories), List.copyOf(crossCategoryOrs));
    }

    /**
     * Parse a cross-category OR block like:
     * <pre>
     * OR = {
     *     authority = { value = auth_corporate }
     *     civics = { value = civic_galactic_sovereign_megacorp }
     * }
     * </pre>
     * Returns a map of category → requirements (each branch is a disjunct).
     */
    private static Map<RequirementCategory, List<Requirement>> parseCrossCategoryOr(ClausewitzNode orNode) {
        Map<RequirementCategory, List<Requirement>> branches = new EnumMap<>(RequirementCategory.class);
        for (var child : orNode.children()) {
            if (child.key() == null || !child.isBlock()) continue;
            RequirementCategory category = RequirementCategory.fromKey(child.key());
            if (category == null) continue;
            List<Requirement> reqs = parseCategoryBlock(child);
            if (!reqs.isEmpty()) {
                branches.computeIfAbsent(category, _ -> new ArrayList<>()).addAll(reqs);
            }
        }
        return branches.isEmpty() ? Map.of() : Map.copyOf(branches);
    }

    /**
     * Parse a single category block (e.g., ethics = { ... }) into a list of requirements.
     */
    private static List<Requirement> parseCategoryBlock(ClausewitzNode categoryNode) {
        List<Requirement> requirements = new ArrayList<>();

        for (var child : categoryNode.children()) {
            if (child.key() == null) continue;

            switch (child.key()) {
                case "value" -> {
                    if (child.value() != null) {
                        requirements.add(new Requirement.Value(child.value()));
                    }
                }
                case "NOT" -> {
                    if (child.isBlock()) {
                        var notValue = child.childValue("value").orElse(null);
                        if (notValue != null) {
                            requirements.add(new Requirement.Not(notValue));
                        }
                    }
                }
                case "NOR" -> {
                    if (child.isBlock()) {
                        List<String> values = extractValues(child);
                        if (!values.isEmpty()) {
                            requirements.add(new Requirement.Nor(List.copyOf(values)));
                        }
                    }
                }
                case "OR" -> {
                    if (child.isBlock()) {
                        List<String> values = extractValues(child);
                        if (!values.isEmpty()) {
                            requirements.add(new Requirement.Or(List.copyOf(values)));
                        }
                    }
                }
                // Skip text = ..., always = ..., and other non-requirement keys
                default -> {}
            }
        }

        return requirements;
    }

    /**
     * Extract all "value = X" entries from a block (used for NOT, NOR, OR children).
     */
    private static List<String> extractValues(ClausewitzNode block) {
        List<String> values = new ArrayList<>();
        for (var child : block.children()) {
            if ("value".equals(child.key()) && child.value() != null) {
                values.add(child.value());
            }
        }
        return values;
    }
}
