package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.engine.*;
import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.parser.LocalizationService;
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
    private final LocalizationService localizationService;

    // In-memory session (single user desktop app)
    private GenerationSession session;

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
            String shipset,
            String shipsetName,
            LeaderDto leader,
            Map<String, Boolean> rerollsAvailable
    ) {
        static EmpireResponse from(GeneratedEmpire empire, GenerationSession session, LocalizationService loc) {
            return new EmpireResponse(
                    empire.ethics().stream().map(e -> EthicDto.from(e, loc)).toList(),
                    AuthorityDto.from(empire.authority(), loc),
                    empire.civics().stream().map(c -> CivicDto.from(c, loc)).toList(),
                    OriginDto.from(empire.origin(), loc),
                    ArchetypeDto.from(empire.speciesArchetype(), loc),
                    empire.speciesClass(),
                    loc.getDisplayName(empire.speciesClass()),
                    empire.speciesTraits().stream().map(t -> TraitDto.from(t, loc)).toList(),
                    empire.traitPointsUsed(),
                    empire.traitPointsBudget(),
                    PlanetClassDto.from(empire.homeworld(), loc),
                    empire.shipset().id(),
                    loc.getDisplayName(empire.shipset().id()),
                    LeaderDto.from(empire.leaderClass(), empire.leaderTrait(), loc),
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

    public record EthicDto(String id, String displayName, int cost, boolean isFanatic) {
        static EthicDto from(Ethic e, LocalizationService loc) {
            return new EthicDto(e.id(), loc.getDisplayName(e.id()), e.cost(), e.isFanatic());
        }
    }

    public record AuthorityDto(String id, String displayName, boolean isGestalt) {
        static AuthorityDto from(Authority a, LocalizationService loc) {
            return new AuthorityDto(a.id(), loc.getDisplayName(a.id()), a.isGestalt());
        }
    }

    public record CivicDto(String id, String displayName) {
        static CivicDto from(Civic c, LocalizationService loc) {
            return new CivicDto(c.id(), loc.getDisplayName(c.id()));
        }
    }

    public record OriginDto(String id, String displayName, String dlcRequirement) {
        static OriginDto from(Origin o, LocalizationService loc) {
            return new OriginDto(o.id(), loc.getDisplayName(o.id()), o.dlcRequirement());
        }
    }

    public record ArchetypeDto(String id, String displayName, int traitPoints, int maxTraits, boolean robotic) {
        static ArchetypeDto from(SpeciesArchetype a, LocalizationService loc) {
            return new ArchetypeDto(a.id(), loc.getDisplayName(a.id()), a.traitPoints(), a.maxTraits(), a.robotic());
        }
    }

    public record TraitDto(String id, String displayName, int cost, List<String> allowedArchetypes) {
        static TraitDto from(SpeciesTrait t, LocalizationService loc) {
            return new TraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.allowedArchetypes());
        }
    }

    public record PlanetClassDto(String id, String displayName, String climate) {
        static PlanetClassDto from(PlanetClass p, LocalizationService loc) {
            return new PlanetClassDto(p.id(), loc.getDisplayName(p.id()), p.climate());
        }
    }

    public record LeaderDto(String leaderClass, String traitId, String traitDisplayName) {
        static LeaderDto from(String leaderClass, StartingRulerTrait trait, LocalizationService loc) {
            return new LeaderDto(
                    leaderClass,
                    trait != null ? trait.id() : null,
                    trait != null ? loc.getDisplayName(trait.id()) : null
            );
        }
    }

    public record RerollRequest(String category) {}

    @PostMapping("/generate")
    public EmpireResponse generate() {
        var empire = generatorService.generate();
        session = new GenerationSession(empire);
        return EmpireResponse.from(empire, session, localizationService);
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
        return EmpireResponse.from(updated, session, localizationService);
    }
}
