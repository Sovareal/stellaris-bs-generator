package com.stellaris.bsgenerator.model;

/**
 * A habitable planet class that can serve as a homeworld.
 *
 * @param id      e.g. "pc_desert", "pc_continental"
 * @param climate "dry", "wet", or "cold"
 */
public record PlanetClass(
        String id,
        String climate
) {}
