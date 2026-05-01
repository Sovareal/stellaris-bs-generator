package com.stellaris.bsgenerator.namepool.extractor;

import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.loader.DirectoryLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Extracts ruler (character) and regnal names from common/name_lists/*.txt.
 * Uses full_names_male + full_names_female as-is (no first/last splitting).
 * Regnal names come from regnal_first_names blocks.
 */
@Slf4j
public class RulerNameExtractor {

    public record Result(List<String> rulerNames, List<String> regnalNames) {}

    public Result extract(Path commonDir, Map<String, String> loc) throws IOException {
        var root = DirectoryLoader.loadDirectory(commonDir.resolve("name_lists"), Map.of());

        var rulerSet = new LinkedHashSet<String>();
        var regnalSet = new LinkedHashSet<String>();

        for (var nameList : root.children()) {
            var charNames = nameList.child("character_names").orElse(null);
            if (charNames == null) continue;

            // Most name lists have a "default" block; some use a numeric key -- collect all
            for (var pool : charNames.children()) {
                addResolved(rulerSet,  pool, "full_names_male",    loc);
                addResolved(rulerSet,  pool, "full_names_female",  loc);
                addResolved(regnalSet, pool, "regnal_first_names", loc);
            }
        }

        log.info("Extracted {} ruler names, {} regnal names from name lists",
                rulerSet.size(), regnalSet.size());
        return new Result(List.copyOf(rulerSet), List.copyOf(regnalSet));
    }

    private static void addResolved(Set<String> target, ClausewitzNode pool,
                                    String key, Map<String, String> loc) {
        pool.child(key).ifPresent(block ->
                block.bareValues().forEach(locKey -> {
                    String resolved = loc.getOrDefault(locKey, null);
                    if (resolved != null && !resolved.isBlank() && !resolved.contains("$")) {
                        target.add(resolved);
                    }
                })
        );
    }
}
