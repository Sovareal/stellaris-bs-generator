package com.stellaris.bsgenerator.model;

import java.util.List;

/**
 * A trait that can be selected for a starting ruler.
 *
 * @param id               e.g. "leader_trait_principled"
 * @param leaderClasses    which classes can use this trait: "official", "commander", "scientist"
 * @param forbiddenOrigins origins that cannot use this trait
 * @param allowedEthics    if non-empty, only empires with one of these ethics can use it
 */
public record StartingRulerTrait(
        String id,
        List<String> leaderClasses,
        List<String> forbiddenOrigins,
        List<String> allowedEthics
) {}
