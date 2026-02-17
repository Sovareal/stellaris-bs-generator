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

    public record SecondarySpeciesDto(
            String title,
            String titleDisplayName,
            String speciesClass,
            String speciesClassName,
            List<TraitDto> enforcedTraits,
            List<TraitDto> additionalTraits,
            int traitPointsUsed,
            int traitPointsBudget,
            int maxTraitPicks
    ) {
        static SecondarySpeciesDto from(SecondarySpecies ss, LocalizationService loc) {
            if (ss == null) return null;
            return new SecondarySpeciesDto(
                    ss.title(),
                    loc.getDisplayName(ss.title()),
                    ss.speciesClass(),
                    loc.getDisplayName(ss.speciesClass()),
                    ss.enforcedTraits().stream().map(t -> TraitDto.from(t, loc)).toList(),
                    ss.additionalTraits().stream().map(t -> TraitDto.from(t, loc)).toList(),
                    ss.traitPointsUsed(),
                    ss.traitPointsBudget(),
                    ss.maxTraitPicks()
            );
        }
    }

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
            SecondarySpeciesDto secondarySpecies,
            Map<String, Boolean> rerollsAvailable
    ) {
        static EmpireResponse from(GeneratedEmpire empire, GenerationSession session, LocalizationService loc) {
            // Mark origin + civic enforced species traits
            var enforcedIds = new java.util.HashSet<>(empire.origin().enforcedTraitIds());
            for (var civic : empire.civics()) {
                enforcedIds.addAll(civic.enforcedTraitIds());
            }
            var traitDtos = empire.speciesTraits().stream()
                    .map(t -> enforcedIds.contains(t.id()) ? TraitDto.fromEnforced(t, loc) : TraitDto.from(t, loc))
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
                    empire.shipset().id(),
                    loc.getDisplayName(empire.shipset().id()),
                    LeaderDto.from(empire.leaderClass(), empire.leaderTraits(), loc),
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
            map.put("traits", available);
            map.put("homeworld", available);
            map.put("shipset", available);
            map.put("leader", available);
            if (empire.secondarySpecies() != null) {
                map.put("secondaryspecies", available);
            }
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

    public record TraitDto(String id, String displayName, int cost, List<String> allowedArchetypes, boolean enforced) {
        static TraitDto from(SpeciesTrait t, LocalizationService loc) {
            return new TraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.allowedArchetypes(), false);
        }

        static TraitDto fromEnforced(SpeciesTrait t, LocalizationService loc) {
            return new TraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.allowedArchetypes(), true);
        }
    }

    public record PlanetClassDto(String id, String displayName, String climate) {
        static PlanetClassDto from(PlanetClass p, LocalizationService loc) {
            return new PlanetClassDto(p.id(), loc.getDisplayName(p.id()), p.climate());
        }
    }

    public record LeaderTraitDto(String id, String displayName, int cost, String gfxKey) {}

    public record LeaderDto(String leaderClass, List<LeaderTraitDto> traits) {
        static LeaderDto from(String leaderClass, List<StartingRulerTrait> leaderTraits, LocalizationService loc) {
            var traitDtos = leaderTraits.stream()
                    .map(t -> new LeaderTraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.gfxKey()))
                    .toList();
            return new LeaderDto(leaderClass, traitDtos);
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
            case "secondaryspecies" -> RerollCategory.SECONDARY_SPECIES;
            default -> throw new IllegalArgumentException("Unknown reroll category: " + request.category());
        };

        var updated = rerollService.reroll(session, category);
        return EmpireResponse.from(updated, session, localizationService);
    }
}
