package com.stellaris.bsgenerator.icon;

import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.ast.ClausewitzParser;
import com.stellaris.bsgenerator.parser.token.Token;
import com.stellaris.bsgenerator.parser.token.Tokenizer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses leader_traits.gfx to build a map of GFX name → relative DDS path.
 * e.g. "GFX_leader_trait_adaptable" → "gfx/interface/icons/traits/leader_trait_icons/adaptable.dds"
 */
@Slf4j
public class LeaderTraitGfxParser {

    public static Map<String, String> parse(Path gfxFile) throws IOException {
        Map<String, String> gfxMap = new HashMap<>();

        if (!Files.exists(gfxFile)) {
            log.warn("Leader traits GFX file not found: {}", gfxFile);
            return gfxMap;
        }

        String content = Files.readString(gfxFile, StandardCharsets.UTF_8);
        List<Token> tokens = Tokenizer.tokenize(content);
        ClausewitzNode root = ClausewitzParser.parse(tokens, Map.of());

        // Root has spriteTypes = { spriteType = { name=..., texturefile=... } ... }
        for (var spriteTypes : root.children()) {
            if (!"spriteTypes".equals(spriteTypes.key())) continue;

            for (var spriteType : spriteTypes.children("spriteType")) {
                var name = spriteType.childValue("name").orElse(null);
                var texturefile = spriteType.childValue("texturefile").orElse(null);
                if (name != null && texturefile != null) {
                    gfxMap.put(name, texturefile);
                }
            }
        }

        log.info("Parsed {} GFX entries from {}", gfxMap.size(), gfxFile.getFileName());
        return gfxMap;
    }
}
