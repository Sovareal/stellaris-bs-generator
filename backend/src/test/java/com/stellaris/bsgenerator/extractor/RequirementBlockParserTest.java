package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequirementBlockParserTest {

    @Test
    void nullNodeReturnsNull() {
        assertNull(RequirementBlockParser.parse(null));
    }

    @Test
    void emptyBlockReturnsNull() {
        var node = ClausewitzNode.block("possible", List.of());
        assertNull(RequirementBlockParser.parse(node));
    }

    @Test
    void alwaysYesOnlyReturnsNull() {
        var node = ClausewitzNode.block("potential", List.of(
                ClausewitzNode.leaf("always", "yes")
        ));
        assertNull(RequirementBlockParser.parse(node));
    }

    @Test
    void singleValueRequirement() {
        // ethics = { value = ethic_gestalt_consciousness }
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(
                ClausewitzNode.leaf("value", "ethic_gestalt_consciousness")
        ));
        var node = ClausewitzNode.block("possible", List.of(ethicsBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        assertTrue(result.hasCategory(RequirementCategory.ETHICS));
        List<Requirement> reqs = result.get(RequirementCategory.ETHICS);
        assertEquals(1, reqs.size());
        assertInstanceOf(Requirement.Value.class, reqs.getFirst());
        assertEquals("ethic_gestalt_consciousness", ((Requirement.Value) reqs.getFirst()).value());
    }

    @Test
    void notRequirement() {
        // ethics = { NOT = { value = ethic_gestalt_consciousness } }
        var notBlock = ClausewitzNode.block("NOT", List.of(
                ClausewitzNode.leaf("value", "ethic_gestalt_consciousness")
        ));
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(notBlock));
        var node = ClausewitzNode.block("possible", List.of(ethicsBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        List<Requirement> reqs = result.get(RequirementCategory.ETHICS);
        assertEquals(1, reqs.size());
        assertInstanceOf(Requirement.Not.class, reqs.getFirst());
        assertEquals("ethic_gestalt_consciousness", ((Requirement.Not) reqs.getFirst()).value());
    }

    @Test
    void norRequirement() {
        // ethics = { NOR = { value = ethic_gestalt_consciousness  value = ethic_authoritarian } }
        var norBlock = ClausewitzNode.block("NOR", List.of(
                ClausewitzNode.leaf("value", "ethic_gestalt_consciousness"),
                ClausewitzNode.leaf("value", "ethic_authoritarian")
        ));
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(norBlock));
        var node = ClausewitzNode.block("possible", List.of(ethicsBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        List<Requirement> reqs = result.get(RequirementCategory.ETHICS);
        assertEquals(1, reqs.size());
        assertInstanceOf(Requirement.Nor.class, reqs.getFirst());
        assertEquals(List.of("ethic_gestalt_consciousness", "ethic_authoritarian"),
                ((Requirement.Nor) reqs.getFirst()).values());
    }

    @Test
    void orRequirement() {
        // ethics = { OR = { value = ethic_authoritarian  value = ethic_fanatic_authoritarian } }
        var orBlock = ClausewitzNode.block("OR", List.of(
                ClausewitzNode.leaf("value", "ethic_authoritarian"),
                ClausewitzNode.leaf("value", "ethic_fanatic_authoritarian")
        ));
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(orBlock));
        var node = ClausewitzNode.block("possible", List.of(ethicsBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        List<Requirement> reqs = result.get(RequirementCategory.ETHICS);
        assertEquals(1, reqs.size());
        assertInstanceOf(Requirement.Or.class, reqs.getFirst());
        assertEquals(List.of("ethic_authoritarian", "ethic_fanatic_authoritarian"),
                ((Requirement.Or) reqs.getFirst()).values());
    }

    @Test
    void multiCategoryBlock() {
        // possible = {
        //     ethics = { NOT = { value = ethic_gestalt_consciousness } }
        //     authority = { NOT = { value = auth_corporate } }
        // }
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(
                ClausewitzNode.block("NOT", List.of(
                        ClausewitzNode.leaf("value", "ethic_gestalt_consciousness")
                ))
        ));
        var authorityBlock = ClausewitzNode.block("authority", List.of(
                ClausewitzNode.block("NOT", List.of(
                        ClausewitzNode.leaf("value", "auth_corporate")
                ))
        ));
        var node = ClausewitzNode.block("possible", List.of(ethicsBlock, authorityBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        assertTrue(result.hasCategory(RequirementCategory.ETHICS));
        assertTrue(result.hasCategory(RequirementCategory.AUTHORITY));
        assertEquals(1, result.get(RequirementCategory.ETHICS).size());
        assertEquals(1, result.get(RequirementCategory.AUTHORITY).size());
    }

    @Test
    void textEntriesIgnored() {
        // ethics = { NOR = { text = civic_tooltip  value = ethic_x  value = ethic_y } }
        var norBlock = ClausewitzNode.block("NOR", List.of(
                ClausewitzNode.leaf("text", "civic_tooltip_not_egalitarian"),
                ClausewitzNode.leaf("value", "ethic_egalitarian"),
                ClausewitzNode.leaf("value", "ethic_fanatic_egalitarian")
        ));
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(norBlock));
        var node = ClausewitzNode.block("possible", List.of(ethicsBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        List<Requirement> reqs = result.get(RequirementCategory.ETHICS);
        assertEquals(1, reqs.size());
        assertInstanceOf(Requirement.Nor.class, reqs.getFirst());
        // Text entries are not values, so only 2 values
        assertEquals(List.of("ethic_egalitarian", "ethic_fanatic_egalitarian"),
                ((Requirement.Nor) reqs.getFirst()).values());
    }

    @Test
    void mixedRequirementsInOneCategory() {
        // civic_imperial_cult possible:
        // ethics = { OR = { value = ethic_authoritarian  value = ethic_fanatic_authoritarian }
        //            OR = { value = ethic_spiritualist   value = ethic_fanatic_spiritualist } }
        var or1 = ClausewitzNode.block("OR", List.of(
                ClausewitzNode.leaf("value", "ethic_authoritarian"),
                ClausewitzNode.leaf("value", "ethic_fanatic_authoritarian")
        ));
        var or2 = ClausewitzNode.block("OR", List.of(
                ClausewitzNode.leaf("value", "ethic_spiritualist"),
                ClausewitzNode.leaf("value", "ethic_fanatic_spiritualist")
        ));
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(or1, or2));
        var authorityBlock = ClausewitzNode.block("authority", List.of(
                ClausewitzNode.leaf("value", "auth_imperial")
        ));
        var node = ClausewitzNode.block("possible", List.of(authorityBlock, ethicsBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        assertEquals(2, result.get(RequirementCategory.ETHICS).size());
        assertEquals(1, result.get(RequirementCategory.AUTHORITY).size());
        // Both ethics requirements should be OR
        assertInstanceOf(Requirement.Or.class, result.get(RequirementCategory.ETHICS).get(0));
        assertInstanceOf(Requirement.Or.class, result.get(RequirementCategory.ETHICS).get(1));
    }

    @Test
    void unrecognizedCategoryKeysIgnored() {
        // Some potential blocks have non-category keys like AND = { limit = { ... } }
        var andBlock = ClausewitzNode.block("AND", List.of(
                ClausewitzNode.leaf("always", "yes")
        ));
        var ethicsBlock = ClausewitzNode.block("ethics", List.of(
                ClausewitzNode.leaf("value", "ethic_gestalt_consciousness")
        ));
        var node = ClausewitzNode.block("possible", List.of(andBlock, ethicsBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        assertTrue(result.hasCategory(RequirementCategory.ETHICS));
        assertFalse(result.hasCategory(RequirementCategory.AUTHORITY));
    }

    @Test
    void speciesArchetypeCategory() {
        // species_archetype = { NOT = { value = MACHINE } }
        var saBlock = ClausewitzNode.block("species_archetype", List.of(
                ClausewitzNode.block("NOT", List.of(
                        ClausewitzNode.leaf("value", "MACHINE")
                ))
        ));
        var node = ClausewitzNode.block("possible", List.of(saBlock));

        RequirementBlock result = RequirementBlockParser.parse(node);

        assertNotNull(result);
        assertTrue(result.hasCategory(RequirementCategory.SPECIES_ARCHETYPE));
        assertInstanceOf(Requirement.Not.class, result.get(RequirementCategory.SPECIES_ARCHETYPE).getFirst());
    }
}
