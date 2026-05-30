package com.stellaris.bsgenerator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Optional;

/**
 * Locates and writes to the Stellaris user_empire_designs_vX.txt file in
 * the user's Documents/Paradox Interactive/Stellaris/ directory.
 */
@Service
@Slf4j
public class UserEmpireFileService {

    /**
     * Finds the latest user_empire_designs_v*.txt in the Stellaris save directory.
     */
    public Optional<Path> findEmpireDesignsFile() {
        Path stellarisDir = resolveStellarisSaveDir();
        if (!Files.isDirectory(stellarisDir)) {
            log.warn("Stellaris save directory not found: {}", stellarisDir);
            return Optional.empty();
        }

        try (var stream = Files.list(stellarisDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().matches("user_empire_designs_v.*\\.txt"))
                    .max(Comparator.comparing(p -> p.getFileName().toString()));
        } catch (IOException e) {
            log.error("Failed to list Stellaris save directory: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Appends a Clausewitz empire block to the designs file.
     * Creates the file (v3.txt) if no matching file is found.
     *
     * @return the path of the file written to
     */
    public Path appendEmpire(String clausewitzBlock) throws IOException {
        Path file = findEmpireDesignsFile().orElseGet(this::createDefaultDesignsFile);
        log.info("Appending empire to: {}", file);

        AccessDeniedException lastDenied = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Files.writeString(file, "\n" + clausewitzBlock,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return file;
            } catch (AccessDeniedException e) {
                lastDenied = e;
                log.warn("File locked, attempt {}/3: {}", attempt, file);
                if (attempt < 3) {
                    try { Thread.sleep(200); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting for file lock", ie);
                    }
                }
            }
        }
        throw lastDenied;
    }

    private Path resolveStellarisSaveDir() {
        return Path.of(System.getProperty("user.home"),
                "Documents", "Paradox Interactive", "Stellaris");
    }

    private Path createDefaultDesignsFile() {
        Path dir = resolveStellarisSaveDir();
        Path file = dir.resolve("user_empire_designs_v3.txt");
        log.info("No existing empire designs file found; will create: {}", file);
        return file;
    }
}
