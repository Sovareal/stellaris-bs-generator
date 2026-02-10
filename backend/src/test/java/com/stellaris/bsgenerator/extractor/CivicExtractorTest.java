package com.stellaris.bsgenerator.extractor;

import com.stellaris.bsgenerator.model.Civic;
import com.stellaris.bsgenerator.model.requirement.Requirement;
import com.stellaris.bsgenerator.model.requirement.RequirementCategory;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
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
class CivicExtractorTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";
    private static List<Civic> civics;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUp() throws IOException {
        var service = new GameFileService(new ParserProperties(GAME_PATH, null));
        service.loadAll();
        civics = new CivicExtractor().extract(service.getCivics());
    }

    @Test
    void extractsManyCivics() {
        assertTrue(civics.size() > 20, "Should have many civics, got " + civics.size());
    }

    @Test
    void noOriginsIncluded() {
        // Origins are in the same file but should be filtered out
        assertTrue(civics.stream().noneMatch(c -> c.id().startsWith("origin_")),
                "Should not contain any origin_ entries");
    }

    @Test
    void corveeSystemHasCorrectConstraints() {
        var corvee = civics.stream().filter(c -> c.id().equals("civic_corvee_system")).findFirst().orElseThrow();

        // potential: ethics NOT gestalt, authority NOT corporate
        assertNotNull(corvee.potential());
        assertTrue(corvee.potential().hasCategory(RequirementCategory.ETHICS));
        assertTrue(corvee.potential().hasCategory(RequirementCategory.AUTHORITY));

        // possible: ethics NOR egalitarian/fanatic_egalitarian, civics NOT civic_free_haven
        assertNotNull(corvee.possible());
        assertTrue(corvee.possible().hasCategory(RequirementCategory.ETHICS));
        assertTrue(corvee.possible().hasCategory(RequirementCategory.CIVICS));
    }

    @Test
    void imperialCultHasMultipleOrRequirements() {
        var cult = civics.stream().filter(c -> c.id().equals("civic_imperial_cult")).findFirst().orElseThrow();
        assertNotNull(cult.possible());
        // Should have authority = auth_imperial + ethics OR blocks
        assertTrue(cult.possible().hasCategory(RequirementCategory.AUTHORITY));
        assertTrue(cult.possible().hasCategory(RequirementCategory.ETHICS));
        // Ethics should have 2 OR requirements (authoritarian + spiritualist)
        var ethicsReqs = cult.possible().get(RequirementCategory.ETHICS);
        long orCount = ethicsReqs.stream().filter(r -> r instanceof Requirement.Or).count();
        assertEquals(2, orCount, "Imperial cult should have 2 OR ethics requirements");
    }

    @Test
    void pickableAtStartDefaultsTrue() {
        var corvee = civics.stream().filter(c -> c.id().equals("civic_corvee_system")).findFirst().orElseThrow();
        assertTrue(corvee.pickableAtStart());
    }
}
