package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.config.DlcRegistry;
import com.stellaris.bsgenerator.model.Origin;
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

            // Filter AI/event-only origins: potential = { always = no }
            var potentialCheckNode = node.child("potential").orElse(null);
            if (potentialCheckNode != null && potentialCheckNode.childBool("always", true) == false) {
                log.debug("Skipping potential=always-no origin: {}", id);
                continue;
            }

            // Filter zero-weight origins (event-spawned variants, not in random pool)
            int randomWeight = node.child("random_weight")
                    .map(rw -> rw.childInt("base", 1))
                    .orElse(1);
            if (randomWeight == 0) {
                log.debug("Skipping zero-weight origin: {}", id);
                continue;
            }

            RequirementBlock potential = node.child("potential")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);

            RequirementBlock possible = node.child("possible")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);

            // Extract DLC requirement from playable = { host_has_dlc = "..." } or has_xxx_dlc triggers
            String dlcRequirement = null;
            if (playableNode != null) {
                String hostDlc = playableNode.childValue("host_has_dlc").orElse(null);
                if (hostDlc != null) {
                    dlcRequirement = DlcRegistry.fromHostDlc(hostDlc);
                    if (dlcRequirement == null) dlcRequirement = hostDlc; // keep raw value if not in registry
                }
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

            SecondarySpeciesConfig secondarySpecies = parseSecondarySpecies(node);

            // Parse origin-level enforced species traits: traits = { trait = X }
            List<String> enforcedTraitIds = node.child("traits")
                    .map(traitsNode -> traitsNode.children("trait").stream()
                            .map(ClausewitzNode::value)
                            .filter(Objects::nonNull)
                            .toList())
                    .orElse(List.of());

            // Parse icon path (e.g. "gfx/interface/icons/origins/origins_default.dds")
            String iconPath = node.childValue("icon").orElse(null);

            // Parse habitability preference (e.g. pc_ocean, pc_habitat)
            String habitabilityPreference = node.childValue("habitability_preference").orElse(null);

            // Parse shipset requirement from possible.graphical_culture (e.g. biogenesis_01/02 for Wilderness)
            List<String> requiredShipsetIds = node.child("possible")
                    .flatMap(p -> p.child("graphical_culture"))
                    .map(gc -> {
                        var orNode = gc.child("OR").orElse(gc);
                        return orNode.children("value").stream()
                                .map(ClausewitzNode::value)
                                .filter(Objects::nonNull)
                                .toList();
                    })
                    .orElse(List.of());

            origins.add(new Origin(id, potential, possible, dlcRequirement, randomWeight, secondarySpecies, enforcedTraitIds, iconPath, habitabilityPreference, requiredShipsetIds));
        }

        log.info("Extracted {} playable origins", origins.size());
        return origins;
    }

    static SecondarySpeciesConfig parseSecondarySpecies(ClausewitzNode node) {
        return node.child("has_secondary_species").map(ssNode -> {
            String title = ssNode.childValue("title").orElse(null);
            List<String> traitIds = ssNode.child("traits")
                    .map(traitsNode -> traitsNode.children("trait").stream()
                            .map(ClausewitzNode::value)
                            .filter(v -> v != null)
                            .toList())
                    .orElse(List.of());
            return new SecondarySpeciesConfig(title, traitIds);
        }).orElse(null);
    }
}
