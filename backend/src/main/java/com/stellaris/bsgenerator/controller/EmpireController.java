package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.dto.EmpireResponse;
import com.stellaris.bsgenerator.dto.RerollRequest;
import com.stellaris.bsgenerator.engine.*;
import com.stellaris.bsgenerator.parser.LocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empire")
@RequiredArgsConstructor
public class EmpireController {

    private final EmpireGeneratorService generatorService;
    private final RerollService rerollService;
    private final LocalizationService localizationService;

    // In-memory session (single user desktop app)
    private GenerationSession session;

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

        // Single-trait reroll is handled separately since it requires an additional traitId parameter
        if ("trait_single".equalsIgnoreCase(request.category())) {
            if (request.traitId() == null || request.traitId().isBlank()) {
                throw new IllegalArgumentException("traitId is required for trait_single reroll");
            }
            var updated = rerollService.rerollSingleTrait(session, request.traitId());
            return EmpireResponse.from(updated, session, localizationService);
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
