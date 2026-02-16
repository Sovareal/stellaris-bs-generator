package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.SpeciesTrait;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SpeciesTraitExtractor {

    public List<SpeciesTrait> extract(ClausewitzNode root) {
        List<SpeciesTrait> traits = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            String id = node.key();

            // Must have allowed_archetypes (skips leader traits)
            var archetypesNode = node.child("allowed_archetypes").orElse(null);
            if (archetypesNode == null) continue;

            // Must have cost field (skips habitability-only traits)
            var costNode = node.child("cost").orElse(null);
            if (costNode == null) continue;

            // Skip traits with initial = no (cyborg, presapient traits)
            if (!node.childBool("initial", true)) continue;

            // Skip auto-mod traits (some don't have initial = no but have auto_mod = yes)
            if (node.childBool("auto_mod", false)) continue;

            // Parse cost: either leaf (cost = 2) or block (cost = { base = 3 })
            int cost = parseCost(costNode);

            List<String> allowedArchetypes = archetypesNode.bareValues();

            // Parse species_class restriction (e.g., species_class = { AQUATIC ART REP })
            List<String> allowedSpeciesClasses = node.child("species_class")
                    .map(ClausewitzNode::bareValues).orElse(List.of());

            // Parse allowed_planet_classes (e.g., allowed_planet_classes = { pc_ocean })
            List<String> allowedPlanetClasses = node.child("allowed_planet_classes")
                    .map(ClausewitzNode::bareValues).orElse(List.of());

            // Parse opposites: either bare values or quoted strings
            List<String> opposites = node.child("opposites")
                    .map(this::parseOpposites)
                    .orElse(List.of());

            boolean randomized = node.childBool("randomized", true);

            // DLC requirement is rare for traits, but check playable block
            String dlcRequirement = node.child("playable")
                    .flatMap(p -> p.childValue("host_has_dlc"))
                    .orElse(null);

            List<String> tags = node.child("tags")
                    .map(ClausewitzNode::bareValues)
                    .orElse(List.of());

            List<String> allowedOrigins = node.child("allowed_origins")
                    .map(ClausewitzNode::bareValues).orElse(List.of());
            List<String> forbiddenOrigins = node.child("forbidden_origins")
                    .map(ClausewitzNode::bareValues).orElse(List.of());
            List<String> allowedCivics = node.child("allowed_civics")
                    .map(ClausewitzNode::bareValues).orElse(List.of());
            List<String> forbiddenCivics = node.child("forbidden_civics")
                    .map(ClausewitzNode::bareValues).orElse(List.of());
            List<String> allowedEthics = node.child("allowed_ethics")
                    .map(ClausewitzNode::bareValues).orElse(List.of());
            List<String> forbiddenEthics = node.child("forbidden_ethics")
                    .map(ClausewitzNode::bareValues).orElse(List.of());

            traits.add(new SpeciesTrait(id, cost, allowedArchetypes, allowedSpeciesClasses,
                    allowedPlanetClasses, opposites, true, randomized, dlcRequirement, tags,
                    allowedOrigins, forbiddenOrigins, allowedCivics, forbiddenCivics,
                    allowedEthics, forbiddenEthics));
        }

        log.info("Extracted {} creation-eligible species traits", traits.size());
        return traits;
    }

    private int parseCost(ClausewitzNode costNode) {
        if (costNode.isLeaf()) {
            // cost = 2
            return (int) Double.parseDouble(costNode.value());
        } else if (costNode.isBlock()) {
            // cost = { base = 3 ... }
            return costNode.childInt("base", 0);
        }
        return 0;
    }

    private List<String> parseOpposites(ClausewitzNode oppositeNode) {
        // Opposites can be bare quoted strings or bare values
        // e.g., opposites = { "trait_slow_breeders" "trait_fertile" }
        // The parser strips quotes, so these appear as bare values
        return oppositeNode.bareValues();
    }
}
