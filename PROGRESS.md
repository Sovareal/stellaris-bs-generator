# Stellaris BS Empire Generator - Progress Tracker

> **How to use:** This file tracks current state only. For phase history see `docs/phases/`. For architectural decisions see `DECISIONS_LOG.md`. For a phase lookup index see `docs/PHASE_REGISTRY.md`.

## Current Session Focus

Session 52: Code review remediation -- Phases 67-71 planned from stellaris_code_review_summary.md.

## Last Completed Task

Phase 70: Dynamic port allocation (TcpListener::bind 0, --server.port=PORT); get_backend_port Tauri command; crash monitor thread + backend-crashed event; backendPortPromise in api.ts; listen in useBackendReady.ts.

## Next Up

Phase 71: Type generation -- typescript-generator Gradle plugin auto-generating empire.ts from Java DTOs.

---

## Blockers & Issues

| Issue | Status | Notes |
|-------|--------|-------|
| Windows SDK missing | CLOSED | Tauri Rust build fails -- `kernel32.lib` not found. Need to install Windows 11 SDK via VS Installer |

---

## Key Decisions (load-bearing for active work)

| Decision | Detail |
|----------|--------|
| Jackson 3.x namespace | `tools.jackson.*` not `com.fasterxml.*`. `JsonMapper.builder().build()` instead of `new ObjectMapper()`. |
| Extended identifier chars | Clausewitz tokenizer isIdentPart includes `\|`, `/`, `'` for real game file values. |
| Single reroll per session | `boolean hasRerolled` on GenerationSession. One reroll total, not per category. |
| Trait-by-trait rolling | Initial generation starts enforced-only. addOneTrait() rolls one at a time. `traitsFinalized` gates export. |
| Species class selection | Generator picks a species CLASS (e.g. AQUATIC, REP) inside the archetype, then filters traits by it. |
| Origin weight boosting | originRarityWeight() gives 2-6x boost to restricted origins. GESTALT_CHANCE=0.30. FUN/PLANT classes 3x weight. |
| Positive-only restriction detection | authorityRestricted/archetypeRestricted checks only positive requirements to avoid boilerplate NOT blocks inflating weights. |
| Value-typed block parsing | `hsv { 0.5 0.3 0.7 }` syntax: parser consumes trailing `{ block }` after scalar when next token is OPEN_BRACE. |

---

## Session History (recent)

| # | Date | Phases | Summary |
|---|------|--------|---------|
| 49 | 2026-04-30 | 62 | Name pool storage refactor. Moved name lists to backend-owned JSON, removed from EmpireResponse. |
| 50 | ~2026-05 | 63 | Toast visibility fix + dead code removal. |
| 51 | 2026-05-?? | 64, 65, 66 | Species class distribution fix. Fanatic ethics frequency increase. Fanatic pacifist weight override. |

> Full session history (sessions 1-47) is in `DECISIONS_LOG.md`.
