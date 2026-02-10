package com.stellaris.bsgenerator.model;

public record SpeciesArchetype(
        String id,
        int traitPoints,
        int maxTraits,
        boolean robotic
) {}
