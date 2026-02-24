package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.engine.GeneratedEmpire;
import com.stellaris.bsgenerator.engine.GenerationSession;
import com.stellaris.bsgenerator.parser.LocalizationService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EmpireResponse(
        List<EthicDto> ethics,
        AuthorityDto authority,
        List<CivicDto> civics,
        OriginDto origin,
        ArchetypeDto speciesArchetype,
        String speciesClass,
        String speciesClassName,
        List<TraitDto> speciesTraits,
        int traitPointsUsed,
        int traitPointsBudget,
        PlanetClassDto homeworld,
        PlanetClassDto habitabilityPreference,
        String shipset,
        String shipsetName,
        LeaderDto leader,
        SecondarySpeciesDto secondarySpecies,
        Map<String, Boolean> rerollsAvailable
) {
    public static EmpireResponse from(GeneratedEmpire empire, GenerationSession session, LocalizationService loc) {
        var originEnforcedIds = new java.util.HashSet<>(empire.origin().enforcedTraitIds());
        var civicEnforcedIds = new java.util.HashSet<String>();
        for (var civic : empire.civics()) {
            civicEnforcedIds.addAll(civic.enforcedTraitIds());
        }
        var traitDtos = empire.speciesTraits().stream()
                .map(t -> originEnforcedIds.contains(t.id()) ? TraitDto.fromFreeEnforced(t, loc)
                        : civicEnforcedIds.contains(t.id())
                            ? (t.cost() == 0 ? TraitDto.fromFreeEnforced(t, loc) : TraitDto.fromEnforced(t, loc))
                        : TraitDto.from(t, loc))
                .toList();

        return new EmpireResponse(
                empire.ethics().stream().map(e -> EthicDto.from(e, loc)).toList(),
                AuthorityDto.from(empire.authority(), loc),
                empire.civics().stream().map(c -> CivicDto.from(c, loc)).toList(),
                OriginDto.from(empire.origin(), loc),
                ArchetypeDto.from(empire.speciesArchetype(), loc),
                empire.speciesClass(),
                loc.getDisplayName(empire.speciesClass()),
                traitDtos,
                empire.traitPointsUsed(),
                empire.traitPointsBudget(),
                PlanetClassDto.from(empire.homeworld(), loc),
                PlanetClassDto.from(empire.habitabilityPreference(), loc),
                empire.shipset().id(),
                loc.getDisplayName(empire.shipset().id()),
                LeaderDto.from(empire.leaderClass(), empire.leaderTraits(), loc, empire.origin().id()),
                SecondarySpeciesDto.from(empire.secondarySpecies(), loc),
                buildRerollMap(empire, session)
        );
    }

    private static Map<String, Boolean> buildRerollMap(GeneratedEmpire empire, GenerationSession session) {
        boolean available = session.canReroll();
        var map = new LinkedHashMap<String, Boolean>();
        map.put("ethics", available);
        map.put("authority", available);
        map.put("civic1", available);
        map.put("civic2", available);
        map.put("origin", available);
        map.put("homeworld", available);
        map.put("shipset", available);
        map.put("leader", available);
        if (empire.secondarySpecies() != null) {
            map.put("secondaryspecies", available);
        }
        return map;
    }
}
