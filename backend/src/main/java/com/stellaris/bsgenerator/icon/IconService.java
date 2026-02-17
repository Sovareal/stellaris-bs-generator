package com.stellaris.bsgenerator.icon;

import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.model.Origin;
import com.stellaris.bsgenerator.model.StartingRulerTrait;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves icon paths, converts DDS→PNG, and caches results.
 * Two-tier cache: in-memory ConcurrentHashMap + disk at {cachePath}/icons/.
 */
@Slf4j
@Service
public class IconService {

    private final SettingsService settingsService;
    private final GameDataManager gameDataManager;
    private final Path iconCacheDir;
    private final ConcurrentHashMap<String, byte[]> memoryCache = new ConcurrentHashMap<>();

    public IconService(SettingsService settingsService, GameDataManager gameDataManager, ParserProperties properties) {
        this.settingsService = settingsService;
        this.gameDataManager = gameDataManager;
        this.iconCacheDir = Path.of(properties.cachePath()).resolve("icons");
    }

    /**
     * Get PNG bytes for an icon by category and id.
     * Returns null if the DDS source doesn't exist.
     */
    public byte[] getIcon(String category, String id) {
        String cacheKey = category + "/" + id;

        // Check memory cache
        byte[] cached = memoryCache.get(cacheKey);
        if (cached != null) return cached;

        // Check disk cache
        Path diskCacheFile = iconCacheDir.resolve(category).resolve(id + ".png");
        if (Files.exists(diskCacheFile)) {
            try {
                cached = Files.readAllBytes(diskCacheFile);
                memoryCache.put(cacheKey, cached);
                return cached;
            } catch (IOException e) {
                log.warn("Failed to read cached icon {}: {}", diskCacheFile, e.getMessage());
            }
        }

        // Resolve DDS path and convert
        Path ddsPath = resolveDdsPath(category, id);
        if (ddsPath == null || !Files.exists(ddsPath)) {
            return null;
        }

        try {
            byte[] pngBytes = convertDdsToPng(ddsPath);
            if (pngBytes == null) return null;

            // Write to disk cache
            Files.createDirectories(diskCacheFile.getParent());
            Files.write(diskCacheFile, pngBytes);

            // Write to memory cache
            memoryCache.put(cacheKey, pngBytes);

            return pngBytes;
        } catch (IOException e) {
            log.warn("Failed to convert DDS→PNG for {}/{}: {}", category, id, e.getMessage());
            return null;
        }
    }

    /**
     * Clear both in-memory and disk caches.
     */
    public void clearCache() {
        memoryCache.clear();
        try {
            if (Files.exists(iconCacheDir)) {
                try (var walk = Files.walk(iconCacheDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                            });
                }
            }
        } catch (IOException e) {
            log.warn("Failed to clear icon cache directory: {}", e.getMessage());
        }
        log.info("Icon cache cleared");
    }

    private Path resolveDdsPath(String category, String id) {
        Path gamePath = Path.of(settingsService.getEffectiveGamePath());

        return switch (category) {
            case "ethics" -> gamePath.resolve("gfx/interface/icons/ethics/" + id + ".dds");
            case "authorities" -> gamePath.resolve("gfx/interface/icons/governments/authorities/" + id + ".dds");
            case "civics" -> gamePath.resolve("gfx/interface/icons/governments/civics/" + id + ".dds");
            case "origins" -> resolveOriginIcon(gamePath, id);
            case "traits" -> resolveTraitIcon(gamePath, id);
            case "leadertraits" -> resolveLeaderTraitIcon(gamePath, id);
            case "planets" -> gamePath.resolve("gfx/interface/icons/planet_backgrounds/" + id + ".dds");
            default -> {
                log.debug("Unknown icon category: {}", category);
                yield null;
            }
        };
    }

    private Path resolveTraitIcon(Path gamePath, String traitId) {
        // Check if the trait has a custom icon path
        if (gameDataManager.getSpeciesTraits() != null) {
            for (var trait : gameDataManager.getSpeciesTraits()) {
                if (trait.id().equals(traitId) && trait.iconPath() != null) {
                    return gamePath.resolve(trait.iconPath());
                }
            }
        }
        // Fallback: direct ID match
        return gamePath.resolve("gfx/interface/icons/traits/" + traitId + ".dds");
    }

    private Path resolveOriginIcon(Path gamePath, String originId) {
        // Origins have explicit icon paths that don't match their IDs
        if (gameDataManager.getOrigins() != null) {
            for (Origin origin : gameDataManager.getOrigins()) {
                if (origin.id().equals(originId) && origin.iconPath() != null) {
                    return gamePath.resolve(origin.iconPath());
                }
            }
        }
        // Fallback: try direct ID match
        return gamePath.resolve("gfx/interface/icons/origins/" + originId + ".dds");
    }

    private Path resolveLeaderTraitIcon(Path gamePath, String traitId) {
        // First try: look up GFX key from the trait data, then resolve via GFX map
        Map<String, String> gfxMap = gameDataManager.getLeaderTraitGfxMap();
        if (gfxMap != null) {
            // Try matching the trait ID to a GFX key from the StartingRulerTrait data
            if (gameDataManager.getStartingRulerTraits() != null) {
                for (StartingRulerTrait trait : gameDataManager.getStartingRulerTraits()) {
                    if (trait.id().equals(traitId) && trait.gfxKey() != null) {
                        String ddsPath = gfxMap.get(trait.gfxKey());
                        if (ddsPath != null) {
                            return gamePath.resolve(ddsPath);
                        }
                    }
                }
            }
            // Also try using the ID directly as a GFX key prefix
            String gfxKey = "GFX_" + traitId;
            String ddsPath = gfxMap.get(gfxKey);
            if (ddsPath != null) {
                return gamePath.resolve(ddsPath);
            }
        }
        // Fallback: try direct path
        return gamePath.resolve("gfx/interface/icons/traits/leader_trait_icons/" + traitId + ".dds");
    }

    private byte[] convertDdsToPng(Path ddsPath) throws IOException {
        BufferedImage image = ImageIO.read(ddsPath.toFile());
        if (image == null) {
            log.warn("ImageIO.read returned null for {}", ddsPath);
            return null;
        }

        var baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
