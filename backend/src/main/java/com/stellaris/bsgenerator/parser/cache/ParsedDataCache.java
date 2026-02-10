package com.stellaris.bsgenerator.parser.cache;

import tools.jackson.databind.ObjectMapper;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
public class ParsedDataCache {

    private static final String CACHE_FILE = "stellaris-cache.json";

    private final Path cachePath;
    private final ObjectMapper mapper;

    public ParsedDataCache(ParserProperties properties, ObjectMapper mapper) {
        this.cachePath = Path.of(properties.cachePath()).resolve(CACHE_FILE);
        this.mapper = mapper;
    }

    public record CacheEntry(String fingerprint, Map<String, ClausewitzNode> data) {}

    public CacheEntry load() {
        if (!Files.exists(cachePath)) {
            log.debug("No cache file found at {}", cachePath);
            return null;
        }
        try {
            CacheEntry entry = mapper.readValue(cachePath.toFile(), CacheEntry.class);
            log.debug("Loaded cache with fingerprint: {}", entry.fingerprint());
            return entry;
        } catch (Exception e) {
            log.warn("Failed to read cache file: {}", e.getMessage());
            return null;
        }
    }

    public void save(String fingerprint, Map<String, ClausewitzNode> data) {
        try {
            Files.createDirectories(cachePath.getParent());
            mapper.writeValue(cachePath.toFile(), new CacheEntry(fingerprint, data));
            log.info("Cache saved to {}", cachePath);
        } catch (Exception e) {
            log.warn("Failed to write cache file: {}", e.getMessage());
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(cachePath);
            log.info("Cache cleared");
        } catch (IOException e) {
            log.warn("Failed to clear cache: {}", e.getMessage());
        }
    }
}
