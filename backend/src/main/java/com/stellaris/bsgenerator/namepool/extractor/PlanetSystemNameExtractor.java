package com.stellaris.bsgenerator.namepool.extractor;

import com.stellaris.bsgenerator.parser.loader.DirectoryLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Extracts homeworld names from planet_names.generic.names blocks in name_lists.
 * System names are not present in game files -- the system names pool is hand-authored in custom_names.json.
 */
@Slf4j
public class PlanetSystemNameExtractor {

    public List<String> extractHomeworldNames(Path commonDir, Map<String, String> loc) throws IOException {
        var root = DirectoryLoader.loadDirectory(commonDir.resolve("name_lists"), Map.of());

        var names = new LinkedHashSet<String>();

        for (var nameList : root.children()) {
            nameList.child("planet_names")
                    .flatMap(p -> p.child("generic"))
                    .flatMap(g -> g.child("names"))
                    .ifPresent(namesBlock ->
                            namesBlock.bareValues().forEach(locKey -> {
                                String resolved = loc.getOrDefault(locKey, null);
                                if (resolved != null && !resolved.isBlank()) {
                                    names.add(resolved);
                                }
                            })
                    );
        }

        log.info("Extracted {} homeworld names from name lists", names.size());
        return List.copyOf(names);
    }
}
