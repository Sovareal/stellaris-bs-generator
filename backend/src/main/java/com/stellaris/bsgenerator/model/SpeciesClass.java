package com.stellaris.bsgenerator.model;

/**
 * A species class (e.g., MAM, REP, LITHOID, MACHINE) that determines the
 * visual appearance and trait availability of a species. Each species class
 * belongs to a species archetype.
 */
public record SpeciesClass(
        String id,
        String archetype,
        /** Canonical DLC name required to use this species class, or null if base-game. */
        String dlcRequirement
) {}
