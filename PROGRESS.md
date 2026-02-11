# Stellaris BS Empire Generator - Progress Tracker

> **How to use:** Update status and notes as each task is completed. This file is the
> primary context anchor across sessions — read it first to understand where we left off.

## Current Session Focus
Phase 5 complete. Frontend UI fully implemented.

## Last Completed Task
Phase 5 — Frontend UI. All tasks (5.1, 5.3, 5.4, 5.5) complete.

## Next Up
Phase 6: Tasks 6.1–6.3 — Localization, error handling, Tauri packaging

---

## Phase 0: Project Scaffolding

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 0.0 | Initialize Git Repository | DONE | .gitignore, README.md, initial commit, all scaffolding committed |
| 0.1 | Initialize Gradle Backend Project | DONE | Spring Boot 4.0.2, Java 25, Gradle. Builds and runs on :8080 |
| 0.2 | Initialize Tauri + React Frontend | DONE | Vite+React+Tailwind+Tauri 2.0 initialized. Tauri Rust build blocked on Windows SDK (kernel32.lib missing) |
| 0.3 | Sidecar Wiring | DONE | Rust sidecar launcher in lib.rs, frontend useBackendReady hook. Needs runtime verification after Windows SDK install |

## Phase 1: Clausewitz File Parser (Backend Core)

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 1.1 | Clausewitz Tokenizer | DONE | Single-pass char lexer. Handles |, /, ' chars found in real game files. 18 test cases. |
| 1.2 | Clausewitz AST Parser | DONE | Recursive descent → ClausewitzNode tree. Variable resolution, bare values, repeated keys. 14 tests. |
| 1.3 | Multi-File Loader | DONE | ScriptedVariableLoader + DirectoryLoader + GameFileService. BOM stripping, file-local var isolation. Integration test with real game files. |
| 1.4 | Game Version Detection & Data Cache | DONE | GameVersion, FileFingerprint (SHA-256), ParsedDataCache (Jackson 3), GameDataManager, DataController. Endpoints: GET /api/data/version, POST /api/data/reload. |

## Phase 2: Game Data Models & Extraction

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 2.1 | Ethics Model & Extractor | DONE | 17 ethics (8 regular + 8 fanatic + 1 gestalt). Derives isFanatic from cost, links regular/fanatic variants. |
| 2.2 | Authority Model & Extractor | DONE | 7 player authorities. Filters AI-only (auth_ancient_machine_intelligence). Parses potential/possible via RequirementBlockParser. |
| 2.3 | Civic Model & Extractor | DONE | 265 civics. Excludes origins (is_origin=yes). Parses potential/possible blocks. |
| 2.4 | Origin Model & Extractor | DONE | 58 playable origins. Filters non-playable (always=no). Extracts DLC requirements from playable block. |
| 2.5 | Species Archetype & Trait Models | DONE | 6 archetypes (resolves inherit_trait_points_from). 154 creation-eligible traits (filters auto_mod, initial=no). |

## Phase 3: Rule / Constraint Engine

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 3.1 | Requirement Block Data Model | DONE | Pulled forward into Phase 2 — sealed Requirement interface, RequirementCategory enum, RequirementBlock record, RequirementBlockParser. |
| 3.2 | Requirement Evaluator | DONE | EmpireState record + RequirementEvaluator. Pattern matching on sealed Requirement. Defers unselected categories. 12 unit tests. |
| 3.3 | Compatibility Filter Service | DONE | CompatibilityFilterService: filters authorities/civics/origins/traits by compatibility. Excludes already-selected civics. 13 integration tests with full chain. |

## Phase 4: Empire Generation Engine

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 4.1 | Random Empire Generator Service | DONE | EmpireState, WeightedRandom, EmpireGeneratorService. Ethics axis detection via category field. 100-rep stress test passes. |
| 4.2 | Session State & Reroll Logic | DONE | GenerationSession, RerollCategory, RerollService. Per-category reroll with locked selection preservation. 9 integration tests. |
| 4.3 | REST API Endpoints | DONE | EmpireController (POST /api/empire/generate, /reroll). GlobalExceptionHandler. DTO records with reroll availability map. |

## Phase 5: Frontend UI

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 5.1 | App Layout & Theme | DONE | shadcn/ui (button/card/badge/tooltip), Stellaris dark theme, Header/Footer/LoadingScreen/ErrorScreen. Path aliases (@/). |
| 5.2 | Backend Connection Hook | DONE | useBackendReady hook — now also fetches game version from /api/data/version |
| 5.3 | Empire Display Card | DONE | EmpireView, EmpireCard, EmpireSlot, EthicsSlot (fanatic badges), TraitsSlot (cost coloring, point budget) |
| 5.4 | Generate & Reroll Controls | DONE | GenerateButton (sparkles/spinner), RerollButton (dice/tooltip/disabled states), ErrorToast (dismissible) |
| 5.5 | Session State (Zustand) | DONE | TypeScript types matching backend DTOs, typed API client with ApiError, Zustand store with generate/reroll/generationId |

## Phase 6: Polish & Packaging

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 6.1 | Localization Display Names | NOT STARTED | Depends on Phase 4 |
| 6.2 | Error Handling & Edge Cases | NOT STARTED | Depends on 6.1 |
| 6.3 | Tauri Build & Sidecar Packaging | NOT STARTED | Depends on 6.2 |

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-02-10 | Project docs created | CLAUDE.md, REQUIREMENTS.md, IMPLEMENTATION_PLAN.md, PROGRESS.md |
| 2026-02-10 | Gradle over Maven | CLAUDE.md specifies Gradle 9.3+ |
| 2026-02-10 | Git initialized | Track changes per implementation plan task |
| 2026-02-10 | Jackson 3.x namespace | Spring Boot 4.0.2 uses Jackson 3 (tools.jackson.* package, not com.fasterxml.*). JsonMapper.builder().build() instead of new ObjectMapper(). |
| 2026-02-10 | Extended identifier chars | Clausewitz format uses |, /, ' in identifiers (value:x|y|, trait/icon, etc.) — added to tokenizer isIdentPart. |
| 2026-02-10 | application.yml over .properties | YAML is more readable for nested config (stellaris.*, spring.web.cors.*). Switched from .properties. |
| 2026-02-10 | Lombok adoption | @Slf4j, @RequiredArgsConstructor, @Getter reduce service/controller boilerplate. Records kept for DTOs. |
| 2026-02-10 | Configurable settings plan | game-path/cache-path defaults in application.yml. Phase 6 adds settings.json + frontend settings page for user overrides. |
| 2026-02-11 | Task 3.1 pulled into Phase 2 | RequirementBlock model needed by Authority/Civic/Origin extractors, so built as prerequisite step A of Phase 2. |
| 2026-02-11 | auto_mod filter for traits | Traits with `auto_mod = yes` must be filtered even if `initial` is not explicitly `no`. |
| 2026-02-11 | Authority AI filter refined | Cannot filter on `hasCategory(COUNTRY_TYPE)` alone — auth_corporate has `country_type = { NOT = { value = primitive } }`. Must check for `Value("ai_empire")` specifically. |
| 2026-02-11 | Civic dedup in filter | CompatibilityFilterService must exclude already-selected civics from candidates — requirement blocks don't self-exclude. |

## Blockers & Issues

| Issue | Status | Notes |
|-------|--------|-------|
| Windows SDK missing | OPEN | Tauri Rust build fails — `kernel32.lib` not found. Need to install Windows 11 SDK via VS Installer |

## Session History

| # | Date | Tasks Covered | Summary |
|---|------|---------------|---------|
| 1 | 2026-02-10 | Planning only | Created project docs: CLAUDE.md, REQUIREMENTS.md, IMPLEMENTATION_PLAN.md, PROGRESS.md. Analyzed Stellaris game files. |
| 2 | 2026-02-10 | 0.0–0.3, 5.2 | Scaffolding: backend verified, Tauri initialized, sidecar code + health hook written. Git repo initialized. Blocker: Windows SDK needed for Tauri build. |
| 3 | 2026-02-10 | 1.1–1.4 | Phase 1 complete: Clausewitz tokenizer, AST parser, multi-file loader, version detection & cache. 16 production files, 9 test files. Full suite passes in <5s. Pushed. |
| 4 | 2026-02-10 | Arch cleanup | Converted application.properties→yml. Added Lombok (@Slf4j, @RequiredArgsConstructor, @Getter) to 5 classes. Updated CLAUDE.md, README.md, IMPLEMENTATION_PLAN.md, PROGRESS.md, LEARNING.md for consistency. Planned configurable settings for Phase 6. |
| 5 | 2026-02-11 | 2.1–2.5, 3.1 | Phase 2 complete. RequirementBlock model + parser (3.1 pulled forward). 6 entity models, 6 extractors, 7 test files. 25 new files, 1440 LOC. Extraction counts: 17 ethics, 7 authorities, 265 civics, 58 origins, 6 archetypes, 154 traits. All tests pass, bootRun verified. |
| 6 | 2026-02-11 | 3.2–3.3 | Phase 3 complete. EmpireState, RequirementEvaluator (pattern matching on sealed Requirement), CompatibilityFilterService. 5 new files, 25 tests. Fixed civic dedup in filter. |
| 7 | 2026-02-11 | 4.1–4.3 | Phase 4 complete. EmpireGeneratorService (weighted random, ethics axis detection), RerollService (per-category reroll with locked selections), EmpireController REST API. 12 new files, 119 tests. Fixed isSameAxis bug for fanatic ethics. |
| 8 | 2026-02-11 | 5.1, 5.3–5.5 | Phase 5 complete. shadcn/ui + Zustand + lucide-react. 14 new components, typed API client, Zustand store. Always-dark Stellaris theme. Empire card with ethics/authority/civics/origin/traits slots, generate + per-slot reroll buttons. 5 commits, clean build. |
