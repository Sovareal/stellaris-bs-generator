package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Authority;
import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
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
class AuthorityExtractorTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";
    private static List<Authority> authorities;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var props = new ParserProperties(GAME_PATH, System.getProperty("java.io.tmpdir"));
        var service = new GameFileService(props, new SettingsService(props));
        service.loadAll();
        authorities = new AuthorityExtractor().extract(service.getAuthorities());
    }

    @Test
    void extractsPlayerAuthorities() {
        // Should have democratic, oligarchic, dictatorial, imperial, hive_mind, machine_intelligence, corporate
        // auth_ancient_machine_intelligence should be filtered out
        assertTrue(authorities.size() >= 7, "Should have at least 7 player authorities, got " + authorities.size());
        assertTrue(authorities.stream().noneMatch(a -> a.id().equals("auth_ancient_machine_intelligence")),
                "Should not contain auth_ancient_machine_intelligence");
    }

    @Test
    void democraticAuthority() {
        var dem = authorities.stream().filter(a -> a.id().equals("auth_democratic")).findFirst().orElseThrow();
        assertEquals("democratic", dem.electionType());
        assertFalse(dem.hasHeir());
        assertFalse(dem.isGestalt());
    }

    @Test
    void imperialAuthorityHasHeir() {
        var imp = authorities.stream().filter(a -> a.id().equals("auth_imperial")).findFirst().orElseThrow();
        assertTrue(imp.hasHeir());
    }

    @Test
    void hiveMindIsGestalt() {
        var hive = authorities.stream().filter(a -> a.id().equals("auth_hive_mind")).findFirst().orElseThrow();
        assertTrue(hive.isGestalt());
    }

    @Test
    void machineIntelligenceIsGestalt() {
        var machine = authorities.stream().filter(a -> a.id().equals("auth_machine_intelligence")).findFirst().orElseThrow();
        assertTrue(machine.isGestalt());
    }

    @Test
    void democraticHasPossibleConstraints() {
        var dem = authorities.stream().filter(a -> a.id().equals("auth_democratic")).findFirst().orElseThrow();
        assertNotNull(dem.possible(), "Democratic should have possible constraints");
        assertTrue(dem.possible().hasCategory(RequirementCategory.ETHICS),
                "Democratic should have ethics requirements");
    }

    @Test
    void randomWeightsPositive() {
        for (var auth : authorities) {
            assertTrue(auth.randomWeight() > 0, auth.id() + " should have positive random weight");
        }
    }
}
