package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.SpeciesTrait;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
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
        var service = new GameFileService(new ParserProperties(GAME_PATH, null));
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

    private SpeciesTrait findById(String id) {
        return traits.stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Trait " + id + " not found"));
    }

    private static class AssertionError extends RuntimeException {
        AssertionError(String msg) { super(msg); }
    }
}
