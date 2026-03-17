package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.config.DlcRegistry;
import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final GameDataManager gameDataManager;

    public record DlcInfoDto(String name, String category) {}

    public record SettingsResponse(
            String gamePath,
            boolean valid,
            String validationMessage,
            Set<String> disabledDlcs,
            List<DlcInfoDto> availableDlcs
    ) {}

    public record SaveSettingsRequest(String gamePath, Set<String> disabledDlcs) {}

    @GetMapping
    public SettingsResponse getSettings() {
        var settings = settingsService.load();
        var validation = settingsService.validate(settings.gamePath());
        return toResponse(settings, validation);
    }

    @PutMapping
    public SettingsResponse saveSettings(@RequestBody SaveSettingsRequest request) throws IOException {
        var validation = settingsService.validate(request.gamePath());
        if (!validation.valid()) {
            var current = settingsService.load();
            return new SettingsResponse(request.gamePath(), false, validation.message(),
                    current.disabledDlcs(), availableDlcs());
        }

        var newSettings = new SettingsService.Settings(request.gamePath(), request.disabledDlcs());
        settingsService.save(newSettings);
        log.info("Settings saved, triggering data reload for path: {}", request.gamePath());

        try {
            gameDataManager.forceReload();
        } catch (IOException e) {
            log.error("Data reload failed after settings save: {}", e.getMessage());
            return new SettingsResponse(request.gamePath(), true,
                    "Settings saved but data reload failed: " + e.getMessage(),
                    request.disabledDlcs(), availableDlcs());
        }

        return new SettingsResponse(request.gamePath(), true,
                "Settings saved and data reloaded successfully",
                request.disabledDlcs(), availableDlcs());
    }

    private SettingsResponse toResponse(SettingsService.Settings settings, SettingsService.ValidationResult validation) {
        return new SettingsResponse(settings.gamePath(), validation.valid(), validation.message(),
                settings.disabledDlcs(), availableDlcs());
    }

    private List<DlcInfoDto> availableDlcs() {
        return DlcRegistry.ALL_DLCS.stream()
                .map(d -> new DlcInfoDto(d.name(), d.category()))
                .toList();
    }
}
