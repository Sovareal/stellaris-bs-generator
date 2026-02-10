package com.stellaris.bsgenerator.parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stellaris")
public record ParserProperties(
        String gamePath,
        String cachePath
) {
    public ParserProperties {
        if (cachePath == null || cachePath.isBlank()) {
            cachePath = System.getProperty("user.home") + "/.stellaris-bs-generator";
        }
    }
}
