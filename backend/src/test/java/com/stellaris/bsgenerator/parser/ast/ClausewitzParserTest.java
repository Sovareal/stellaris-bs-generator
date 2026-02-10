package com.stellaris.bsgenerator.parser.ast;

import com.stellaris.bsgenerator.parser.token.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClausewitzParserTest {

    private ClausewitzNode parse(String input) {
        return parse(input, new HashMap<>());
    }

    private ClausewitzNode parse(String input, Map<String, String> vars) {
        return ClausewitzParser.parse(Tokenizer.tokenize(input), vars);
    }

    @Test
    void simpleKeyValue() {
        var root = parse("cost = 2");
        assertEquals(1, root.children().size());
        assertEquals("2", root.childValue("cost").orElseThrow());
        assertEquals(2, root.childInt("cost", 0));
    }

    @Test
    void stringValue() {
        var root = parse("category = \"col\"");
        assertEquals("col", root.childValue("category").orElseThrow());
    }

    @Test
    void nestedBlock() {
        var root = parse("ethic = { cost = 2 category = \"col\" }");
        var ethic = root.child("ethic").orElseThrow();
        assertTrue(ethic.isBlock());
        assertEquals("2", ethic.childValue("cost").orElseThrow());
        assertEquals("col", ethic.childValue("category").orElseThrow());
    }

    @Test
    void deeplyNestedBlocks() {
        var root = parse("""
                outer = {
                    middle = {
                        inner = {
                            value = 42
                        }
                    }
                }
                """);
        var inner = root.child("outer").orElseThrow()
                .child("middle").orElseThrow()
                .child("inner").orElseThrow();
        assertEquals("42", inner.childValue("value").orElseThrow());
    }

    @Test
    void bareValues() {
        var root = parse("tags = { TAG_A TAG_B TAG_C }");
        var tags = root.child("tags").orElseThrow();
        assertEquals(3, tags.bareValues().size());
        assertEquals("TAG_A", tags.bareValues().get(0));
        assertEquals("TAG_B", tags.bareValues().get(1));
        assertEquals("TAG_C", tags.bareValues().get(2));
    }

    @Test
    void repeatedKeys() {
        var root = parse("""
                pop_attraction_tag = { desc = first }
                pop_attraction_tag = { desc = second }
                """);
        var all = root.children("pop_attraction_tag");
        assertEquals(2, all.size());
        assertEquals("first", all.get(0).childValue("desc").orElseThrow());
        assertEquals("second", all.get(1).childValue("desc").orElseThrow());
    }

    @Test
    void booleanValues() {
        var root = parse("is_origin = yes\npickable = no");
        assertTrue(root.childBool("is_origin", false));
        assertFalse(root.childBool("pickable", true));
    }

    @Test
    void variableDefinitionAndResolution() {
        var root = parse("""
                @my_weight = 5
                random_weight = { base = @my_weight }
                """);
        var rw = root.child("random_weight").orElseThrow();
        assertEquals("5", rw.childValue("base").orElseThrow());
    }

    @Test
    void preExistingVariables() {
        var vars = new HashMap<>(Map.of("civic_default_random_weight", "5"));
        var root = parse("random_weight = { base = @civic_default_random_weight }", vars);
        var rw = root.child("random_weight").orElseThrow();
        assertEquals("5", rw.childValue("base").orElseThrow());
    }

    @Test
    void undefinedVariableThrows() {
        assertThrows(ParseException.class, () -> parse("val = @undefined_var"));
    }

    @Test
    void comparisonOperator() {
        var root = parse("count > 0");
        assertEquals("> 0", root.childValue("count").orElseThrow());
    }

    @Test
    void childDefaultValues() {
        var root = parse("a = 1");
        assertEquals(99, root.childInt("missing", 99));
        assertEquals(1.5, root.childDouble("missing", 1.5), 0.001);
        assertTrue(root.childBool("missing", true));
    }

    @Test
    void realCivicSnippet() {
        String input = """
                civic_corvee_system = {
                    description = "civic_corvee_system_effects"
                    potential = {
                        ethics = { NOT = { value = ethic_gestalt_consciousness } }
                        authority = { NOT = { value = auth_corporate } }
                    }
                    possible = {
                        ethics = {
                            NOR = {
                                text = civic_tooltip_not_egalitarian
                                value = ethic_egalitarian
                                value = ethic_fanatic_egalitarian
                            }
                        }
                        civics = {
                            NOT = {
                                value = civic_free_haven
                            }
                        }
                    }
                }
                """;
        var root = parse(input);
        var civic = root.child("civic_corvee_system").orElseThrow();
        assertEquals("civic_corvee_system_effects", civic.childValue("description").orElseThrow());

        var potential = civic.child("potential").orElseThrow();
        assertFalse(potential.children().isEmpty());
        var potentialEthics = potential.child("ethics").orElseThrow();
        var ethicsNot = potentialEthics.child("NOT").orElseThrow();
        assertEquals("ethic_gestalt_consciousness", ethicsNot.childValue("value").orElseThrow());

        var possible = civic.child("possible").orElseThrow();
        var possibleEthics = possible.child("ethics").orElseThrow();
        var nor = possibleEthics.child("NOR").orElseThrow();
        var norValues = nor.children("value");
        assertEquals(2, norValues.size());
    }

    @Test
    void realEthicsSnippet() {
        String input = """
                ethic_fanatic_authoritarian = {
                    cost = 2
                    category = "col"
                    category_value = 0
                    use_for_pops = no
                    regular_variant = ethic_authoritarian
                    country_modifier = {
                        country_ethic_influence_produces_add = 1
                        planet_jobs_worker_produces_mult = 0.1
                    }
                    tags = {
                        ETHIC_ALLOWS_STRATIFIED_SOCIETY
                        ETHIC_ALLOWS_SLAVERY
                        ETHIC_ONLY_AUTOCRACY
                    }
                    random_weight = {
                        base = 150
                    }
                }
                """;
        var root = parse(input);
        var ethic = root.child("ethic_fanatic_authoritarian").orElseThrow();
        assertEquals(2, ethic.childInt("cost", 0));
        assertEquals("col", ethic.childValue("category").orElseThrow());
        assertFalse(ethic.childBool("use_for_pops", true));
        assertEquals("ethic_authoritarian", ethic.childValue("regular_variant").orElseThrow());

        var tags = ethic.child("tags").orElseThrow();
        assertEquals(3, tags.bareValues().size());
        assertTrue(tags.bareValues().contains("ETHIC_ALLOWS_SLAVERY"));

        var rw = ethic.child("random_weight").orElseThrow();
        assertEquals(150, rw.childInt("base", 0));
    }
}
