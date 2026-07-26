package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.SpeciesTrait;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("gameFilesExist")
class SpeciesTraitExtractorTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";
    private static List<SpeciesTrait> traits;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new GameFileService(props, new SettingsService(props));
        service.loadAll();
        traits = new SpeciesTraitExtractor().extract(service.getTraits());
    }

    @Test
    void extractsManyTraits() {
        assertTrue(traits.size() > 20, "Should have many creation-eligible traits, got " + traits.size());
    }

    @Test
    void agrarianTraitCorrect() {
        var agrarian = findById("trait_agrarian");
        assertEquals(2, agrarian.cost());
        assertTrue(agrarian.allowedArchetypes().contains("BIOLOGICAL"));
        assertTrue(agrarian.allowedArchetypes().contains("LITHOID"));
    }

    @Test
    void roboticTraitsExist() {
        var roboticTraits = traits.stream()
                .filter(t -> t.allowedArchetypes().contains("ROBOT") || t.allowedArchetypes().contains("MACHINE"))
                .toList();
        assertFalse(roboticTraits.isEmpty(), "Should have some robotic traits");
    }

    @Test
    void oppositesExtracted() {
        // trait_rapid_breeders should have opposites
        var rapid = findById("trait_rapid_breeders");
        assertFalse(rapid.opposites().isEmpty(), "trait_rapid_breeders should have opposites");
        assertTrue(rapid.opposites().contains("trait_slow_breeders"),
                "trait_rapid_breeders should oppose trait_slow_breeders");
    }

    @Test
    void noAutoModTraits() {
        // Auto-mod traits have initial = no, should be excluded
        assertTrue(traits.stream().noneMatch(t -> t.id().equals("trait_auto_mod_biological")),
                "Should not contain auto-mod traits (initial = no)");
        assertTrue(traits.stream().noneMatch(t -> t.id().equals("trait_auto_mod_robotic")),
                "Should not contain robotic auto-mod traits (initial = no)");
    }

    @Test
    void noHabitabilityTraits() {
        // Traits without cost field should be excluded (habitability traits)
        for (var trait : traits) {
            assertNotNull(trait.allowedArchetypes(), trait.id() + " should have allowed archetypes");
            assertFalse(trait.allowedArchetypes().isEmpty(), trait.id() + " should have non-empty allowed archetypes");
        }
    }

    @Test
    void tagsExtracted() {
        var agrarian = findById("trait_agrarian");
        assertFalse(agrarian.tags().isEmpty(), "trait_agrarian should have tags");
    }

    @Test
    void loyaltyCircuitsExcludesHiveMind() {
        // trait_cyborg_loyalty_circuits declares its Hive Mind conflict via
        // species_potential_add = { NOT = { has_trait = trait_hive_mind } } }, not via the
        // `opposites` list -- a second conflict mechanism (phase 80, issue 3) that must be
        // merged into `opposites` at extraction time.
        var loyaltyCircuits = findById("trait_cyborg_loyalty_circuits");
        assertTrue(loyaltyCircuits.opposites().contains("trait_hive_mind"),
                "trait_cyborg_loyalty_circuits should exclude trait_hive_mind");
        // Its declared opposites entry should still be preserved alongside the merged one
        assertTrue(loyaltyCircuits.opposites().contains("trait_cyborg_apathy_loops"),
                "trait_cyborg_loyalty_circuits should still oppose trait_cyborg_apathy_loops");
    }

    @Test
    void allTraitOppositesCapturesInitialNoTraits() {
        // trait_pathogenic_genes (initial = no, granted by origin_synthetic_fertility) is
        // excluded from the creation-eligible pool, but its opposites list must still be
        // recoverable for stubbing the enforced trait (phase 80, issue 1).
        var service = new GameFileService(
                new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir")),
                new SettingsService(new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"))));
        try {
            service.loadAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var allOpposites = new SpeciesTraitExtractor().extractAllTraitOpposites(service.getTraits());

        assertTrue(traits.stream().noneMatch(t -> t.id().equals("trait_pathogenic_genes")),
                "trait_pathogenic_genes should NOT be in the creation-eligible pool (initial = no)");

        var pathogenicOpposites = allOpposites.get("trait_pathogenic_genes");
        assertNotNull(pathogenicOpposites, "trait_pathogenic_genes should still have an opposites entry");
        for (var expected : List.of("trait_rapid_breeders", "trait_slow_breeders", "trait_adaptive",
                "trait_extremely_adaptive", "trait_nonadaptive", "trait_sedentary", "trait_egg_laying",
                "trait_nascent_stage", "trait_incubator")) {
            assertTrue(pathogenicOpposites.contains(expected),
                    "trait_pathogenic_genes opposites should contain " + expected);
        }
    }

    private SpeciesTrait findById(String id) {
        return traits.stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Trait " + id + " not found"));
    }

    private static class AssertionError extends RuntimeException {
        AssertionError(String msg) { super(msg); }
    }
}
