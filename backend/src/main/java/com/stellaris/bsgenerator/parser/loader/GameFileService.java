package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameFileService {

    private final ParserProperties properties;

    @Getter private ClausewitzNode ethics;
    @Getter private ClausewitzNode authorities;
    @Getter private ClausewitzNode civics;
    @Getter private ClausewitzNode origins;
    @Getter private ClausewitzNode speciesArchetypes;
    @Getter private ClausewitzNode traits;
    @Getter private ClausewitzNode planetClasses;
    @Getter private ClausewitzNode graphicalCultures;

    public void loadAll() throws IOException {
        Path gamePath = Path.of(properties.gamePath());
        Path common = gamePath.resolve("common");

        log.info("Loading game files from {}", gamePath);
        long start = System.currentTimeMillis();

        // Load global scripted variables first
        Map<String, String> globalVars = ScriptedVariableLoader.loadFromDirectory(
                common.resolve("scripted_variables"));
        log.info("Loaded {} global scripted variables", globalVars.size());

        // Parse each category
        ethics = DirectoryLoader.loadDirectory(common.resolve("ethics"), globalVars);
        log.info("Loaded {} ethics", ethics.children().size());

        authorities = DirectoryLoader.loadDirectory(
                common.resolve("governments").resolve("authorities"), globalVars);
        log.info("Loaded {} authorities", authorities.children().size());

        civics = DirectoryLoader.loadDirectory(
                common.resolve("governments").resolve("civics"), globalVars);
        log.info("Loaded {} civics/origins entries", civics.children().size());

        speciesArchetypes = DirectoryLoader.loadDirectory(
                common.resolve("species_archetypes"), globalVars);
        log.info("Loaded {} species archetypes", speciesArchetypes.children().size());

        traits = DirectoryLoader.loadDirectory(common.resolve("traits"), globalVars);
        log.info("Loaded {} traits", traits.children().size());

        planetClasses = DirectoryLoader.loadDirectory(common.resolve("planet_classes"), globalVars);
        log.info("Loaded {} planet class entries", planetClasses.children().size());

        graphicalCultures = DirectoryLoader.loadDirectory(common.resolve("graphical_culture"), globalVars);
        log.info("Loaded {} graphical culture entries", graphicalCultures.children().size());

        long elapsed = System.currentTimeMillis() - start;
        log.info("Game file loading complete in {}ms", elapsed);
    }

}
