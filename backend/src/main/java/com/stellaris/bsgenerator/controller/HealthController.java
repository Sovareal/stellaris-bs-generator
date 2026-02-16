package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final GameDataManager gameDataManager;

    public record HealthResponse(String status, String version, String dataStatus, String dataError) {}

    @GetMapping("/health")
    public HealthResponse health() {
        var ds = gameDataManager.getDataStatus();
        return new HealthResponse(
                "ok",
                "0.1.0",
                ds.name().toLowerCase(),
                gameDataManager.getDataError()
        );
    }
}
