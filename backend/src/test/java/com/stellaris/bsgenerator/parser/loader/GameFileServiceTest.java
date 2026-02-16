package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.config.SettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameFileServiceTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @Test
    @EnabledIf("gameFilesExist")
    void loadAllFromRealGameFiles() throws IOException {
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new GameFileService(props, new SettingsService(props));
        service.loadAll();

        assertNotNull(service.getEthics());
        assertTrue(service.getEthics().children().size() > 5, "Should have multiple ethics");

        assertNotNull(service.getAuthorities());
        assertTrue(service.getAuthorities().children().size() >= 4, "Should have at least 4 authorities");

        assertNotNull(service.getCivics());
        assertTrue(service.getCivics().children().size() > 20, "Should have many civics");

        assertNotNull(service.getSpeciesArchetypes());
        assertTrue(service.getSpeciesArchetypes().children().size() > 3, "Should have multiple archetypes");

        assertNotNull(service.getTraits());
        assertTrue(service.getTraits().children().size() > 20, "Should have many traits");
    }
}
