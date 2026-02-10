package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementBlock;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RequirementEvaluatorTest {

    private final RequirementEvaluator evaluator = new RequirementEvaluator();

    @Test
    void nullBlockPasses() {
        assertTrue(evaluator.evaluate(null, EmpireState.empty()));
    }

    @Test
    void emptyBlockPasses() {
        var block = new RequirementBlock(Map.of());
        assertTrue(evaluator.evaluate(block, EmpireState.empty()));
    }

    @Test
    void unselectedCategoryIsSkipped() {
        // Require ethic_gestalt but ethics aren't selected yet → passes (deferred)
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(new Requirement.Value("ethic_gestalt_consciousness"))
        ));
        assertTrue(evaluator.evaluate(block, EmpireState.empty()));
    }

    // --- auth_democratic possible ---
    // ethics = { NOR = { value = ethic_gestalt_consciousness  value = ethic_authoritarian  value = ethic_fanatic_authoritarian } }
    @Test
    void democraticRejectsAuthoritarian() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(
                        new Requirement.Nor(List.of("ethic_gestalt_consciousness", "ethic_authoritarian", "ethic_fanatic_authoritarian"))
                )
        ));
        var state = EmpireState.empty().withEthics(Set.of("ethic_authoritarian", "ethic_militarist", "ethic_xenophobe"));
        assertFalse(evaluator.evaluate(block, state));
    }

    @Test
    void democraticAcceptsEgalitarian() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(
                        new Requirement.Nor(List.of("ethic_gestalt_consciousness", "ethic_authoritarian", "ethic_fanatic_authoritarian"))
                )
        ));
        var state = EmpireState.empty().withEthics(Set.of("ethic_egalitarian", "ethic_militarist", "ethic_xenophobe"));
        assertTrue(evaluator.evaluate(block, state));
    }

    // --- civic_corvee_system potential ---
    // ethics = { NOT = { value = ethic_gestalt_consciousness } }
    // authority = { NOT = { value = auth_corporate } }
    @Test
    void corveeSystemPotentialAcceptsNonGestaltNonCorporate() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(new Requirement.Not("ethic_gestalt_consciousness")),
                RequirementCategory.AUTHORITY, List.of(new Requirement.Not("auth_corporate"))
        ));
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_authoritarian", "ethic_militarist"))
                .withAuthority("auth_imperial");
        assertTrue(evaluator.evaluate(block, state));
    }

    @Test
    void corveeSystemPotentialRejectsGestalt() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(new Requirement.Not("ethic_gestalt_consciousness")),
                RequirementCategory.AUTHORITY, List.of(new Requirement.Not("auth_corporate"))
        ));
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_gestalt_consciousness"))
                .withAuthority("auth_hive_mind");
        assertFalse(evaluator.evaluate(block, state));
    }

    // --- civic_corvee_system possible ---
    // ethics = { NOR = { value = ethic_egalitarian  value = ethic_fanatic_egalitarian } }
    // civics = { NOT = { value = civic_free_haven } }
    @Test
    void corveeSystemPossibleRejectsEgalitarian() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(
                        new Requirement.Nor(List.of("ethic_egalitarian", "ethic_fanatic_egalitarian"))
                ),
                RequirementCategory.CIVICS, List.of(new Requirement.Not("civic_free_haven"))
        ));
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_egalitarian", "ethic_xenophile"))
                .withCivics(Set.of("civic_meritocracy"));
        assertFalse(evaluator.evaluate(block, state));
    }

    @Test
    void corveeSystemPossibleRejectsFreeHaven() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(
                        new Requirement.Nor(List.of("ethic_egalitarian", "ethic_fanatic_egalitarian"))
                ),
                RequirementCategory.CIVICS, List.of(new Requirement.Not("civic_free_haven"))
        ));
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_authoritarian", "ethic_militarist"))
                .withCivics(Set.of("civic_free_haven"));
        assertFalse(evaluator.evaluate(block, state));
    }

    @Test
    void corveeSystemPossibleAcceptsValidCombo() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(
                        new Requirement.Nor(List.of("ethic_egalitarian", "ethic_fanatic_egalitarian"))
                ),
                RequirementCategory.CIVICS, List.of(new Requirement.Not("civic_free_haven"))
        ));
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_authoritarian", "ethic_militarist"))
                .withCivics(Set.of("civic_meritocracy"));
        assertTrue(evaluator.evaluate(block, state));
    }

    // --- civic_imperial_cult possible ---
    // authority = { value = auth_imperial }
    // ethics = { OR = { value = ethic_authoritarian  value = ethic_fanatic_authoritarian }
    //            OR = { value = ethic_spiritualist   value = ethic_fanatic_spiritualist } }
    @Test
    void imperialCultRequiresBothOrBlocks() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.AUTHORITY, List.of(new Requirement.Value("auth_imperial")),
                RequirementCategory.ETHICS, List.of(
                        new Requirement.Or(List.of("ethic_authoritarian", "ethic_fanatic_authoritarian")),
                        new Requirement.Or(List.of("ethic_spiritualist", "ethic_fanatic_spiritualist"))
                )
        ));

        // Has both auth + spiritualist → passes
        var valid = EmpireState.empty()
                .withAuthority("auth_imperial")
                .withEthics(Set.of("ethic_fanatic_authoritarian", "ethic_spiritualist"));
        assertTrue(evaluator.evaluate(block, valid));

        // Has auth_imperial + authoritarian but NOT spiritualist → fails
        var missingSpiritualist = EmpireState.empty()
                .withAuthority("auth_imperial")
                .withEthics(Set.of("ethic_authoritarian", "ethic_militarist"));
        assertFalse(evaluator.evaluate(block, missingSpiritualist));

        // Has auth_democratic → fails
        var wrongAuth = EmpireState.empty()
                .withAuthority("auth_democratic")
                .withEthics(Set.of("ethic_fanatic_authoritarian", "ethic_spiritualist"));
        assertFalse(evaluator.evaluate(block, wrongAuth));
    }

    // --- auth_hive_mind possible ---
    // ethics = { value = ethic_gestalt_consciousness }
    // species_archetype = { NOT = { value = MACHINE } }
    @Test
    void hiveMindRequiresGestaltAndNonMachine() {
        var block = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(new Requirement.Value("ethic_gestalt_consciousness")),
                RequirementCategory.SPECIES_ARCHETYPE, List.of(new Requirement.Not("MACHINE"))
        ));

        var valid = EmpireState.empty()
                .withEthics(Set.of("ethic_gestalt_consciousness"))
                .withSpeciesArchetype("BIOLOGICAL");
        assertTrue(evaluator.evaluate(block, valid));

        var machine = EmpireState.empty()
                .withEthics(Set.of("ethic_gestalt_consciousness"))
                .withSpeciesArchetype("MACHINE");
        assertFalse(evaluator.evaluate(block, machine));
    }

    @Test
    void evaluateBothChecksPotentalAndPossible() {
        var potential = new RequirementBlock(Map.of(
                RequirementCategory.ETHICS, List.of(new Requirement.Not("ethic_gestalt_consciousness"))
        ));
        var possible = new RequirementBlock(Map.of(
                RequirementCategory.AUTHORITY, List.of(new Requirement.Value("auth_imperial"))
        ));

        // Passes both
        var valid = EmpireState.empty()
                .withEthics(Set.of("ethic_authoritarian"))
                .withAuthority("auth_imperial");
        assertTrue(evaluator.evaluateBoth(potential, possible, valid));

        // Fails potential (gestalt)
        var gestalt = EmpireState.empty()
                .withEthics(Set.of("ethic_gestalt_consciousness"))
                .withAuthority("auth_imperial");
        assertFalse(evaluator.evaluateBoth(potential, possible, gestalt));

        // Fails possible (wrong authority)
        var wrongAuth = EmpireState.empty()
                .withEthics(Set.of("ethic_authoritarian"))
                .withAuthority("auth_democratic");
        assertFalse(evaluator.evaluateBoth(potential, possible, wrongAuth));
    }
}
