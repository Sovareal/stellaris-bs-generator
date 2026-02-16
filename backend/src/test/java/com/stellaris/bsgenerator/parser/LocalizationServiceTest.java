package com.stellaris.bsgenerator.parser;

import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.config.SettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalizationServiceTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "localisation", "english"));
    }

    @Test
    @EnabledIf("gameFilesExist")
    void loadsLocalizationKeys() {
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new LocalizationService(props, new SettingsService(props));
        service.load();

        assertFalse(service.getLocalizations().isEmpty(), "Should load localization keys");
        assertTrue(service.getLocalizations().size() > 1000, "Should have many keys");
    }

    @Test
    @EnabledIf("gameFilesExist")
    void resolvesKeyWithoutVersionDigit() {
        // origin_void_machines: "Voidforged" (no digit after colon)
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new LocalizationService(props, new SettingsService(props));
        service.load();

        String name = service.getDisplayName("origin_void_machines");
        assertNotNull(name, "origin_void_machines should be resolved (colon without digit)");
        assertEquals("Voidforged", name);
    }

    @Test
    @EnabledIf("gameFilesExist")
    void resolvesVariableReferences() {
        // trait_fleeting_lithoid:0 "$trait_fleeting$" should resolve to "Fleeting"
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new LocalizationService(props, new SettingsService(props));
        service.load();

        String fleeting = service.getDisplayName("trait_fleeting");
        assertNotNull(fleeting, "trait_fleeting should exist");
        assertEquals("Fleeting", fleeting);

        String fleetingLithoid = service.getDisplayName("trait_fleeting_lithoid");
        assertNotNull(fleetingLithoid, "trait_fleeting_lithoid should be resolved via $variable$");
        assertEquals("Fleeting", fleetingLithoid);
    }

    @Test
    @EnabledIf("gameFilesExist")
    void resolvesTrWithoutDigit() {
        // trait_humanoid_jinxed: "Jinxed" (no digit after colon)
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new LocalizationService(props, new SettingsService(props));
        service.load();

        String name = service.getDisplayName("trait_humanoid_jinxed");
        assertNotNull(name, "trait_humanoid_jinxed should be resolved (colon without digit)");
        assertEquals("Jinxed", name);
    }
}
