package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameFileService {

    private final ParserProperties properties;
    private final SettingsService settingsService;

    @Getter private ClausewitzNode ethics;
    @Getter private ClausewitzNode authorities;
    @Getter private ClausewitzNode civics;
    @Getter private ClausewitzNode origins;
    @Getter private ClausewitzNode speciesArchetypes;
    @Getter private ClausewitzNode traits;
    @Getter private ClausewitzNode planetClasses;
    @Getter private ClausewitzNode graphicalCultures;
    @Getter private ClausewitzNode speciesClasses;

    /** Files skipped by the most recent {@link #loadAll()}. Non-empty means the game data is incomplete. */
    @Getter private List<ParseFailure> parseFailures = List.of();

    private ClausewitzNode load(Path dir, Map<String, String> globalVars, List<ParseFailure> failures) throws IOException {
        var result = DirectoryLoader.loadDirectoryReporting(dir, globalVars);
        failures.addAll(result.failures());
        return result.root();
    }

    public void loadAll() throws IOException {
        Path gamePath = Path.of(settingsService.getEffectiveGamePath());
        Path common = gamePath.resolve("common");

        log.info("Loading game files from {}", gamePath);
        long start = System.currentTimeMillis();

        // Load global scripted variables first
        Map<String, String> globalVars = ScriptedVariableLoader.loadFromDirectory(
                common.resolve("scripted_variables"));
        log.info("Loaded {} global scripted variables", globalVars.size());

        // Parse each category
        var failures = new ArrayList<ParseFailure>();
        ethics = load(common.resolve("ethics"), globalVars, failures);
        log.info("Loaded {} ethics", ethics.children().size());

        authorities = load(common.resolve("governments").resolve("authorities"), globalVars, failures);
        log.info("Loaded {} authorities", authorities.children().size());

        civics = load(common.resolve("governments").resolve("civics"), globalVars, failures);
        log.info("Loaded {} civics/origins entries", civics.children().size());

        speciesArchetypes = load(common.resolve("species_archetypes"), globalVars, failures);
        log.info("Loaded {} species archetypes", speciesArchetypes.children().size());

        traits = load(common.resolve("traits"), globalVars, failures);
        log.info("Loaded {} traits", traits.children().size());

        planetClasses = load(common.resolve("planet_classes"), globalVars, failures);
        log.info("Loaded {} planet class entries", planetClasses.children().size());

        graphicalCultures = load(common.resolve("graphical_culture"), globalVars, failures);
        log.info("Loaded {} graphical culture entries", graphicalCultures.children().size());

        speciesClasses = load(common.resolve("species_classes"), globalVars, failures);
        log.info("Loaded {} species class entries", speciesClasses.children().size());

        parseFailures = List.copyOf(failures);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Game file loading complete in {}ms", elapsed);
    }

}
