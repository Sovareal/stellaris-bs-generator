package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.engine.*;
import com.stellaris.bsgenerator.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/empire")
@RequiredArgsConstructor
public class EmpireController {

    private final EmpireGeneratorService generatorService;
    private final RerollService rerollService;

    // In-memory session (single user desktop app)
    private GenerationSession session;

    public record EmpireResponse(
            List<EthicDto> ethics,
            AuthorityDto authority,
            List<CivicDto> civics,
            OriginDto origin,
            ArchetypeDto speciesArchetype,
            List<TraitDto> speciesTraits,
            int traitPointsUsed,
            int traitPointsBudget,
            PlanetClassDto homeworld,
            String shipset,
            LeaderDto leader,
            Map<String, Boolean> rerollsAvailable
    ) {
        static EmpireResponse from(GeneratedEmpire empire, GenerationSession session) {
            return new EmpireResponse(
                    empire.ethics().stream().map(EthicDto::from).toList(),
                    AuthorityDto.from(empire.authority()),
                    empire.civics().stream().map(CivicDto::from).toList(),
                    OriginDto.from(empire.origin()),
                    ArchetypeDto.from(empire.speciesArchetype()),
                    empire.speciesTraits().stream().map(TraitDto::from).toList(),
                    empire.traitPointsUsed(),
                    empire.traitPointsBudget(),
                    PlanetClassDto.from(empire.homeworld()),
                    empire.shipset().id(),
                    LeaderDto.from(empire.leaderClass(), empire.leaderTrait()),
                    buildRerollMap(session)
            );
        }

        private static Map<String, Boolean> buildRerollMap(GenerationSession session) {
            boolean available = session.canReroll();
            var map = new LinkedHashMap<String, Boolean>();
            map.put("ethics", available);
            map.put("authority", available);
            map.put("civic1", available);
            map.put("civic2", available);
            map.put("origin", available);
            map.put("traits", available);
            map.put("homeworld", available);
            map.put("shipset", available);
            map.put("leader", available);
            return map;
        }
    }

    public record EthicDto(String id, int cost, boolean isFanatic) {
        static EthicDto from(Ethic e) { return new EthicDto(e.id(), e.cost(), e.isFanatic()); }
    }

    public record AuthorityDto(String id, boolean isGestalt) {
        static AuthorityDto from(Authority a) { return new AuthorityDto(a.id(), a.isGestalt()); }
    }

    public record CivicDto(String id) {
        static CivicDto from(Civic c) { return new CivicDto(c.id()); }
    }

    public record OriginDto(String id, String dlcRequirement) {
        static OriginDto from(Origin o) { return new OriginDto(o.id(), o.dlcRequirement()); }
    }

    public record ArchetypeDto(String id, int traitPoints, int maxTraits, boolean robotic) {
        static ArchetypeDto from(SpeciesArchetype a) { return new ArchetypeDto(a.id(), a.traitPoints(), a.maxTraits(), a.robotic()); }
    }

    public record TraitDto(String id, int cost, List<String> allowedArchetypes) {
        static TraitDto from(SpeciesTrait t) { return new TraitDto(t.id(), t.cost(), t.allowedArchetypes()); }
    }

    public record PlanetClassDto(String id, String climate) {
        static PlanetClassDto from(PlanetClass p) { return new PlanetClassDto(p.id(), p.climate()); }
    }

    public record LeaderDto(String leaderClass, String traitId) {
        static LeaderDto from(String leaderClass, StartingRulerTrait trait) {
            return new LeaderDto(leaderClass, trait != null ? trait.id() : null);
        }
    }

    public record RerollRequest(String category) {}

    @PostMapping("/generate")
    public EmpireResponse generate() {
        var empire = generatorService.generate();
        session = new GenerationSession(empire);
        return EmpireResponse.from(empire, session);
    }

    @PostMapping("/reroll")
    public EmpireResponse reroll(@RequestBody RerollRequest request) {
        if (session == null) {
            throw new IllegalStateException("No active session — generate an empire first");
        }

        RerollCategory category = switch (request.category().toLowerCase()) {
            case "ethics" -> RerollCategory.ETHICS;
            case "authority" -> RerollCategory.AUTHORITY;
            case "civic1" -> RerollCategory.CIVIC1;
            case "civic2" -> RerollCategory.CIVIC2;
            case "origin" -> RerollCategory.ORIGIN;
            case "traits" -> RerollCategory.TRAITS;
            case "homeworld" -> RerollCategory.HOMEWORLD;
            case "shipset" -> RerollCategory.SHIPSET;
            case "leader" -> RerollCategory.LEADER;
            default -> throw new IllegalArgumentException("Unknown reroll category: " + request.category());
        };

        var updated = rerollService.reroll(session, category);
        return EmpireResponse.from(updated, session);
    }
}
