package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Ethic;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
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
class EthicExtractorTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";
    private static List<Ethic> ethics;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new GameFileService(props, new SettingsService(props));
        service.loadAll();
        ethics = new EthicExtractor().extract(service.getEthics());
    }

    @Test
    void extractsMultipleEthics() {
        assertTrue(ethics.size() >= 15, "Should have at least 15 ethics (8 regular + 8 fanatic - some + gestalt), got " + ethics.size());
    }

    @Test
    void regularEthicHasCostOne() {
        var auth = ethics.stream().filter(e -> e.id().equals("ethic_authoritarian")).findFirst().orElseThrow();
        assertEquals(1, auth.cost());
        assertFalse(auth.isFanatic());
    }

    @Test
    void fanaticEthicHasCostTwo() {
        var fAuth = ethics.stream().filter(e -> e.id().equals("ethic_fanatic_authoritarian")).findFirst().orElseThrow();
        assertEquals(2, fAuth.cost());
        assertTrue(fAuth.isFanatic());
    }

    @Test
    void fanaticAndRegularVariantsLinked() {
        var fanatic = ethics.stream().filter(e -> e.id().equals("ethic_fanatic_authoritarian")).findFirst().orElseThrow();
        assertEquals("ethic_authoritarian", fanatic.regularVariant());

        var regular = ethics.stream().filter(e -> e.id().equals("ethic_authoritarian")).findFirst().orElseThrow();
        assertEquals("ethic_fanatic_authoritarian", regular.fanaticVariant());
    }

    @Test
    void gestaltEthicDetected() {
        var gestalt = ethics.stream().filter(Ethic::isGestalt).toList();
        assertEquals(1, gestalt.size());
        assertEquals("ethic_gestalt_consciousness", gestalt.getFirst().id());
    }

    @Test
    void tagsExtracted() {
        var auth = ethics.stream().filter(e -> e.id().equals("ethic_authoritarian")).findFirst().orElseThrow();
        assertFalse(auth.tags().isEmpty(), "Authoritarian should have tags");
    }

    @Test
    void randomWeightExtracted() {
        var auth = ethics.stream().filter(e -> e.id().equals("ethic_authoritarian")).findFirst().orElseThrow();
        assertTrue(auth.randomWeight() > 0, "Random weight should be positive");
    }
}
