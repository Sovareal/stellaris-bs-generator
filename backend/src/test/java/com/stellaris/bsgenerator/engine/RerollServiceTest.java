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
                new SpeciesArchetypeExtractor(), new SpeciesTraitExtractor());
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
        var original = session.getEmpire().authority().id();
        var updated = rerollService.reroll(session, RerollCategory.AUTHORITY);

        assertNotNull(updated);
        // Authority might be the same if only one is compatible, but session should track reroll
        assertFalse(session.canReroll(RerollCategory.AUTHORITY), "Authority should be marked as rerolled");
        assertEquals(2, updated.civics().size(), "Civics should still be 2");
    }

    @Test
    void rerollCivic1() {
        var originalCivic2 = session.getEmpire().civics().get(1).id();
        var updated = rerollService.reroll(session, RerollCategory.CIVIC1);

        assertNotNull(updated);
        assertEquals(originalCivic2, updated.civics().get(1).id(), "Civic 2 should be unchanged");
        assertFalse(session.canReroll(RerollCategory.CIVIC1));
    }

    @Test
    void rerollCivic2() {
        var originalCivic1 = session.getEmpire().civics().get(0).id();
        var updated = rerollService.reroll(session, RerollCategory.CIVIC2);

        assertNotNull(updated);
        assertEquals(originalCivic1, updated.civics().get(0).id(), "Civic 1 should be unchanged");
        assertFalse(session.canReroll(RerollCategory.CIVIC2));
    }

    @Test
    void rerollOrigin() {
        var original = session.getEmpire().origin().id();
        var updated = rerollService.reroll(session, RerollCategory.ORIGIN);

        assertNotNull(updated);
        assertEquals(session.getEmpire().authority().id(), updated.authority().id(), "Authority should be unchanged");
        assertFalse(session.canReroll(RerollCategory.ORIGIN));
    }

    @Test
    void rerollTraits() {
        var updated = rerollService.reroll(session, RerollCategory.TRAITS);

        assertNotNull(updated);
        assertTrue(updated.traitPointsUsed() <= updated.traitPointsBudget());
        assertFalse(session.canReroll(RerollCategory.TRAITS));
    }

    @Test
    void cannotRerollSameCategoryTwice() {
        rerollService.reroll(session, RerollCategory.ORIGIN);
        assertThrows(IllegalStateException.class,
                () -> rerollService.reroll(session, RerollCategory.ORIGIN));
    }

    @Test
    void newGenerationResetsRerolls() {
        rerollService.reroll(session, RerollCategory.ORIGIN);
        assertFalse(session.canReroll(RerollCategory.ORIGIN));

        // Generate new empire and reset session
        var newEmpire = generator.generate();
        session.reset(newEmpire);

        assertTrue(session.canReroll(RerollCategory.ORIGIN), "Rerolls should be reset after new generation");
    }

    @Test
    void rerollPreservesLockedSelections() {
        var original = session.getEmpire();

        // Reroll civic 1 — everything else should stay the same
        var updated = rerollService.reroll(session, RerollCategory.CIVIC1);

        assertEquals(original.ethics(), updated.ethics(), "Ethics should be unchanged");
        assertEquals(original.authority().id(), updated.authority().id(), "Authority should be unchanged");
        assertEquals(original.civics().get(1).id(), updated.civics().get(1).id(), "Civic 2 should be unchanged");
        assertEquals(original.origin().id(), updated.origin().id(), "Origin should be unchanged");
        assertEquals(original.speciesArchetype().id(), updated.speciesArchetype().id(), "Archetype should be unchanged");
        assertEquals(original.speciesTraits(), updated.speciesTraits(), "Traits should be unchanged");
    }
}
