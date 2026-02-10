package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import com.stellaris.bsgenerator.parser.cache.GameVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final GameDataManager gameDataManager;

    public record VersionResponse(String version, String rawVersion, String buildHash) {
        static VersionResponse from(GameVersion gv) {
            return new VersionResponse(gv.version(), gv.rawVersion(), gv.buildHash());
        }
    }

    @GetMapping("/version")
    public VersionResponse version() {
        GameVersion gv = gameDataManager.getGameVersion();
        if (gv == null) {
            return new VersionResponse("unknown", "unknown", "");
        }
        return VersionResponse.from(gv);
    }

    public record ReloadResponse(String status) {}

    @PostMapping("/reload")
    public ReloadResponse reload() throws IOException {
        gameDataManager.forceReload();
        return new ReloadResponse("reloaded");
    }
}
