package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.config.DlcRegistry;
import com.stellaris.bsgenerator.model.Civic;
import com.stellaris.bsgenerator.model.SecondarySpeciesConfig;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CivicExtractor {

    public List<Civic> extract(ClausewitzNode root) {
        List<Civic> civics = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            // Skip origins — they have is_origin = yes
            if (node.childBool("is_origin", false)) continue;

            String id = node.key();

            // Skip civics that require NOT having a DLC (we always assume all DLCs are active).
            // e.g. civic_corporate_dominion: playable = { NOT = { host_has_dlc = "Megacorp" } }
            var playableNode = node.child("playable").orElse(null);
            if (playableNode != null) {
                var notNode = playableNode.child("NOT").orElse(null);
                if (notNode != null && notNode.child("host_has_dlc").isPresent()) {
                    log.debug("Skipping no-DLC-required civic: {}", id);
                    continue;
                }
            }
            // Extract DLC requirement from playable = { host_has_dlc = "..." } (positive form only)
            String dlcRequirement = null;
            if (playableNode != null) {
                String hostDlc = playableNode.childValue("host_has_dlc").orElse(null);
                if (hostDlc != null) {
                    dlcRequirement = DlcRegistry.fromHostDlc(hostDlc);
                }
                // Also check has_xxx_dlc triggers in the playable block
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

            boolean pickableAtStart = node.childBool("pickable_at_start", true);

            RequirementBlock potential = node.child("potential")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);

            RequirementBlock possible = node.child("possible")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);

            int randomWeight = node.child("random_weight")
                    .map(rw -> rw.childInt("base", 1))
                    .orElse(1);

            SecondarySpeciesConfig secondarySpecies = OriginExtractor.parseSecondarySpecies(node);

            // Parse civic-enforced traits: traits = { trait = trait_aquatic }
            List<String> enforcedTraitIds = node.child("traits")
                    .map(t -> t.children().stream()
                            .filter(c -> "trait".equals(c.key()) && c.isLeaf())
                            .map(ClausewitzNode::value)
                            .toList())
                    .orElse(List.of());

            // Parse shipset constraints from possible.graphical_culture:
            // OR { value = x } -> whitelist (requiredShipsetIds)
            // NOR { value = x } -> blacklist (excludedShipsetIds)
            var gcNode = node.child("possible").flatMap(p -> p.child("graphical_culture"));
            List<String> requiredShipsetIds = gcNode
                    .flatMap(gc -> gc.child("OR"))
                    .map(or -> or.children("value").stream()
                            .map(ClausewitzNode::value)
                            .filter(Objects::nonNull)
                            .toList())
                    .orElse(List.of());
            List<String> excludedShipsetIds = gcNode
                    .flatMap(gc -> gc.child("NOR"))
                    .map(nor -> nor.children("value").stream()
                            .map(ClausewitzNode::value)
                            .filter(Objects::nonNull)
                            .toList())
                    .orElse(List.of());

            civics.add(new Civic(id, potential, possible, pickableAtStart, randomWeight, secondarySpecies,
                    enforcedTraitIds, requiredShipsetIds, excludedShipsetIds, dlcRequirement));
        }

        log.info("Extracted {} civics", civics.size());
        return civics;
    }
}
