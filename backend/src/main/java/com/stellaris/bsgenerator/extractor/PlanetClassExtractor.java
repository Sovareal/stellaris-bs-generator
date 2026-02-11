package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.PlanetClass;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts habitable planet classes (initial=yes) from common/planet_classes/.
 */
@Slf4j
@Service
public class PlanetClassExtractor {

    public List<PlanetClass> extract(ClausewitzNode root) {
        List<PlanetClass> planetClasses = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            boolean colonizable = node.childBool("colonizable", false);
            boolean initial = node.childBool("initial", false);

            if (!colonizable || !initial) continue;

            String id = node.key();
            String climate = node.childValue("climate").orElse("unknown");

            planetClasses.add(new PlanetClass(id, climate));
        }

        log.info("Extracted {} habitable planet classes (initial=yes)", planetClasses.size());
        return planetClasses;
    }
}
