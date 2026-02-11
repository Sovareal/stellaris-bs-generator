package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.GraphicalCulture;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts player-selectable graphical cultures (shipsets) from common/graphical_culture/.
 * Includes entries that either have no selectable block (base cultures) or
 * have a selectable block that isn't "always = no" (DLC cultures).
 * Excludes NPC-only entries (selectable = { always = no }).
 */
@Slf4j
@Service
public class GraphicalCultureExtractor {

    public List<GraphicalCulture> extract(ClausewitzNode root) {
        List<GraphicalCulture> cultures = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            // Check selectable block
            var selectable = node.child("selectable");
            if (selectable.isPresent()) {
                // If selectable = { always = no }, skip (NPC-only)
                if (selectable.get().childBool("always", true) == false) {
                    continue;
                }
            }

            // Include: no selectable block (base cultures) or selectable with conditions
            cultures.add(new GraphicalCulture(node.key()));
        }

        log.info("Extracted {} player-selectable graphical cultures", cultures.size());
        return cultures;
    }
}
