package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.StartingRulerTrait;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts starting ruler traits (starting_ruler_trait = yes) from common/traits/.
 * Skips tier-2 traits (those with replace_traits).
 */
@Slf4j
@Service
public class StartingRulerTraitExtractor {

    public List<StartingRulerTrait> extract(ClausewitzNode root) {
        List<StartingRulerTrait> traits = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            boolean isStartingRulerTrait = node.childBool("starting_ruler_trait", false);
            if (!isStartingRulerTrait) continue;

            // Skip tier-2 upgraded traits (they have replace_traits)
            var replaceTraits = node.child("replace_traits");
            if (replaceTraits.isPresent() && !replaceTraits.get().bareValues().isEmpty()) continue;

            String id = node.key();
            List<String> leaderClasses = node.child("leader_class")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());
            List<String> forbiddenOrigins = node.child("forbidden_origins")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());
            List<String> allowedEthics = node.child("allowed_ethics")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());
            List<String> allowedOrigins = node.child("allowed_origins")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());
            List<String> allowedCivics = node.child("allowed_civics")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());
            List<String> forbiddenCivics = node.child("forbidden_civics")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());
            List<String> forbiddenEthics = node.child("forbidden_ethics")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());
            int cost = node.childInt("cost", 0);
            List<String> opposites = node.child("opposites")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());

            // Extract GFX key from inline_script ICON field (e.g. "GFX_leader_trait_principled")
            String gfxKey = node.child("inline_script")
                    .flatMap(n -> n.childValue("ICON"))
                    .orElse(null);

            traits.add(new StartingRulerTrait(id, leaderClasses, forbiddenOrigins, allowedEthics,
                    allowedOrigins, allowedCivics, forbiddenCivics, forbiddenEthics, cost, opposites, gfxKey));
        }

        log.info("Extracted {} starting ruler traits", traits.size());
        return traits;
    }
}
