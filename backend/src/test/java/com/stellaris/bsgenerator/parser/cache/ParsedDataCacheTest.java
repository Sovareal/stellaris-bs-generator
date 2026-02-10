package com.stellaris.bsgenerator.parser.cache;

import tools.jackson.databind.json.JsonMapper;
import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParsedDataCacheTest {

    @TempDir
    Path tempDir;

    private ParsedDataCache cache;

    @BeforeEach
    void setUp() {
        var props = new ParserProperties("dummy", tempDir.toString());
        cache = new ParsedDataCache(props, JsonMapper.builder().build());
    }

    @Test
    void roundTrip() {
        var node = ClausewitzNode.root(List.of(
                ClausewitzNode.leaf("key", "value")
        ));
        Map<String, ClausewitzNode> data = Map.of("test", node);

        cache.save("fp123", data);
        ParsedDataCache.CacheEntry loaded = cache.load();

        assertNotNull(loaded);
        assertEquals("fp123", loaded.fingerprint());
        assertNotNull(loaded.data().get("test"));
    }

    @Test
    void missingCacheReturnsNull() {
        assertNull(cache.load());
    }

    @Test
    void corruptCacheReturnsNull() throws IOException {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("stellaris-cache.json"), "not json{{{");
        assertNull(cache.load());
    }

    @Test
    void clearRemovesFile() {
        cache.save("fp", Map.of());
        cache.clear();
        assertNull(cache.load());
    }

    @Test
    void savesNestedNodes() {
        var inner = ClausewitzNode.block("ethics", List.of(
                ClausewitzNode.leaf("cost", "2"),
                ClausewitzNode.block("tags", List.of(
                        ClausewitzNode.bareValue("TAG_A"),
                        ClausewitzNode.bareValue("TAG_B")
                ))
        ));
        var root = ClausewitzNode.root(List.of(inner));
        Map<String, ClausewitzNode> data = Map.of("ethics", root);

        cache.save("fp456", data);
        ParsedDataCache.CacheEntry loaded = cache.load();

        assertNotNull(loaded);
        assertEquals("fp456", loaded.fingerprint());
    }
}
