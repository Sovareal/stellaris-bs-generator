# Claude Code — Project Instructions

## Session Start
Read **PROGRESS.md** first every session. Update it when starting AND after completing each task.
Update **LEARNING.md** after each completed phase.

## Working Documents (gitignored, local only)
- **REQUIREMENTS.md** — Game data reference, entity fields, Clausewitz syntax
- **IMPLEMENTATION_PLAN.md** — 6 phases, 25 tasks, dependency graph
- **PROGRESS.md** — Task status, decisions log, blockers, session history
- **LEARNING.md** — Per-phase retrospective: tools, lessons, Claude cost tips

## Code Style

**Java (Backend):** Java 25. Records for DTOs, pattern matching for switch, constructor injection only. Lombok `@Slf4j`, `@RequiredArgsConstructor`, `@Getter` to reduce boilerplate — use `@Data`/`@Builder` for mutable model classes where records don't fit. Token-based parsing (no heavy regex). Error handling via `Result<T>` or `@ControllerAdvice`. Config via `application.yml` (not .properties).

**TypeScript (Frontend):** Functional components + Hooks. shadcn/ui primitives. Tailwind utility classes. Zustand for state.

## Git
Commit after each task, push after finishing each phase. Conventional-style messages. No co-author lines. Never add files listed in `.gitignore` to commits (no `git add -f` for ignored files).

## Key Paths
- Stellaris game files: `F:\Games\SteamLibrary\steamapps\common\Stellaris`
- Backend: `backend/` (Spring Boot 4.0, Gradle, port 8080)
- Frontend: `frontend/` (Vite + React 19, port 5173)
- Tauri shell: `frontend/src-tauri/`

## Context & Cost Rules
1. **Validate before building.** Check prerequisites (tools, ports) before any build. Never retry a failed build without diagnosing first.
2. **Read files once, read small.** Use `limit: 80` for game files. Use REQUIREMENTS.md for format reference, not raw game files.
3. **Fail fast, never retry blindly.** Diagnose root cause before re-attempting. No background retries of the same failing command.
4. **Run targeted tests.** `gradle :backend:test --tests "*SpecificTest"` — never full suite unless asked.
5. **Delegate boilerplate to Haiku.** Model records, config files, repetitive code → `model: "haiku"` subagents.
6. **Compact between phases.** `/compact` with focus instructions when switching phases.
7. **Write directly, don't init-then-edit.** Use Write when target content is known.
8. **Parallelize only independent tasks.** Phase 2 extractors (2.1–2.5) can parallel. Sequential chains (1.1→1.2→1.3→1.4) stay in main thread.
