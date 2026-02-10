package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.engine.*;
import com.stellaris.bsgenerator.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    buildRerollMap(session)
            );
        }

        private static Map<String, Boolean> buildRerollMap(GenerationSession session) {
            return Map.of(
                    "ethics", session.canReroll(RerollCategory.ETHICS),
                    "authority", session.canReroll(RerollCategory.AUTHORITY),
                    "civic1", session.canReroll(RerollCategory.CIVIC1),
                    "civic2", session.canReroll(RerollCategory.CIVIC2),
                    "origin", session.canReroll(RerollCategory.ORIGIN),
                    "traits", session.canReroll(RerollCategory.TRAITS)
            );
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
            default -> throw new IllegalArgumentException("Unknown reroll category: " + request.category());
        };

        var updated = rerollService.reroll(session, category);
        return EmpireResponse.from(updated, session);
    }
}
