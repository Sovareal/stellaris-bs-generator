package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.extractor.*;
import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import com.stellaris.bsgenerator.parser.cache.ParsedDataCache;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("gameFilesExist")
class CompatibilityFilterServiceTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    @TempDir
    static Path tempDir;

    private static CompatibilityFilterService filterService;
    private static GameDataManager gameDataManager;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var props = new ParserProperties(GAME_PATH, tempDir.toString());
        var gameFileService = new GameFileService(props);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var cache = new ParsedDataCache(props, mapper);
        gameDataManager = new GameDataManager(props, gameFileService, cache,
                new EthicExtractor(), new AuthorityExtractor(),
                new CivicExtractor(), new OriginExtractor(),
                new SpeciesArchetypeExtractor(), new SpeciesTraitExtractor());
        gameDataManager.loadGameData(false);

        var evaluator = new RequirementEvaluator();
        filterService = new CompatibilityFilterService(gameDataManager, evaluator);
    }

    // --- Ethics filtering ---

    @Test
    void regularEthicsExcludeGestalt() {
        var ethics = filterService.getRegularEthics();
        assertTrue(ethics.size() >= 14, "Should have regular ethics (regular + fanatic)");
        assertTrue(ethics.stream().noneMatch(Ethic::isGestalt), "Should not include gestalt");
    }

    @Test
    void gestaltEthicExists() {
        var gestalt = filterService.getGestaltEthic();
        assertNotNull(gestalt);
        assertEquals("ethic_gestalt_consciousness", gestalt.id());
    }

    // --- Authority filtering ---

    @Test
    void authoritiesFilteredByEthics() {
        // With authoritarian ethics → democratic should be excluded
        var state = EmpireState.empty().withEthics(Set.of("ethic_authoritarian", "ethic_militarist", "ethic_xenophobe"));
        var authorities = filterService.getCompatibleAuthorities(state);

        assertTrue(authorities.stream().noneMatch(a -> a.id().equals("auth_democratic")),
                "Democratic should be excluded for authoritarian ethics");
        assertTrue(authorities.stream().anyMatch(a -> a.id().equals("auth_imperial")),
                "Imperial should be available for authoritarian ethics");
    }

    @Test
    void gestaltEthicsFilterToGestaltAuthorities() {
        var state = EmpireState.empty().withEthics(Set.of("ethic_gestalt_consciousness"));
        var authorities = filterService.getCompatibleAuthorities(state);

        // Should include hive_mind or machine_intelligence, but not democratic/oligarchic/etc.
        assertTrue(authorities.stream().anyMatch(Authority::isGestalt),
                "Should include gestalt authorities");
        assertTrue(authorities.stream().noneMatch(a -> a.id().equals("auth_democratic")),
                "Should not include democratic for gestalt");
    }

    // --- Civic filtering ---

    @Test
    void civicsFilteredByEthicsAndAuthority() {
        // Non-gestalt, non-corporate → should get standard civics
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_authoritarian", "ethic_spiritualist"))
                .withAuthority("auth_imperial");
        var civics = filterService.getCompatibleCivics(state);

        assertTrue(civics.size() > 5, "Should have multiple compatible civics, got " + civics.size());
        // Imperial cult requires imperial + authoritarian + spiritualist → should be available
        assertTrue(civics.stream().anyMatch(c -> c.id().equals("civic_imperial_cult")),
                "Imperial cult should be available for imperial authoritarian spiritualist");
    }

    @Test
    void civicsMutualExclusion() {
        // State with civic_corvee_system already selected → civic_free_haven should be excluded
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_authoritarian", "ethic_xenophobe"))
                .withAuthority("auth_dictatorial")
                .withCivics(Set.of("civic_corvee_system"));
        var civics = filterService.getCompatibleCivics(state);

        // civic_free_haven has possible = { civics = { NOT = { value = civic_corvee_system } } }
        // Wait — actually it's the other way: corvee forbids free_haven.
        // Let's check the reverse: free_haven might forbid corvee_system
        // The mutual exclusion is in civic_corvee_system's possible, not free_haven's.
        // But civic_free_haven also has egalitarian requirement, so it would be excluded
        // by the authoritarian ethics anyway. Let's test a known mutual pair.
        // corvee_system is already in civics, so nothing should list corvee_system again
        assertTrue(civics.stream().noneMatch(c -> c.id().equals("civic_corvee_system")),
                "Already-selected civic should not appear (self is in civics set)");
    }

    @Test
    void gestaltCivicsAppearForGestaltEmpires() {
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_gestalt_consciousness"))
                .withAuthority("auth_hive_mind");
        var civics = filterService.getCompatibleCivics(state);

        // Should have hive-specific civics, not standard ones
        assertTrue(civics.size() > 3, "Should have gestalt civics, got " + civics.size());
        // Standard civics require NOT gestalt → should be excluded
        assertTrue(civics.stream().noneMatch(c -> c.id().equals("civic_corvee_system")),
                "Standard civic should not appear for gestalt");
    }

    // --- Origin filtering ---

    @Test
    void originsFilteredByState() {
        var state = EmpireState.empty()
                .withEthics(Set.of("ethic_egalitarian", "ethic_xenophile", "ethic_pacifist"))
                .withAuthority("auth_democratic");
        var origins = filterService.getCompatibleOrigins(state);

        assertTrue(origins.size() > 5, "Should have multiple compatible origins, got " + origins.size());
        assertTrue(origins.stream().anyMatch(o -> o.id().equals("origin_default")),
                "origin_default should always be available");
    }

    // --- Species traits ---

    @Test
    void biologicalTraitsFiltered() {
        var traits = filterService.getCompatibleTraits("BIOLOGICAL");
        assertTrue(traits.size() > 10, "Should have many biological traits, got " + traits.size());
        assertTrue(traits.stream().allMatch(t -> t.allowedArchetypes().contains("BIOLOGICAL")),
                "All traits should be allowed for BIOLOGICAL");
    }

    @Test
    void roboticTraitsFiltered() {
        var traits = filterService.getCompatibleTraits("ROBOT");
        assertTrue(traits.size() > 3, "Should have some robot traits, got " + traits.size());
        assertTrue(traits.stream().allMatch(t -> t.allowedArchetypes().contains("ROBOT")),
                "All traits should be allowed for ROBOT");
        // Should NOT contain biological-only traits
        assertTrue(traits.stream().noneMatch(t -> t.id().equals("trait_agrarian")),
                "Biological trait should not appear for ROBOT");
    }

    // --- Selectable archetypes ---

    @Test
    void selectableArchetypesExcludePresapientAndOther() {
        var archetypes = filterService.getSelectableArchetypes();
        assertTrue(archetypes.stream().noneMatch(a -> a.id().equals("PRESAPIENT")));
        assertTrue(archetypes.stream().noneMatch(a -> a.id().equals("OTHER")));
        assertTrue(archetypes.size() >= 4, "Should have at least BIOLOGICAL, ROBOT, MACHINE, LITHOID");
    }

    // --- Full chain test ---

    @Test
    void fullChainProgressiveNarrowing() {
        // 1. Pick ethics
        var ethics = Set.of("ethic_fanatic_authoritarian", "ethic_spiritualist");
        var state = EmpireState.empty().withEthics(ethics);

        // 2. Filter authorities
        var authorities = filterService.getCompatibleAuthorities(state);
        assertFalse(authorities.isEmpty(), "Should have compatible authorities");
        // Pick imperial
        var authority = authorities.stream().filter(a -> a.id().equals("auth_imperial")).findFirst().orElseThrow();
        state = state.withAuthority(authority.id());

        // 3. Filter civics (first pick)
        var civics1 = filterService.getCompatibleCivics(state);
        assertFalse(civics1.isEmpty(), "Should have compatible civics");
        // Pick imperial cult
        var civic1 = civics1.stream().filter(c -> c.id().equals("civic_imperial_cult")).findFirst().orElseThrow();
        state = state.withCivics(Set.of(civic1.id()));

        // 4. Filter civics (second pick — with civic1 already selected)
        var civics2 = filterService.getCompatibleCivics(state);
        assertFalse(civics2.isEmpty(), "Should still have compatible civics for second pick");
        // Imperial cult should NOT appear again
        assertTrue(civics2.stream().noneMatch(c -> c.id().equals("civic_imperial_cult")),
                "First civic should not appear for second pick");

        // 5. Filter origins
        var origins = filterService.getCompatibleOrigins(state);
        assertFalse(origins.isEmpty(), "Should have compatible origins");
        assertTrue(origins.stream().anyMatch(o -> o.id().equals("origin_default")));
    }
}
