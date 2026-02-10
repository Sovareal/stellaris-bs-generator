package com.stellaris.bsgenerator.parser.cache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public final class FileFingerprint {

    private FileFingerprint() {}

    public static String compute(Path gamePath, List<String> subdirectories) throws IOException {
        var entries = new ArrayList<String>();
        Path common = gamePath.resolve("common");

        for (String subdir : subdirectories) {
            Path dir = common.resolve(subdir);
            if (!Files.isDirectory(dir)) continue;

            try (Stream<Path> stream = Files.list(dir)) {
                stream
                    .filter(p -> p.toString().endsWith(".txt"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            String relativePath = gamePath.relativize(p).toString();
                            long lastModified = Files.getLastModifiedTime(p).toMillis();
                            long size = Files.size(p);
                            entries.add(relativePath + ":" + lastModified + ":" + size);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
        }

        entries.sort(String::compareTo);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String entry : entries) {
                digest.update(entry.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
