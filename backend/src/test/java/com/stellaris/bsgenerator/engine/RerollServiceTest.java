package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.extractor.*;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import com.stellaris.bsgenerator.parser.cache.ParsedDataCache;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("gameFilesExist")
class RerollServiceTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    @TempDir
    static Path tempDir;

    private static EmpireGeneratorService generator;
    private static RerollService rerollService;
    private GenerationSession session;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUpOnce() throws IOException {
        var props = new ParserProperties(GAME_PATH, tempDir.toString());
        var gameFileService = new GameFileService(props);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var cache = new ParsedDataCache(props, mapper);
        var gameDataManager = new GameDataManager(props, gameFileService, cache,
                new EthicExtractor(), new AuthorityExtractor(),
                new CivicExtractor(), new OriginExtractor(),
                new SpeciesArchetypeExtractor(), new SpeciesTraitExtractor(),
                new PlanetClassExtractor(), new GraphicalCultureExtractor(),
                new StartingRulerTraitExtractor(), new SpeciesClassExtractor());
        gameDataManager.loadGameData(false);

        var evaluator = new RequirementEvaluator();
        var filterService = new CompatibilityFilterService(gameDataManager, evaluator);
        generator = new EmpireGeneratorService(filterService, evaluator);
        rerollService = new RerollService(filterService, evaluator, generator);
    }

    @BeforeEach
    void setUp() {
        var empire = generator.generate();
        session = new GenerationSession(empire);
    }

    @Test
    void rerollAuthority() {
        var updated = rerollService.reroll(session, RerollCategory.AUTHORITY);

        assertNotNull(updated);
        assertFalse(session.canReroll(), "Reroll should be used up");
        assertEquals(2, updated.civics().size(), "Civics should still be 2");
    }

    @Test
    void rerollCivic1() {
        var originalCivic2 = session.getEmpire().civics().get(1).id();
        var updated = rerollService.reroll(session, RerollCategory.CIVIC1);

        assertNotNull(updated);
        assertEquals(originalCivic2, updated.civics().get(1).id(), "Civic 2 should be unchanged");
        assertFalse(session.canReroll());
    }

    @Test
    void rerollCivic2() {
        var originalCivic1 = session.getEmpire().civics().get(0).id();
        var updated = rerollService.reroll(session, RerollCategory.CIVIC2);

        assertNotNull(updated);
        assertEquals(originalCivic1, updated.civics().get(0).id(), "Civic 1 should be unchanged");
        assertFalse(session.canReroll());
    }

    @Test
    void rerollOrigin() {
        var updated = rerollService.reroll(session, RerollCategory.ORIGIN);

        assertNotNull(updated);
        assertEquals(session.getEmpire().authority().id(), updated.authority().id(), "Authority should be unchanged");
        assertFalse(session.canReroll());
    }

    @Test
    void rerollTraits() {
        var updated = rerollService.reroll(session, RerollCategory.TRAITS);

        assertNotNull(updated);
        assertTrue(updated.traitPointsUsed() <= updated.traitPointsBudget());
        assertFalse(session.canReroll());
    }

    @Test
    void cannotRerollTwice() {
        rerollService.reroll(session, RerollCategory.ORIGIN);
        // Second reroll of ANY category should fail
        assertThrows(IllegalStateException.class,
                () -> rerollService.reroll(session, RerollCategory.AUTHORITY));
    }

    @Test
    void cannotRerollDifferentCategoryAfterFirst() {
        rerollService.reroll(session, RerollCategory.CIVIC1);
        // Even a different category should fail after one reroll
        assertThrows(IllegalStateException.class,
                () -> rerollService.reroll(session, RerollCategory.TRAITS));
    }

    @Test
    void newGenerationResetsReroll() {
        rerollService.reroll(session, RerollCategory.ORIGIN);
        assertFalse(session.canReroll());

        var newEmpire = generator.generate();
        session.reset(newEmpire);

        assertTrue(session.canReroll(), "Reroll should be available after new generation");
    }

    @Test
    void rerollPreservesLockedSelections() {
        var original = session.getEmpire();

        var updated = rerollService.reroll(session, RerollCategory.CIVIC1);

        assertEquals(original.ethics(), updated.ethics(), "Ethics should be unchanged");
        assertEquals(original.authority().id(), updated.authority().id(), "Authority should be unchanged");
        assertEquals(original.civics().get(1).id(), updated.civics().get(1).id(), "Civic 2 should be unchanged");
        assertEquals(original.origin().id(), updated.origin().id(), "Origin should be unchanged");
        assertEquals(original.speciesArchetype().id(), updated.speciesArchetype().id(), "Archetype should be unchanged");
        assertEquals(original.speciesTraits(), updated.speciesTraits(), "Traits should be unchanged");
    }
}
