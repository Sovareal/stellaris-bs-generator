package com.stellaris.bsgenerator.service;

/**
 * User-supplied name options for empire export.
 * All fields must be non-null; plural/adjective should be pre-defaulted before passing here.
 */
public record ExportOptions(
        String empireName,
        String speciesName,
        String speciesPlural,
        String speciesAdjective,
        String rulerName
) {}
