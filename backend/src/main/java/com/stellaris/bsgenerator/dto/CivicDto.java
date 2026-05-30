package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Civic;
import com.stellaris.bsgenerator.parser.LocalizationService;
import org.springframework.lang.Nullable;

public record CivicDto(String id, @Nullable String displayName) {
    public static CivicDto from(Civic c, LocalizationService loc) {
        return new CivicDto(c.id(), loc.getDisplayName(c.id()));
    }
}
