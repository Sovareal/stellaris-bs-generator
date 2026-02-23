package com.stellaris.bsgenerator.dto;

/**
 * Response body for POST /api/empire/export.
 */
public record ExportResponse(
        boolean success,
        String filePath,
        String empireName
) {}
