package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Civic;
import com.stellaris.bsgenerator.parser.LocalizationService;

public record CivicDto(String id, String displayName) {
    public static CivicDto from(Civic c, LocalizationService loc) {
        return new CivicDto(c.id(), loc.getDisplayName(c.id()));
    }
}
