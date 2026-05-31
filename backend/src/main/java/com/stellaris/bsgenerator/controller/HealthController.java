package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final GameDataManager gameDataManager;
    private final BuildProperties buildProperties;

    public record HealthResponse(String status, String version, String dataStatus, String dataError) {}

    @GetMapping("/health")
    public HealthResponse health(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        log.info("Health check -- Origin: {}", origin != null ? origin : "(none)");
        var ds = gameDataManager.getDataStatus();
        return new HealthResponse(
                "ok",
                buildProperties.getVersion(),
                ds.name().toLowerCase(),
                gameDataManager.getDataError()
        );
    }
}
