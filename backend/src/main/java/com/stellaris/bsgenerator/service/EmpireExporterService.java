package com.stellaris.bsgenerator.service;

import com.stellaris.bsgenerator.engine.GeneratedEmpire;
import com.stellaris.bsgenerator.model.SecondarySpecies;
import com.stellaris.bsgenerator.model.SpeciesTrait;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds a Clausewitz-format empire block from a GeneratedEmpire and user-supplied names,
 * ready to be appended to user_empire_designs_v3.*.txt.
 */
@Service
@Slf4j
public class EmpireExporterService {

    // speciesClass → one valid portrait ID (first/safest entry for each class)
    private static final Map<String, String> SPECIES_PORTRAITS = Map.ofEntries(
        Map.entry("HUM", "humanoid_01"),
        Map.entry("MAM", "mam1"),
        Map.entry("REP", "rep1"),
        Map.entry("AVI", "avi1"),
        Map.entry("ART", "art1"),
        Map.entry("MOL", "mol1"),
        Map.entry("FUN", "fun1"),
        Map.entry("PLANT", "pla1"),
        Map.entry("NECROID", "nec1"),
        Map.entry("AQUATIC", "aqu1"),
        Map.entry("TOX", "tox1"),
        Map.entry("INF", "inf1"),
        Map.entry("LITHOID", "lit1"),
        Map.entry("ROBOT", "robot1"),
        Map.entry("MACHINE", "machine_1"),
        Map.entry("BIOGENESIS_01", "pla1"),
        Map.entry("MINDWARDEN", "humanoid_01")
    );

    // authority ID → government ID (most generic valid choice per authority)
    private static final Map<String, String> AUTHORITY_GOVERNMENTS = Map.of(
        "auth_democratic", "gov_democratic_republic",
        "auth_oligarchic", "gov_oligarchic_republic",
        "auth_dictatorial", "gov_military_dictatorship",
        "auth_imperial", "gov_imperial",
        "auth_corporate", "gov_trade_league",
        "auth_hive_mind", "gov_hive_mind",
        "auth_machine_intelligence", "gov_machine_empire"
    );

    // speciesClass → flag icon category (Stellaris gfx/flags/<category>/ directory)
    private static final Map<String, String> SPECIES_FLAG_CATEGORIES = Map.ofEntries(
        Map.entry("HUM", "human"),
        Map.entry("MAM", "mammalian"),
        Map.entry("REP", "reptilian"),
        Map.entry("AVI", "avian"),
        Map.entry("ART", "arthropoid"),
        Map.entry("MOL", "molluscoid"),
        Map.entry("FUN", "fungoid")
    );

    // authority ID → advisor voice type (omitted for most authorities)
    private static final Map<String, String> AUTHORITY_ADVISORS = Map.of(
        "auth_machine_intelligence", "l_machine"
    );

    // speciesClass → name_list ID
    private static final Map<String, String> SPECIES_NAME_LISTS = Map.ofEntries(
        Map.entry("HUM", "HUM1"),
        Map.entry("MAM", "MAM1"),
        Map.entry("REP", "REP1"),
        Map.entry("AVI", "AVI1"),
        Map.entry("ART", "ART1"),
        Map.entry("MOL", "MOL1"),
        Map.entry("FUN", "FUN1"),
        Map.entry("PLANT", "PLANT1"),
        Map.entry("NECROID", "NECROID1"),
        Map.entry("AQUATIC", "AQUATIC1"),
        Map.entry("TOX", "TOX1"),
        Map.entry("INF", "INF1"),
        Map.entry("LITHOID", "LITHOID1"),
        Map.entry("ROBOT", "ROBOT1"),
        Map.entry("MACHINE", "MACHINE1"),
        Map.entry("BIOGENESIS_01", "PLANT1"),
        Map.entry("MINDWARDEN", "HUM1")
    );

    /**
     * Builds a complete Clausewitz empire block ready to append to user_empire_designs.
     */
    public String buildEmpireBlock(GeneratedEmpire empire, ExportOptions opts) {
        String speciesClass = empire.speciesClass();
        String portrait = SPECIES_PORTRAITS.getOrDefault(speciesClass, "humanoid_01");
        String government = AUTHORITY_GOVERNMENTS.getOrDefault(empire.authority().id(), "gov_democratic_republic");
        String nameList = SPECIES_NAME_LISTS.getOrDefault(speciesClass, "HUM1");
        String shipset = empire.shipset().id();
        String flagCategory = SPECIES_FLAG_CATEGORIES.getOrDefault(speciesClass, "ornate");
        String flagFile = "flag_" + flagCategory + "_1.dds";
        String authorityId = empire.authority().id();
        String advisorVoice = AUTHORITY_ADVISORS.get(authorityId);

        log.debug("Building Clausewitz empire block for '{}' (class={}, authority={})",
                opts.empireName(), speciesClass, authorityId);

        var sb = new StringBuilder();

        // Empire top-level block
        sb.append(q(opts.empireName())).append("=\n{\n");
        sb.append("\tkey=").append(q(opts.empireName())).append("\n");

        // ship_prefix block
        sb.append("\tship_prefix=\n\t{\n\t\tkey=\"\"\n\t}\n");

        // Primary species block
        sb.append("\tspecies=\n\t{\n");
        sb.append("\t\tclass=").append(q(speciesClass)).append("\n");
        sb.append("\t\tportrait=").append(q(portrait)).append("\n");
        appendNameBlock(sb, "\t\tspecies_name", opts.speciesName());
        appendNameBlock(sb, "\t\tspecies_plural", opts.speciesPlural());
        appendNameBlock(sb, "\t\tspecies_adjective", opts.speciesAdjective());
        sb.append("\t\tname_list=").append(q(nameList)).append("\n");
        sb.append("\t\tgender=not_set\n");
        for (SpeciesTrait trait : empire.speciesTraits()) {
            sb.append("\t\ttrait=").append(q(trait.id())).append("\n");
        }
        sb.append("\t}\n");

        // Secondary species block (if present)
        SecondarySpecies sec = empire.secondarySpecies();
        if (sec != null) {
            appendSecondarySpecies(sb, sec);
        }

        // Empire name and adjective
        appendNameBlock(sb, "\tname", opts.empireName());
        appendNameBlock(sb, "\tadjective", opts.speciesAdjective());

        // Authority and government
        sb.append("\tauthority=").append(q(authorityId)).append("\n");
        sb.append("\tgovernment=").append(q(government)).append("\n");

        // Advisor voice (only for authorities that require it)
        if (advisorVoice != null) {
            sb.append("\tadvisor_voice_type=").append(q(advisorVoice)).append("\n");
        }

        // Homeworld
        appendNameBlock(sb, "\tplanet_name", "Homeworld");
        sb.append("\tplanet_class=").append(q(empire.homeworld().id())).append("\n");
        appendNameBlock(sb, "\tsystem_name", "Home System");
        sb.append("\tinitializer=\"\"\n");

        // Graphical culture (shipset)
        sb.append("\tgraphical_culture=").append(q(shipset)).append("\n");
        sb.append("\tcity_graphical_culture=").append(q(shipset)).append("\n");

        // Flag block
        sb.append("\tempire_flag=\n\t{\n");
        sb.append("\t\ticon=\n\t\t{\n");
        sb.append("\t\t\tcategory=").append(q(flagCategory)).append("\n");
        sb.append("\t\t\tfile=").append(q(flagFile)).append("\n");
        sb.append("\t\t}\n");
        sb.append("\t\tbackground=\n\t\t{\n");
        sb.append("\t\t\tcategory=\"backgrounds\"\n");
        sb.append("\t\t\tfile=\"diagonal.dds\"\n");
        sb.append("\t\t}\n");
        sb.append("\t\tcolors=\n\t\t{\n");
        sb.append("\t\t\t\"blue\"\n");
        sb.append("\t\t\t\"black\"\n");
        sb.append("\t\t\t\"null\"\n");
        sb.append("\t\t\t\"null\"\n");
        sb.append("\t\t}\n");
        sb.append("\t}\n");

        // Ruler block
        sb.append("\truler=\n\t{\n");
        sb.append("\t\tgender=male\n");
        sb.append("\t\tname=\n\t\t{\n\t\t\tfull_names=\n\t\t\t{\n\t\t\t\tkey=")
          .append(q(opts.rulerName())).append("\n\t\t\t}\n\t\t}\n");
        sb.append("\t\tportrait=").append(q(portrait)).append("\n");
        sb.append("\t\ttexture=0\n");
        sb.append("\t\tattachment=0\n");
        sb.append("\t\tclothes=0\n");
        sb.append("\t\truler_title=\n\t\t{\n\t\t\tkey=\"\"\n\t\t}\n");
        sb.append("\t\truler_title_female=\n\t\t{\n\t\t\tkey=\"\"\n\t\t}\n");
        sb.append("\t\tleader_class=").append(q(empire.leaderClass())).append("\n");
        sb.append("\t}\n");

        // Flags
        sb.append("\tspawn_as_fallen=no\n");
        sb.append("\tignore_portrait_duplication=no\n");
        sb.append("\troom=\"default\"\n");
        sb.append("\tspawn_enabled=yes\n");

        // Ethics
        for (var ethic : empire.ethics()) {
            sb.append("\tethic=").append(q(ethic.id())).append("\n");
        }

        // Civics
        sb.append("\tcivics=\n\t{\n");
        for (var civic : empire.civics()) {
            sb.append("\t\t").append(q(civic.id())).append("\n");
        }
        sb.append("\t}\n");

        // Origin
        sb.append("\torigin=").append(q(empire.origin().id())).append("\n");

        sb.append("}\n");

        return sb.toString();
    }

    private void appendSecondarySpecies(StringBuilder sb, SecondarySpecies sec) {
        String secClass = sec.speciesClass();
        String secPortrait = SPECIES_PORTRAITS.getOrDefault(secClass, "humanoid_01");
        String secNameList = SPECIES_NAME_LISTS.getOrDefault(secClass, "HUM1");

        sb.append("\tsecondary_species=\n\t{\n");
        sb.append("\t\tclass=").append(q(secClass)).append("\n");
        sb.append("\t\tportrait=").append(q(secPortrait)).append("\n");
        appendSecNameBlock(sb, "\t\tspecies_name", secClass);
        appendSecNameBlock(sb, "\t\tspecies_plural", secClass);
        appendSecNameBlock(sb, "\t\tspecies_adjective", secClass);
        sb.append("\t\tname_list=").append(q(secNameList)).append("\n");
        sb.append("\t\tgender=not_set\n");

        var allTraits = new ArrayList<SpeciesTrait>(sec.enforcedTraits());
        allTraits.addAll(sec.additionalTraits());
        for (SpeciesTrait trait : allTraits) {
            sb.append("\t\ttrait=").append(q(trait.id())).append("\n");
        }
        sb.append("\t}\n");
    }

    /** Appends a `key = { key = "value" }` block. */
    private void appendNameBlock(StringBuilder sb, String fieldName, String value) {
        sb.append(fieldName).append("=\n");
        // Compute indent from fieldName's leading tabs
        String indent = fieldName.replaceFirst("[^\t].*", "");
        sb.append(indent).append("{\n");
        sb.append(indent).append("\tkey=").append(q(value)).append("\n");
        sb.append(indent).append("}\n");
    }

    /** Like appendNameBlock but uses the class name as a fallback display value. */
    private void appendSecNameBlock(StringBuilder sb, String fieldName, String value) {
        appendNameBlock(sb, fieldName, value);
    }

    private static String q(String value) {
        return "\"" + value + "\"";
    }
}
