package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.extractor.*;
import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.parser.LocalizationService;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import com.stellaris.bsgenerator.parser.cache.ParsedDataCache;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("gameFilesExist")
class EmpireGeneratorServiceTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    @TempDir
    static Path tempDir;

    private static EmpireGeneratorService generator;
    private static RequirementEvaluator evaluator;
    private static GameDataManager gameDataManager;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var props = new ParserProperties(GAME_PATH, tempDir.toString());
        var settingsService = new SettingsService(props);
        var gameFileService = new GameFileService(props, settingsService);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var cache = new ParsedDataCache(props, mapper);
        gameDataManager = new GameDataManager(settingsService, gameFileService, cache,
                new EthicExtractor(), new AuthorityExtractor(),
                new CivicExtractor(), new OriginExtractor(),
                new SpeciesArchetypeExtractor(), new SpeciesTraitExtractor(),
                new PlanetClassExtractor(), new GraphicalCultureExtractor(),
                new StartingRulerTraitExtractor(), new SpeciesClassExtractor(),
                new LocalizationService(props, settingsService));
        gameDataManager.loadGameData(false);

        evaluator = new RequirementEvaluator();
        var filterService = new CompatibilityFilterService(gameDataManager, evaluator);
        generator = new EmpireGeneratorService(filterService, evaluator);
    }

    @Test
    void generateProducesCompleteEmpire() {
        var empire = generator.generate();

        assertNotNull(empire);
        assertFalse(empire.ethics().isEmpty(), "Should have ethics");
        assertNotNull(empire.authority(), "Should have authority");
        assertEquals(2, empire.civics().size(), "Should have 2 civics");
        assertNotNull(empire.origin(), "Should have origin");
        assertNotNull(empire.speciesArchetype(), "Should have archetype");
        assertNotNull(empire.speciesTraits(), "Should have traits list");
    }

    @Test
    void ethicsCostEqualsThree() {
        var empire = generator.generate();
        int totalCost = empire.ethics().stream().mapToInt(Ethic::cost).sum();
        assertEquals(3, totalCost, "Ethics cost must equal 3");
    }

    @Test
    void civicsAreDifferent() {
        var empire = generator.generate();
        assertEquals(2, empire.civics().size());
        assertNotEquals(empire.civics().get(0).id(), empire.civics().get(1).id(),
                "Two civics must be different");
    }

    @Test
    void traitPointsWithinBudget() {
        var empire = generator.generate();
        assertTrue(empire.traitPointsUsed() <= empire.traitPointsBudget(),
                "Trait points used (" + empire.traitPointsUsed() + ") should not exceed budget (" + empire.traitPointsBudget() + ")");
    }

    @Test
    void traitsAreCompatibleWithArchetype() {
        var empire = generator.generate();
        for (var trait : empire.speciesTraits()) {
            // Enforced stubs (initial=no traits not in creation pool) have empty allowedArchetypes — skip them
            if (trait.allowedArchetypes().isEmpty()) continue;
            assertTrue(trait.allowedArchetypes().contains(empire.speciesArchetype().id()),
                    "Trait " + trait.id() + " should be allowed for archetype " + empire.speciesArchetype().id());
        }
    }

    @Test
    void traitsHaveNoOppositeConflicts() {
        var empire = generator.generate();
        Set<String> traitIds = new HashSet<>();
        for (var trait : empire.speciesTraits()) {
            traitIds.add(trait.id());
        }
        for (var trait : empire.speciesTraits()) {
            for (var opposite : trait.opposites()) {
                assertFalse(traitIds.contains(opposite),
                        "Trait " + trait.id() + " conflicts with opposite " + opposite);
            }
        }
    }

    @Test
    void authorityCompatibleWithEthics() {
        var empire = generator.generate();
        var state = EmpireState.empty()
                .withEthics(toIdSet(empire.ethics()));
        assertTrue(evaluator.evaluateBoth(empire.authority().potential(), empire.authority().possible(), state),
                "Authority should be compatible with selected ethics");
    }

    @Test
    void civicsCompatibleWithState() {
        var empire = generator.generate();
        var state = EmpireState.empty()
                .withEthics(toIdSet(empire.ethics()))
                .withAuthority(empire.authority().id());

        for (var civic : empire.civics()) {
            assertTrue(evaluator.evaluateBoth(civic.potential(), civic.possible(), state),
                    "Civic " + civic.id() + " should be compatible with state");
            // Add civic to state for next check
            var newCivics = new HashSet<>(state.civics());
            newCivics.add(civic.id());
            state = state.withCivics(newCivics);
        }
    }

    @RepeatedTest(100)
    void generate100ValidEmpires() {
        var empire = generator.generate();
        assertNotNull(empire);

        // Validate ethics cost
        int totalCost = empire.ethics().stream().mapToInt(Ethic::cost).sum();
        assertEquals(3, totalCost, "Ethics cost must equal 3");

        // Validate 2 different civics
        assertEquals(2, empire.civics().size());

        // Validate trait budget
        assertTrue(empire.traitPointsUsed() <= empire.traitPointsBudget());
    }

    @RepeatedTest(200)
    void generate200IncludingGestalt() {
        var empire = generator.generate();
        assertNotNull(empire);

        int totalCost = empire.ethics().stream().mapToInt(Ethic::cost).sum();
        assertEquals(3, totalCost, "Ethics cost must equal 3");

        boolean isGestalt = empire.ethics().stream().anyMatch(Ethic::isGestalt);
        if (isGestalt) {
            // Gestalt: single ethic, gestalt authority, compatible archetype
            assertEquals(1, empire.ethics().size(), "Gestalt should have exactly 1 ethic");
            assertTrue(empire.authority().isGestalt(),
                    "Gestalt empire must have gestalt authority, got: " + empire.authority().id());

            if ("auth_machine_intelligence".equals(empire.authority().id())) {
                assertTrue(empire.speciesArchetype().robotic(),
                        "Machine intelligence must have robotic archetype, got: " + empire.speciesArchetype().id());
            } else {
                assertFalse(empire.speciesArchetype().robotic(),
                        "Hive mind must have non-robotic archetype, got: " + empire.speciesArchetype().id());
            }
        }

        assertEquals(2, empire.civics().size());
        assertTrue(empire.traitPointsUsed() <= empire.traitPointsBudget());
    }

    @RepeatedTest(200)
    void secondarySpeciesValidWhenPresent() {
        var empire = generator.generate();
        var secondary = empire.secondarySpecies();

        // Check if this empire should have a secondary species
        boolean originRequiresSecondary = empire.origin().secondarySpecies() != null;
        boolean civicRequiresSecondary = empire.civics().stream()
                .anyMatch(c -> c.secondarySpecies() != null);

        if (originRequiresSecondary || civicRequiresSecondary) {
            assertNotNull(secondary, "Empire with origin " + empire.origin().id()
                    + " and civics " + empire.civics().stream().map(c -> c.id()).toList()
                    + " should have a secondary species");

            // Secondary species class should differ from primary
            assertNotEquals(empire.speciesClass(), secondary.speciesClass(),
                    "Secondary species class should differ from primary");

            // Trait budget should be respected
            assertTrue(secondary.traitPointsUsed() <= secondary.traitPointsBudget(),
                    "Secondary species trait points used (" + secondary.traitPointsUsed()
                    + ") should not exceed budget (" + secondary.traitPointsBudget() + ")");

            // Total picks should not exceed max
            int totalPicks = secondary.enforcedTraits().size() + secondary.additionalTraits().size();
            assertTrue(totalPicks <= secondary.maxTraitPicks(),
                    "Total secondary species picks (" + totalPicks
                    + ") should not exceed max (" + secondary.maxTraitPicks() + ")");
        } else {
            assertNull(secondary, "Empire without secondary species origin/civic should have null secondary species");
        }
    }

    @RepeatedTest(200)
    void underOneRuleTraitsAndEthicsValid() {
        var empire = generator.generate();

        boolean isUOR = "origin_legendary_leader".equals(empire.origin().id());
        if (!isUOR) return;

        // Issue 2: UOR leaders must only have Luminary traits (allowedOrigins = [origin_legendary_leader])
        for (var trait : empire.leaderTraits()) {
            assertTrue(trait.allowedOrigins().contains("origin_legendary_leader"),
                    "UOR leader trait " + trait.id() + " must be Luminary");
        }

        // Issue 6: UOR requires auth_dictatorial, which forbids Gestalt Consciousness
        assertFalse(empire.ethics().stream().anyMatch(Ethic::isGestalt),
                "UOR empire must not have Gestalt Consciousness ethics");
    }

    private Set<String> toIdSet(java.util.List<Ethic> ethics) {
        var set = new HashSet<String>();
        for (var e : ethics) set.add(e.id());
        return set;
    }
}
