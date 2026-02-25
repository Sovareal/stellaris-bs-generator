# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-02-25

### Added

#### Infrastructure & Tooling
- Initialize git repository with README and gitignore
- Scaffold Spring Boot backend (Task 0.1)
- Scaffold frontend with Tauri 2.0 and sidecar wiring (Tasks 0.2, 0.3)
- Add Tauri build pipeline with bundled JRE and NSIS installer (Phase 30)
- Cross-platform build support with `before-build.js` script
- App branding and custom icons (Phase 31)
- GitHub Actions CI workflow (backend tests + TypeScript type-check)
- GitHub Actions release workflow (3-platform Tauri builds on `v*` tags)
- `scripts/bump-version.js` for syncing version across all manifest files

#### Backend -- Parsing & Extraction
- Implement Clausewitz tokenizer (Task 1.1)
- Implement Clausewitz AST parser (Task 1.2)
- Implement multi-file loader with global variable support (Task 1.3)
- Implement version detection and data cache (Task 1.4)
- Implement game data models and extractors (Phase 2): ethics, civics, traits, origins, species classes
- Icon resolution with DDS-to-PNG conversion and caching
- Extract DTOs to package, add habitability preference
- Civic modifier and description extraction

#### Backend -- Generation & Rules
- Implement rule/constraint engine (Phase 3)
- Implement empire generation engine (Phase 4)
- Gestalt consciousness empire generation (Task 6.1)
- Origin/civic/ethic-aware trait filtering (Task 6.2)
- Single reroll per generation enforcement (Task 6.3)
- Secondary species generation for multi-species origins and civics
- Origin-enforced species traits and Luminary leader multi-trait selection
- Homeworld, shipset, and starting leader generation (Phase 7)
- Species class model, extraction, and trait filtering
- Rarity-weighted origin selection with stratified promotion paths
- Per-trait reroll via `TRAIT_SINGLE` category

#### Frontend -- UI & Features
- TypeScript types, API client, and Zustand store (Task 5.5)
- Layout shell, header/footer, loading/error screens (Task 5.1)
- Empire display card with ethics, traits, and slot components (Task 5.3)
- Generate button, reroll controls, and error toast (Task 5.4)
- shadcn/ui component library, Zustand state, Stellaris theme
- Error handling, settings page, and configurable game path
- Homeworld/habitability preference display
- Game entity icons in the UI
- Save to Game feature (Phase 35)
- Per-trait reroll from the empire card
- Secondary species slot with trait rolling
- Trait-by-trait rolling animation for species and Luminary leader traits (Phase 39)
- Trait preservation, picks fixes, and remove-trait controls (Phase 41)
- No-blink trait roll, color coding, and leader 3-case display (Phase 42)
- Secondary species trait rolling (Phase 43)
- Roll Trait / Done Rolling button animations (Phase 46)
- Auto-dismiss save toast on next click

### Fixed

- Localization display names, homeworld trait constraints, and trait points display
- 13 playtest issues in the generation pipeline (Phase 9)
- CORS for Tauri production origin; hide backend console window on Windows
- Filter non-selectable origins in OriginExtractor
- Show real trait cost for enforced traits (e.g. Aquatic shows +2, not +0)
- Homeworld/habitability preference sync on origin/trait reroll
- Filter civics requiring absent DLC (removes `civic_corporate_dominion`)
- Origin distribution floor: all 46 origins achieve >= 1.5% representation
- Display fixes, generation order restructure, civic-enforced traits
- Count enforced traits against picks/budget; fix export government + leader traits
- Reroll token enforcement and trait budget rolling logic (Phase 40)
- Budget fix, no-blink trait roll, color coding, leader 3-case (Phase 42)
- Shipset enforcement, export names, leader trait styling (Phase 45)
- Resolve 7 issues from manual testing (Phase 44)
- `beforeBuildCommand` path for Tauri 2 CWD resolution
- Wildcard CORS, backend log capture

### Removed

- Civic description/modifier tooltip feature (reverted -- incomplete)

[Unreleased]: https://github.com/sova/stellaris-bs-generator/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/sova/stellaris-bs-generator/releases/tag/v0.1.0
