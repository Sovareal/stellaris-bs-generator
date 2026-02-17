package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.PlanetClass;
import com.stellaris.bsgenerator.parser.LocalizationService;

public record PlanetClassDto(String id, String displayName, String climate) {
    public static PlanetClassDto from(PlanetClass p, LocalizationService loc) {
        return new PlanetClassDto(p.id(), loc.getDisplayName(p.id()), p.climate());
    }
}
