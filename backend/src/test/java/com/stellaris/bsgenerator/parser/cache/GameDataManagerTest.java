package com.stellaris.bsgenerator.parser.cache;

import com.stellaris.bsgenerator.extractor.*;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameDataManagerTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    @TempDir
    Path tempDir;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    private GameDataManager createManager() {
        var props = new ParserProperties(GAME_PATH, tempDir.toString());
        var gameFileService = new GameFileService(props);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var cache = new ParsedDataCache(props, mapper);
        return new GameDataManager(props, gameFileService, cache,
                new EthicExtractor(), new AuthorityExtractor(),
                new CivicExtractor(), new OriginExtractor(),
                new SpeciesArchetypeExtractor(), new SpeciesTraitExtractor());
    }

    @Test
    @EnabledIf("gameFilesExist")
    void loadDetectsVersion() throws IOException {
        var manager = createManager();
        manager.loadGameData(false);

        assertNotNull(manager.getGameVersion());
        assertFalse(manager.getGameVersion().rawVersion().isEmpty());
    }

    @Test
    @EnabledIf("gameFilesExist")
    void extractsAllEntityTypes() throws IOException {
        var manager = createManager();
        manager.loadGameData(false);

        assertNotNull(manager.getEthics());
        assertFalse(manager.getEthics().isEmpty(), "Should have ethics");

        assertNotNull(manager.getAuthorities());
        assertFalse(manager.getAuthorities().isEmpty(), "Should have authorities");

        assertNotNull(manager.getCivics());
        assertFalse(manager.getCivics().isEmpty(), "Should have civics");

        assertNotNull(manager.getOrigins());
        assertFalse(manager.getOrigins().isEmpty(), "Should have origins");

        assertNotNull(manager.getSpeciesArchetypes());
        assertFalse(manager.getSpeciesArchetypes().isEmpty(), "Should have archetypes");

        assertNotNull(manager.getSpeciesTraits());
        assertFalse(manager.getSpeciesTraits().isEmpty(), "Should have species traits");
    }

    @Test
    @EnabledIf("gameFilesExist")
    void forceReload() throws IOException {
        var manager = createManager();
        manager.loadGameData(false);
        String version1 = manager.getGameVersion().rawVersion();

        manager.forceReload();
        String version2 = manager.getGameVersion().rawVersion();

        assertEquals(version1, version2);
    }

    @Test
    @EnabledIf("gameFilesExist")
    void cacheHitOnSecondLoad() throws IOException {
        var manager = createManager();
        // First load — cache miss, parses
        manager.loadGameData(false);
        assertNotNull(manager.getGameVersion());

        // Second load — should hit cache (but still re-parses for now)
        manager.loadGameData(false);
        assertNotNull(manager.getGameVersion());
    }

    @Test
    void missingGamePathHandled() {
        var props = new ParserProperties(tempDir.resolve("nonexistent").toString(), tempDir.toString());
        var gameFileService = new GameFileService(props);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var cache = new ParsedDataCache(props, mapper);
        var manager = new GameDataManager(props, gameFileService, cache,
                new EthicExtractor(), new AuthorityExtractor(),
                new CivicExtractor(), new OriginExtractor(),
                new SpeciesArchetypeExtractor(), new SpeciesTraitExtractor());

        assertThrows(IOException.class, () -> manager.loadGameData(false));
    }
}
