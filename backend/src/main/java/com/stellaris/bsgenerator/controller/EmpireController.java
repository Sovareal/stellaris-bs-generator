package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.dto.EmpireResponse;
import com.stellaris.bsgenerator.dto.ExportRequest;
import com.stellaris.bsgenerator.dto.ExportResponse;
import com.stellaris.bsgenerator.dto.RerollRequest;
import com.stellaris.bsgenerator.engine.*;
import com.stellaris.bsgenerator.parser.LocalizationService;
import com.stellaris.bsgenerator.service.EmpireExporterService;
import com.stellaris.bsgenerator.service.ExportOptions;
import com.stellaris.bsgenerator.service.UserEmpireFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/empire")
@RequiredArgsConstructor
public class EmpireController {

    private final EmpireGeneratorService generatorService;
    private final RerollService rerollService;
    private final LocalizationService localizationService;
    private final EmpireExporterService exporterService;
    private final UserEmpireFileService userEmpireFileService;

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

        // Trait-level operations — unlimited, don't consume the session reroll token
        String cat = request.category().toLowerCase();
        if ("trait_single".equals(cat)) {
            if (request.traitId() == null || request.traitId().isBlank()) {
                throw new IllegalArgumentException("traitId is required for trait_single reroll");
            }
            var updated = rerollService.rerollSingleTrait(session, request.traitId());
            return EmpireResponse.from(updated, session, localizationService);
        }
        if ("trait_add".equals(cat)) {
            var updated = rerollService.addOneTrait(session);
            return EmpireResponse.from(updated, session, localizationService);
        }
        if ("leader_trait_add".equals(cat)) {
            var updated = rerollService.addLeaderTrait(session);
            return EmpireResponse.from(updated, session, localizationService);
        }

        RerollCategory category = switch (cat) {
            case "ethics" -> RerollCategory.ETHICS;
            case "authority" -> RerollCategory.AUTHORITY;
            case "civic1" -> RerollCategory.CIVIC1;
            case "civic2" -> RerollCategory.CIVIC2;
            case "origin" -> RerollCategory.ORIGIN;
            case "homeworld" -> RerollCategory.HOMEWORLD;
            case "shipset" -> RerollCategory.SHIPSET;
            case "leader" -> RerollCategory.LEADER;
            case "secondaryspecies" -> RerollCategory.SECONDARY_SPECIES;
            default -> throw new IllegalArgumentException("Unknown reroll category: " + request.category());
        };

        var updated = rerollService.reroll(session, category);
        return EmpireResponse.from(updated, session, localizationService);
    }

    @PostMapping("/export")
    public ResponseEntity<ExportResponse> exportEmpire(@RequestBody ExportRequest req) throws IOException {
        if (session == null || session.getEmpire() == null) {
            throw new IllegalStateException("No empire generated yet — generate an empire first");
        }

        String plural = (req.speciesPlural() != null && !req.speciesPlural().isBlank())
                ? req.speciesPlural()
                : req.speciesName() + "s";
        String adjective = (req.speciesAdjective() != null && !req.speciesAdjective().isBlank())
                ? req.speciesAdjective()
                : req.speciesName();

        var opts = new ExportOptions(
                req.empireName(),
                req.speciesName(),
                plural,
                adjective,
                req.rulerName()
        );

        String block = exporterService.buildEmpireBlock(session.getEmpire(), opts);
        Path file = userEmpireFileService.appendEmpire(block);

        return ResponseEntity.ok(new ExportResponse(true, file.toString(), req.empireName()));
    }
}
