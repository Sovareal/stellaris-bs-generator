package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Service
public class GameFileService {

    private static final Logger log = LoggerFactory.getLogger(GameFileService.class);

    private final ParserProperties properties;

    private ClausewitzNode ethics;
    private ClausewitzNode authorities;
    private ClausewitzNode civics;
    private ClausewitzNode origins;
    private ClausewitzNode speciesArchetypes;
    private ClausewitzNode traits;

    public GameFileService(ParserProperties properties) {
        this.properties = properties;
    }

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

        long elapsed = System.currentTimeMillis() - start;
        log.info("Game file loading complete in {}ms", elapsed);
    }

    public ClausewitzNode getEthics() { return ethics; }
    public ClausewitzNode getAuthorities() { return authorities; }
    public ClausewitzNode getCivics() { return civics; }
    public ClausewitzNode getSpeciesArchetypes() { return speciesArchetypes; }
    public ClausewitzNode getTraits() { return traits; }
}
