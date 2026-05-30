# Phase 71: TypeScript Type Generation from Java DTOs

**Status:** DONE
**Session:** 52

## Tasks

1. [x] Create `dto/VersionResponse.java` (moved from DataController inner record)
2. [x] Create `dto/DlcInfo.java` (moved from SettingsController inner record DlcInfoDto, renamed to match TypeScript)
3. [x] Create `dto/SettingsResponse.java` (moved from SettingsController, with @Nullable on disabledDlcs)
4. [x] Add `@Nullable` to nullable String/reference fields across all 10 DTOs
5. [x] Add `@Nullable` to optional String fields in ExportRequest
6. [x] Update DataController: use dto.VersionResponse, inline the from() factory
7. [x] Update SettingsController: use dto.SettingsResponse and dto.DlcInfo
8. [x] Add `generateTypeScript` Gradle task to build.gradle.kts (local functions in doLast)
9. [x] Wire `bootJar.dependsOn("generateTypeScript")` so it runs during production build
10. [x] Run task: generates frontend/src/types/empire-generated.ts
11. [x] Update empire.ts: re-export from empire-generated.ts + manual RerollCategory
12. [x] Update ExportModal.tsx: conditional spread -> `field || null` (matches @Nullable Java)
13. [x] Verify: gradle compileJava + tsc --noEmit + knip all clean

## Key Decisions

- **Custom Gradle task over typescript-generator plugin**: Avoided third-party plugin with uncertain
  Java 25 / Spring Boot 4.0 / Jackson 3.x compatibility. Custom task reads source files directly,
  no compilation dependency needed.
- **Local functions in doLast**: Kotlin DSL top-level function calls from task lambdas can have
  scoping issues; local function declarations inside doLast are safe and self-contained.
- **@Nullable for nullability source-of-truth**: Instead of a hardcoded map or "all strings nullable"
  heuristic, `org.springframework.lang.@Nullable` on record components is the explicit contract.
  Generator checks for `@Nullable` string in the raw component text.
- **Alphabetical file ordering**: TypeScript interfaces can reference types defined later in the same
  file; alphabetical sort is stable and readable.
- **ExportRequest stays in generated file**: Changed from `speciesPlural?: string` (optional) to
  `speciesPlural: string | null` to match the @Nullable Java annotation. ExportModal.tsx updated
  to `value || null` pattern. Semantically equivalent -- absent vs null are both null in Spring.
- **DlcInfo (no Dto suffix)**: Named to match existing TypeScript usage in SettingsPage.tsx.
- **RerollCategory stays manual**: It is a TypeScript-only narrowing of the Java enum -- the enum
  values use UPPER_CASE but the API accepts lowercase strings. Not worth generating.
- **empire-generated.ts committed**: Committed so frontend developers can run tsc without
  running the Gradle build first. Regenerated automatically on bootJar.
