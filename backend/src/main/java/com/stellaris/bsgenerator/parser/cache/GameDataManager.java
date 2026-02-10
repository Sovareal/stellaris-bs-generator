package com.stellaris.bsgenerator.parser.cache;

import com.stellaris.bsgenerator.extractor.*;
import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameDataManager {

    private static final List<String> PARSED_SUBDIRECTORIES = List.of(
            "ethics",
            "governments/authorities",
            "governments/civics",
            "species_archetypes",
            "traits"
    );

    private final ParserProperties properties;
    private final GameFileService gameFileService;
    private final ParsedDataCache cache;
    private final EthicExtractor ethicExtractor;
    private final AuthorityExtractor authorityExtractor;
    private final CivicExtractor civicExtractor;
    private final OriginExtractor originExtractor;
    private final SpeciesArchetypeExtractor speciesArchetypeExtractor;
    private final SpeciesTraitExtractor speciesTraitExtractor;

    @Getter private GameVersion gameVersion;
    @Getter private List<Ethic> ethics;
    @Getter private List<Authority> authorities;
    @Getter private List<Civic> civics;
    @Getter private List<Origin> origins;
    @Getter private List<SpeciesArchetype> speciesArchetypes;
    @Getter private List<SpeciesTrait> speciesTraits;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            loadGameData(false);
        } catch (IOException e) {
            log.error("Failed to load game data on startup: {}", e.getMessage(), e);
        }
    }

    public void loadGameData(boolean forceReload) throws IOException {
        Path gamePath = Path.of(properties.gamePath());

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
        extractTypedData();

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
        loadGameData(true);
    }

    private void restoreFromCache(ParsedDataCache.CacheEntry cached) {
        // For now, cached data isn't directly restored to GameFileService
        // since ClausewitzNode serialization/deserialization would need custom handling.
        // Instead, we re-parse (fast enough at < 5s) and only use fingerprint for staleness check.
        // TODO: Implement full cache restore when parse time becomes a concern.
        try {
            gameFileService.loadAll();
            extractTypedData();
        } catch (IOException e) {
            log.error("Failed to re-parse game files: {}", e.getMessage(), e);
        }
    }

    private void extractTypedData() {
        ethics = ethicExtractor.extract(gameFileService.getEthics());
        authorities = authorityExtractor.extract(gameFileService.getAuthorities());
        civics = civicExtractor.extract(gameFileService.getCivics());
        origins = originExtractor.extract(gameFileService.getCivics());
        speciesArchetypes = speciesArchetypeExtractor.extract(gameFileService.getSpeciesArchetypes());
        speciesTraits = speciesTraitExtractor.extract(gameFileService.getTraits());

        log.info("Extracted: {} ethics, {} authorities, {} civics, {} origins, {} archetypes, {} traits",
                ethics.size(), authorities.size(), civics.size(), origins.size(),
                speciesArchetypes.size(), speciesTraits.size());
    }
}
