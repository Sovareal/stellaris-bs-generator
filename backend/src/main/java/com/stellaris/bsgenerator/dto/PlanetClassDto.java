package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.PlanetClass;
import com.stellaris.bsgenerator.parser.LocalizationService;
import org.springframework.lang.Nullable;

public record PlanetClassDto(String id, @Nullable String displayName, String climate) {
    public static PlanetClassDto from(PlanetClass p, LocalizationService loc) {
        return new PlanetClassDto(p.id(), loc.getDisplayName(p.id()), p.climate());
    }
}
