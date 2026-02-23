package com.stellaris.bsgenerator.dto;

/**
 * Request body for POST /api/empire/export.
 * speciesPlural and speciesAdjective are optional (nullable = auto-derived).
 */
public record ExportRequest(
        String empireName,
        String speciesName,
        String speciesPlural,
        String speciesAdjective,
        String rulerName
) {}
