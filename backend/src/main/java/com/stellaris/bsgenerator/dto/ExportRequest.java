package com.stellaris.bsgenerator.dto;

import org.springframework.lang.Nullable;

public record ExportRequest(
        String empireName,
        String speciesName,
        @Nullable String speciesPlural,
        @Nullable String speciesAdjective,
        String rulerName,
        @Nullable String homeworldName,
        @Nullable String homeSystemName
) {}
