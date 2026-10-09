package com.stellaris.bsgenerator.parser.cache;

import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.extractor.*;
import com.stellaris.bsgenerator.icon.IconService;
import com.stellaris.bsgenerator.icon.LeaderTraitGfxParser;
import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.parser.LocalizationService;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameDataManager {

    public enum DataStatus { LOADING, READY, ERROR }

    @Getter private volatile DataStatus dataStatus = DataStatus.LOADING;
    @Getter private volatile String dataError = null;

    private static final List<String> PARSED_SUBDIRECTORIES = List.of(
            "ethics",
            "governments/authorities",
            "governments/civics",
            "species_archetypes",
            "traits",
            "planet_classes",
            "graphical_culture",
            "species_classes"
    );

    private final SettingsService settingsService;
    private final GameFileService gameFileService;
    private final ParsedDataCache cache;
    private final EthicExtractor ethicExtractor;
    private final AuthorityExtractor authorityExtractor;
    private final CivicExtractor civicExtractor;
    private final OriginExtractor originExtractor;
    private final SpeciesArchetypeExtractor speciesArchetypeExtractor;
    private final SpeciesTraitExtractor speciesTraitExtractor;
    private final PlanetClassExtractor planetClassExtractor;
    private final GraphicalCultureExtractor graphicalCultureExtractor;
    private final StartingRulerTraitExtractor startingRulerTraitExtractor;
    private final SpeciesClassExtractor speciesClassExtractor;
    private final LocalizationService localizationService;

    private IconService iconService;

    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    public void setIconService(IconService iconService) {
        this.iconService = iconService;
    }

    @Getter private GameVersion gameVersion;
    @Getter private List<Ethic> ethics;
    @Getter private List<Authority> authorities;
    @Getter private List<Civic> civics;
    @Getter private List<Origin> origins;
    @Getter private List<SpeciesArchetype> speciesArchetypes;
    @Getter private List<SpeciesTrait> speciesTraits;
    @Getter private List<PlanetClass> planetClasses;
    @Getter private List<GraphicalCulture> graphicalCultures;
    @Getter private List<StartingRulerTrait> startingRulerTraits;
    @Getter private List<SpeciesClass> speciesClasses;
    @Getter private Map<String, String> leaderTraitGfxMap;
    @Getter private Map<String, String> allTraitIconPaths;
    @Getter private Map<String, Integer> allTraitCosts;
    @Getter private Map<String, List<String>> allTraitOpposites;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            String effectivePath = settingsService.getEffectiveGamePath();
            Path gamePath = Path.of(effectivePath);
            if (!Files.isDirectory(gamePath)) {
                dataStatus = DataStatus.ERROR;
                dataError = "Game path does not exist or is not a directory: " + gamePath;
                log.error(dataError);
                return;
            }
            loadGameData(false);
            dataStatus = DataStatus.READY;
            dataError = null;
            if (iconService != null) {
                iconService.warmCacheAsync();
            }
        } catch (Exception e) {
            dataStatus = DataStatus.ERROR;
            dataError = e.getMessage();
            log.error("Failed to load game data on startup: {}", e.getMessage(), e);
        }
    }

    public void loadGameData(boolean forceReload) throws IOException {
        Path gamePath = Path.of(settingsService.getEffectiveGamePath());

        // Detect version
        gameVersion = GameVersion.fromLauncherSettings(gamePath);
        log.info("{} detected", gameVersion);

        // Compute fingerprint
        String fileHash = FileFingerprint.compute(gamePath, PARSED_SUBDIRECTORIES);
        String fingerprint = gameVersion.rawVersion() + ":" + fileHash;

        if (!forceReload) {
            // Try loading from cache
            ParsedDataCache.CacheEntry cached = cache.load();
            if (cached != null && fingerprint.equals(cached.fingerprint())) {
                log.info("Cache valid — loading from cache");
                restoreFromCache(cached);
                return;
            }
            log.info("Cache stale or missing — re-parsing game files");
        } else {
            log.info("Force reload requested — re-parsing game files");
            cache.clear();
        }

        // Parse fresh
        gameFileService.loadAll();
        verifyNoParseFailures();
        extractTypedData();
        verifyExtractedData();
        localizationService.load();

        // Save to cache
        Map<String, com.stellaris.bsgenerator.parser.ast.ClausewitzNode> data = Map.of(
                "ethics", gameFileService.getEthics(),
                "authorities", gameFileService.getAuthorities(),
                "civics", gameFileService.getCivics(),
                "speciesArchetypes", gameFileService.getSpeciesArchetypes(),
                "traits", gameFileService.getTraits()
        );
        cache.save(fingerprint, data);
    }

    public void forceReload() throws IOException {
        dataStatus = DataStatus.LOADING;
        dataError = null;
        if (iconService != null) {
            iconService.clearCache();
        }
        try {
            loadGameData(true);
            dataStatus = DataStatus.READY;
            dataError = null;
        } catch (Exception e) {
            dataStatus = DataStatus.ERROR;
            dataError = e.getMessage();
            throw e;
        }
    }

    private void restoreFromCache(ParsedDataCache.CacheEntry cached) {
        // For now, cached data isn't directly restored to GameFileService
        // since ClausewitzNode serialization/deserialization would need custom handling.
        // Instead, we re-parse (fast enough at < 5s) and only use fingerprint for staleness check.
        try {
            gameFileService.loadAll();
        } catch (IOException e) {
            throw new GameDataLoadException("Failed to read game files: " + e.getMessage());
        }
        verifyNoParseFailures();
        extractTypedData();
        verifyExtractedData();
        localizationService.load();
    }

    /**
     * Skipping an unparseable file silently produces empty or partial data (phases 81 and 82), so any
     * parse failure is fatal and reported to the user with the file names.
     */
    private void verifyNoParseFailures() {
        var failures = gameFileService.getParseFailures();
        if (failures.isEmpty()) return;
        var sb = new StringBuilder("Could not parse ").append(failures.size()).append(" game file(s)");
        if (gameVersion != null) sb.append(" (").append(gameVersion).append(")");
        sb.append(". This game version is probably not supported yet. Please report this message:");
        failures.forEach(f -> sb.append("\n- ").append(f));
        throw new GameDataLoadException(sb.toString());
    }

    /** Catches files that parse but yield nothing (e.g. a changed block structure). */
    private void verifyExtractedData() {
        var problems = new ArrayList<String>();
        checkMinimum(problems, "ethics", ethics, 10);
        checkMinimum(problems, "authorities", authorities, 4);
        checkMinimum(problems, "civics", civics, 20);
        checkMinimum(problems, "origins", origins, 5);
        checkMinimum(problems, "species archetypes", speciesArchetypes, 3);
        checkMinimum(problems, "species traits", speciesTraits, 20);
        checkMinimum(problems, "planet classes", planetClasses, 1);
        checkMinimum(problems, "graphical cultures", graphicalCultures, 1);
        checkMinimum(problems, "species classes", speciesClasses, 1);
        if (problems.isEmpty()) return;
        throw new GameDataLoadException("Game data looks incomplete"
                + (gameVersion != null ? " (" + gameVersion + ")" : "")
                + ". This game version is probably not supported yet. Please report this message:\n- "
                + String.join("\n- ", problems));
    }

    private static void checkMinimum(List<String> problems, String name, List<?> items, int min) {
        int count = items == null ? 0 : items.size();
        if (count < min) {
            problems.add("only " + count + " " + name + " extracted (expected at least " + min + ")");
        }
    }

    private void extractTypedData() {
        ethics = ethicExtractor.extract(gameFileService.getEthics());
        authorities = authorityExtractor.extract(gameFileService.getAuthorities());
        civics = civicExtractor.extract(gameFileService.getCivics());
        origins = originExtractor.extract(gameFileService.getCivics());
        speciesArchetypes = speciesArchetypeExtractor.extract(gameFileService.getSpeciesArchetypes());
        speciesTraits = speciesTraitExtractor.extract(gameFileService.getTraits());
        allTraitIconPaths = speciesTraitExtractor.extractIconPaths(gameFileService.getTraits());
        allTraitCosts = speciesTraitExtractor.extractCosts(gameFileService.getTraits());
        allTraitOpposites = speciesTraitExtractor.extractAllTraitOpposites(gameFileService.getTraits());
        planetClasses = planetClassExtractor.extract(gameFileService.getPlanetClasses());
        graphicalCultures = graphicalCultureExtractor.extract(gameFileService.getGraphicalCultures());
        startingRulerTraits = startingRulerTraitExtractor.extract(gameFileService.getTraits());
        speciesClasses = speciesClassExtractor.extract(gameFileService.getSpeciesClasses());

        // Parse leader trait GFX mappings
        Path gamePath = Path.of(settingsService.getEffectiveGamePath());
        Path gfxFile = gamePath.resolve("interface/icons/traits/leader_traits.gfx");
        try {
            leaderTraitGfxMap = LeaderTraitGfxParser.parse(gfxFile);
        } catch (IOException e) {
            log.warn("Failed to parse leader_traits.gfx: {}", e.getMessage());
            leaderTraitGfxMap = Map.of();
        }

        log.info("Extracted: {} ethics, {} authorities, {} civics, {} origins, {} archetypes, {} traits, {} planets, {} shipsets, {} ruler traits, {} species classes, {} leader GFX",
                ethics.size(), authorities.size(), civics.size(), origins.size(),
                speciesArchetypes.size(), speciesTraits.size(),
                planetClasses.size(), graphicalCultures.size(), startingRulerTraits.size(),
                speciesClasses.size(), leaderTraitGfxMap.size());
    }
}
