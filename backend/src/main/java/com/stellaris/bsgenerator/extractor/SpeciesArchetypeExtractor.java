package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.SpeciesArchetype;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SpeciesArchetypeExtractor {

    public List<SpeciesArchetype> extract(ClausewitzNode root) {
        // First pass: collect raw data and inheritance references
        record RawArchetype(String id, int traitPoints, int maxTraits, boolean robotic,
                            String inheritFrom) {}

        Map<String, RawArchetype> rawMap = new HashMap<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            String id = node.key();
            int traitPoints = node.childInt("species_trait_points", -1);
            int maxTraits = node.childInt("species_max_traits", -1);
            boolean robotic = node.childBool("robotic", false);
            String inheritFrom = node.childValue("inherit_trait_points_from").orElse(null);

            rawMap.put(id, new RawArchetype(id, traitPoints, maxTraits, robotic, inheritFrom));
        }

        // Second pass: resolve inheritance
        List<SpeciesArchetype> archetypes = new ArrayList<>();
        for (var raw : rawMap.values()) {
            int traitPoints = raw.traitPoints();
            int maxTraits = raw.maxTraits();

            if (raw.inheritFrom() != null) {
                var parent = rawMap.get(raw.inheritFrom());
                if (parent != null) {
                    if (traitPoints < 0) traitPoints = parent.traitPoints();
                    if (maxTraits < 0) maxTraits = parent.maxTraits();
                } else {
                    log.warn("Archetype {} inherits from unknown archetype {}", raw.id(), raw.inheritFrom());
                }
            }

            // Default to 0 if still unset
            if (traitPoints < 0) traitPoints = 0;
            if (maxTraits < 0) maxTraits = 0;

            archetypes.add(new SpeciesArchetype(raw.id(), traitPoints, maxTraits, raw.robotic()));
        }

        log.info("Extracted {} species archetypes", archetypes.size());
        return archetypes;
    }
}
