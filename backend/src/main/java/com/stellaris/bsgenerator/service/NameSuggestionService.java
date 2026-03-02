package com.stellaris.bsgenerator.service;

import com.stellaris.bsgenerator.model.Authority;
import com.stellaris.bsgenerator.model.Ethic;
import com.stellaris.bsgenerator.model.SpeciesArchetype;

import java.util.List;
import java.util.Map;

/**
 * Pure utility — no Spring bean. Derives a thematic empire name from ethics, authority, and archetype.
 * Formula: [archetype prefix] [ethic adjective] [authority noun]
 */
public final class NameSuggestionService {

    private static final Map<String, String> ETHIC_ADJECTIVE = Map.ofEntries(
            Map.entry("ethic_fanatic_authoritarian", "Eternal"),
            Map.entry("ethic_authoritarian", "Grand"),
            Map.entry("ethic_fanatic_egalitarian", "Free"),
            Map.entry("ethic_egalitarian", "United"),
            Map.entry("ethic_fanatic_xenophile", "Interstellar"),
            Map.entry("ethic_xenophile", "Galactic"),
            Map.entry("ethic_fanatic_xenophobe", "Closed"),
            Map.entry("ethic_xenophobe", "Sovereign"),
            Map.entry("ethic_fanatic_militarist", "Military"),
            Map.entry("ethic_militarist", "Martial"),
            Map.entry("ethic_fanatic_pacifist", "Peaceful"),
            Map.entry("ethic_pacifist", "Harmonious"),
            Map.entry("ethic_fanatic_spiritualist", "Divine"),
            Map.entry("ethic_spiritualist", "Sacred"),
            Map.entry("ethic_fanatic_materialist", "Scientific"),
            Map.entry("ethic_materialist", "Rational")
    );

    private static final Map<String, String> AUTHORITY_NOUN = Map.of(
            "auth_democratic", "Republic",
            "auth_oligarchic", "Hegemony",
            "auth_dictatorial", "Dominion",
            "auth_imperial", "Empire",
            "auth_hive_mind", "Collective",
            "auth_machine_intelligence", "Consensus",
            "auth_corporate", "Syndicate"
    );

    private NameSuggestionService() {}

    public static String suggest(List<Ethic> ethics, Authority authority, SpeciesArchetype archetype) {
        String archetypePrefix = resolveArchetypePrefix(archetype);
        String authorityNoun = AUTHORITY_NOUN.getOrDefault(authority.id(), "Dominion");
        String ethicAdj = resolveEthicAdjective(ethics);

        StringBuilder name = new StringBuilder();
        if (!archetypePrefix.isEmpty()) {
            name.append(archetypePrefix).append(" ");
        }
        if (!ethicAdj.isEmpty()) {
            name.append(ethicAdj).append(" ");
        }
        name.append(authorityNoun);
        return name.toString().trim();
    }

    private static String resolveArchetypePrefix(SpeciesArchetype archetype) {
        if (archetype == null) return "";
        String id = archetype.id().toLowerCase();
        if (id.contains("machine")) return "Machine";
        if (id.contains("hive")) return "Hive";
        if (id.contains("lithoid")) return "Lithoid";
        return "";
    }

    private static String resolveEthicAdjective(List<Ethic> ethics) {
        if (ethics == null || ethics.isEmpty()) return "";
        // Gestalt empires have only ethic_gestalt_consciousness — no adjective
        if (ethics.size() == 1 && ethics.getFirst().id().equals("ethic_gestalt_consciousness")) return "";

        // Prefer fanatic ethic
        Ethic chosen = ethics.stream()
                .filter(e -> e.id().startsWith("ethic_fanatic_"))
                .findFirst()
                .orElse(ethics.getFirst());

        return ETHIC_ADJECTIVE.getOrDefault(chosen.id(), "");
    }
}
