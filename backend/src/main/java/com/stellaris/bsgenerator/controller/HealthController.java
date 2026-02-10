package com.stellaris.bsgenerator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    public record HealthResponse(String status, String version) {}

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "0.1.0");
    }
}
