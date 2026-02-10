package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.SpeciesArchetype;
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
class SpeciesArchetypeExtractorTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";
    private static List<SpeciesArchetype> archetypes;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var service = new GameFileService(new ParserProperties(GAME_PATH, null));
        service.loadAll();
        archetypes = new SpeciesArchetypeExtractor().extract(service.getSpeciesArchetypes());
    }

    @Test
    void extractsAllArchetypes() {
        assertTrue(archetypes.size() >= 6, "Should have at least 6 archetypes, got " + archetypes.size());
    }

    @Test
    void biologicalArchetype() {
        var bio = findById("BIOLOGICAL");
        assertEquals(2, bio.traitPoints());
        assertEquals(5, bio.maxTraits());
        assertFalse(bio.robotic());
    }

    @Test
    void robotArchetype() {
        var robot = findById("ROBOT");
        assertEquals(0, robot.traitPoints());
        assertEquals(4, robot.maxTraits());
        assertTrue(robot.robotic());
    }

    @Test
    void machineArchetype() {
        var machine = findById("MACHINE");
        assertEquals(1, machine.traitPoints());
        assertEquals(5, machine.maxTraits());
        assertTrue(machine.robotic());
    }

    @Test
    void lithoidInheritsFromBiological() {
        var lithoid = findById("LITHOID");
        assertEquals(2, lithoid.traitPoints(), "LITHOID should inherit 2 trait points from BIOLOGICAL");
        assertEquals(5, lithoid.maxTraits(), "LITHOID should inherit 5 max traits from BIOLOGICAL");
        assertFalse(lithoid.robotic());
    }

    private SpeciesArchetype findById(String id) {
        return archetypes.stream().filter(a -> a.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Archetype " + id + " not found"));
    }

    // Custom assertion error for better messages
    private static class AssertionError extends RuntimeException {
        AssertionError(String msg) { super(msg); }
    }
}
