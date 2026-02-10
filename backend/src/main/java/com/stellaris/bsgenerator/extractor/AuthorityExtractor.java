package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Authority;
import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AuthorityExtractor {

    private static final List<String> GESTALT_AUTHORITIES = List.of(
            "auth_hive_mind", "auth_machine_intelligence"
    );

    public List<Authority> extract(ClausewitzNode root) {
        List<Authority> authorities = new ArrayList<>();

        for (var node : root.children()) {
            if (node.key() == null || !node.isBlock()) continue;

            String id = node.key();

            // Filter out non-player authorities (e.g., auth_ancient_machine_intelligence)
            // These have potential = { country_type = { value = ai_empire } }
            // Note: auth_corporate has country_type = { NOT = { value = primitive } } which is fine
            RequirementBlock potential = node.child("potential")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);
            if (isNonPlayerAuthority(potential)) {
                log.debug("Skipping non-player authority: {}", id);
                continue;
            }

            String electionType = node.childValue("election_type").orElse("none");
            boolean hasHeir = node.childBool("has_heir", false);

            RequirementBlock possible = node.child("possible")
                    .map(RequirementBlockParser::parse)
                    .orElse(null);

            int randomWeight = node.child("random_weight")
                    .map(rw -> rw.childInt("base", 1))
                    .orElse(1);

            boolean isGestalt = GESTALT_AUTHORITIES.contains(id);

            authorities.add(new Authority(id, electionType, hasHeir,
                    potential, possible, randomWeight, isGestalt));
        }

        log.info("Extracted {} player authorities", authorities.size());
        return authorities;
    }

    /**
     * Check if an authority is non-player by looking for country_type = { value = ai_empire }.
     * Authorities like auth_corporate have country_type = { NOT = { value = primitive } } which
     * should NOT be filtered.
     */
    private boolean isNonPlayerAuthority(RequirementBlock potential) {
        if (potential == null || !potential.hasCategory(RequirementCategory.COUNTRY_TYPE)) {
            return false;
        }
        return potential.get(RequirementCategory.COUNTRY_TYPE).stream()
                .anyMatch(r -> r instanceof Requirement.Value v && v.value().equals("ai_empire"));
    }
}
