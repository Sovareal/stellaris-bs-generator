package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Origin;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OriginExtractor {

    public List<Origin> extract(ClausewitzNode root) {
        List<Origin> origins = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            // Only process origins (is_origin = yes)
            if (!node.childBool("is_origin", false)) continue;

            String id = node.key();

            // Filter non-playable origins: playable = { always = no }
            var playableNode = node.child("playable").orElse(null);
            if (playableNode != null && playableNode.childBool("always", true) == false) {
                log.debug("Skipping non-playable origin: {}", id);
                continue;
            }

            RequirementBlock potential = node.child("potential")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);

            RequirementBlock possible = node.child("possible")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);

            // Extract DLC requirement from playable = { host_has_dlc = "..." }
            String dlcRequirement = null;
            if (playableNode != null) {
                dlcRequirement = playableNode.childValue("host_has_dlc").orElse(null);
            }

            int randomWeight = node.child("random_weight")
                    .map(rw -> rw.childInt("base", 1))
                    .orElse(1);

            origins.add(new Origin(id, potential, possible, dlcRequirement, randomWeight));
        }

        log.info("Extracted {} playable origins", origins.size());
        return origins;
    }
}
