package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Civic;
import com.stellaris.bsgenerator.model.SecondarySpeciesConfig;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

            civics.add(new Civic(id, potential, possible, pickableAtStart, randomWeight, secondarySpecies, enforcedTraitIds));
        }

        log.info("Extracted {} civics", civics.size());
        return civics;
    }
}
