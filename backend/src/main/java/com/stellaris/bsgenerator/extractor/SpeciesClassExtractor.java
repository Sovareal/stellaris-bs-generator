package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.config.DlcRegistry;
import com.stellaris.bsgenerator.model.SpeciesClass;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SpeciesClassExtractor {

    public List<SpeciesClass> extract(ClausewitzNode root) {
        List<SpeciesClass> classes = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            String id = node.key();

            // Must have archetype field (skip ship-set-only entries like IMPERIAL, CYBERNETIC, PSIONIC)
            var archetypeValue = node.childValue("archetype").orElse(null);
            if (archetypeValue == null) continue;

            // Skip presapient species (PRE_MAM, PRE_REP, etc.)
            if ("PRESAPIENT".equals(archetypeValue)) continue;

            // Skip if playable = { always = no }
            var playableNode = node.child("playable").orElse(null);
            if (playableNode != null && playableNode.isBlock()) {
                var alwaysVal = playableNode.childValue("always").orElse(null);
                if ("no".equals(alwaysVal)) continue;
            }

            // Skip ROBOT — only available after game start (has_global_flag = game_started),
            // not in the character creator
            if (playableNode != null && playableNode.isBlock()) {
                var globalFlag = playableNode.childValue("has_global_flag").orElse(null);
                if ("game_started".equals(globalFlag)) continue;
            }

            // Skip classes explicitly marked as non-randomizable: randomized = { always = no }
            var randomizedNode = node.child("randomized").orElse(null);
            if (randomizedNode != null && randomizedNode.isBlock()) {
                var alwaysVal = randomizedNode.childValue("always").orElse(null);
                if ("no".equals(alwaysVal)) {
                    log.debug("Skipping non-randomizable species class: {}", id);
                    continue;
                }
            }

            // Extract DLC requirement from the playable block
            String dlcRequirement = null;
            if (playableNode != null && playableNode.isBlock()) {
                // Try host_has_dlc first
                String hostDlc = playableNode.childValue("host_has_dlc").orElse(null);
                if (hostDlc != null) {
                    dlcRequirement = DlcRegistry.fromHostDlc(hostDlc);
                }
                // Then try has_xxx_dlc triggers
                if (dlcRequirement == null) {
                    for (var child : playableNode.children()) {
                        if (child.key() != null && child.isLeaf()) {
                            String mapped = DlcRegistry.fromTrigger(child.key());
                            if (mapped != null) {
                                dlcRequirement = mapped;
                                break;
                            }
                        }
                    }
                }
            }

            classes.add(new SpeciesClass(id, archetypeValue, dlcRequirement));
        }

        log.info("Extracted {} playable species classes", classes.size());
        return classes;
    }
}
