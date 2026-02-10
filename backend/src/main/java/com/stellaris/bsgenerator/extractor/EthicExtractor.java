package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Ethic;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EthicExtractor {

    public List<Ethic> extract(ClausewitzNode root) {
        List<Ethic> ethics = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            String id = node.key();
            int cost = node.childInt("cost", 0);
            String category = node.childValue("category").orElse(null);
            String regularVariant = node.childValue("regular_variant").orElse(null);
            String fanaticVariant = node.childValue("fanatic_variant").orElse(null);
            boolean isFanatic = cost == 2;
            boolean isGestalt = id.equals("ethic_gestalt_consciousness");

            List<String> tags = node.child("tags")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());

            int randomWeight = node.child("random_weight")
                    .map(rw -> rw.childInt("base", 1))
                    .orElse(1);

            ethics.add(new Ethic(id, cost, category, isFanatic, isGestalt,
                    regularVariant, fanaticVariant, tags, randomWeight));
        }

        log.info("Extracted {} ethics", ethics.size());
        return ethics;
    }
}
