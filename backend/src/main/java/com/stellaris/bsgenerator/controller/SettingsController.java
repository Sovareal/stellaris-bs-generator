package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final GameDataManager gameDataManager;

    public record SettingsResponse(String gamePath, boolean valid, String validationMessage) {}

    public record SaveSettingsRequest(String gamePath) {}

    @GetMapping
    public SettingsResponse getSettings() {
        var settings = settingsService.load();
        var validation = settingsService.validate(settings.gamePath());
        return new SettingsResponse(settings.gamePath(), validation.valid(), validation.message());
    }

    @PutMapping
    public SettingsResponse saveSettings(@RequestBody SaveSettingsRequest request) throws IOException {
        var validation = settingsService.validate(request.gamePath());
        if (!validation.valid()) {
            return new SettingsResponse(request.gamePath(), false, validation.message());
        }

        settingsService.save(new SettingsService.Settings(request.gamePath()));
        log.info("Settings saved, triggering data reload for path: {}", request.gamePath());

        try {
            gameDataManager.forceReload();
        } catch (IOException e) {
            log.error("Data reload failed after settings save: {}", e.getMessage());
            return new SettingsResponse(request.gamePath(), true, "Settings saved but data reload failed: " + e.getMessage());
        }

        return new SettingsResponse(request.gamePath(), true, "Settings saved and data reloaded successfully");
    }
}
