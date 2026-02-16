package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Origin;
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
class OriginExtractorTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";
    private static List<Origin> origins;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new GameFileService(props, new SettingsService(props));
        service.loadAll();
        origins = new OriginExtractor().extract(service.getCivics());
    }

    @Test
    void extractsPlayableOrigins() {
        assertTrue(origins.size() >= 10, "Should have at least 10 playable origins, got " + origins.size());
    }

    @Test
    void originDefaultExists() {
        var def = origins.stream().filter(o -> o.id().equals("origin_default")).findFirst().orElseThrow();
        assertEquals(100, def.randomWeight(), "origin_default should have weight 100");
    }

    @Test
    void nonPlayableOriginsExcluded() {
        // Origins from 01_origins_non_playable.txt with playable = { always = no } should be excluded
        assertTrue(origins.stream().noneMatch(o -> o.id().equals("origin_fallen_empire")),
                "Should not contain non-playable origin_fallen_empire");
    }

    @Test
    void dlcGatedOriginsHaveDlcRequirement() {
        // Some origins require specific DLC
        var dlcOrigins = origins.stream().filter(o -> o.dlcRequirement() != null).toList();
        // There should be at least some DLC-gated origins
        // (Not all games have the same DLC, so just check structure)
        for (var origin : dlcOrigins) {
            assertFalse(origin.dlcRequirement().isBlank(), origin.id() + " DLC requirement should not be blank");
        }
    }

    @Test
    void necrophageHasSecondarySpecies() {
        var necro = origins.stream().filter(o -> o.id().equals("origin_necrophage")).findFirst().orElseThrow();
        assertNotNull(necro.secondarySpecies(), "Necrophage should have secondary species config");
        assertEquals("civic_necrophage_secondary_species", necro.secondarySpecies().title());
        assertTrue(necro.secondarySpecies().enforcedTraitIds().isEmpty(),
                "Necrophage prepatent species should have no enforced traits");
    }

    @Test
    void syncreticEvolutionHasSecondarySpeciesWithEnforcedTrait() {
        var syncretic = origins.stream().filter(o -> o.id().equals("origin_syncretic_evolution")).findFirst().orElseThrow();
        assertNotNull(syncretic.secondarySpecies(), "Syncretic Evolution should have secondary species config");
        assertEquals("civic_syncretic_evolution_secondary_species", syncretic.secondarySpecies().title());
        assertEquals(1, syncretic.secondarySpecies().enforcedTraitIds().size());
        assertEquals("trait_syncretic_proles", syncretic.secondarySpecies().enforcedTraitIds().get(0));
    }

    @Test
    void originDefaultHasNoSecondarySpecies() {
        var def = origins.stream().filter(o -> o.id().equals("origin_default")).findFirst().orElseThrow();
        assertNull(def.secondarySpecies(), "origin_default should have no secondary species");
    }

    @Test
    void allOriginsHaveNonNegativeWeight() {
        for (var origin : origins) {
            assertTrue(origin.randomWeight() >= 0, origin.id() + " should have non-negative random weight");
        }
    }

    @Test
    void originDefaultHasHighWeight() {
        var def = origins.stream().filter(o -> o.id().equals("origin_default")).findFirst().orElseThrow();
        assertTrue(def.randomWeight() > 0, "origin_default should have positive weight");
    }
}
