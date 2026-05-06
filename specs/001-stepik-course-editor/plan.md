# Implementation Plan: Stepik Course Editor Plugin

**Branch**: `001-stepik-course-editor` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-stepik-course-editor/spec.md`

## Summary

Build an IntelliJ Platform plugin for OpenIDE that enables course authors to edit Stepik courses through `.stepik` files. The plugin provides a tree navigator for course structure, a split editor (form-based UI + HTML editor on the left, JCEF live preview on the right), local persistence, and batch push to Stepik with step-order preservation. Edit-only for v1 — no creation or deletion of course elements.

## Technical Context

**Language/Version**: Kotlin 2.2, JVM toolchain 21 (JetBrains Runtime)
**Primary Dependencies**: IntelliJ Platform SDK 2024.3 (`org.jetbrains.intellij.platform` Gradle plugin 2.16.0), JCEF (bundled), `com.intellij.util.io.HttpRequests` (bundled), `kotlinx.serialization` for JSON
**Storage**: `.stepik` JSON file (project-local, one file per course)
**Testing**: JUnit 5 via `kotlin-test`, IntelliJ test framework (`BasePlatformTestCase`) for integration tests
**Target Platform**: OpenIDE / IntelliJ IDEA 2024.3+ (JBR 21)
**Project Type**: IntelliJ Platform Plugin (desktop-app extension)
**Performance Goals**: Course load <10s, preview update <1s, push cycle <2min (per spec SC-001..SC-004)
**Constraints**: JCEF must be available; plugin runs in EDT — heavy operations (API calls, file I/O) must use background threads
**Scale/Scope**: Single user editing one course at a time; typical course: ~10 sections, ~50 lessons, ~200 steps

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution is an unfilled template — no gates configured. Proceeding without constraints.

Post-Phase 1 re-check: No constitution violations. Design follows single-project structure with clear separation of concerns.

## Project Structure

### Documentation (this feature)

```text
specs/001-stepik-course-editor/
├── plan.md              # This file
├── research.md          # Phase 0: technology research decisions
├── data-model.md        # Phase 1: .stepik file schema and entity model
├── quickstart.md        # Phase 1: developer setup guide
├── contracts/
│   └── stepik-api.md    # Phase 1: Stepik REST API contract
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/kotlin/org/example/stepik/
├── StepikFileType.kt                  # FileType registration for .stepik
├── model/                              # Data classes (kotlinx.serialization)
│   ├── StepikFileData.kt              # Root model for .stepik JSON
│   ├── SectionData.kt
│   ├── LessonData.kt
│   ├── StepData.kt
│   └── StepContent.kt                 # Step content + type-specific source
├── api/                                # Stepik REST API client
│   ├── StepikApiClient.kt             # HTTP calls, JSON parsing
│   ├── StepikAuth.kt                  # OAuth2 client credentials token cache
│   └── EnvReader.kt                   # .env file parser
├── editor/                             # Split editor components
│   ├── StepikEditorProvider.kt        # FileEditorProvider for .stepik files
│   ├── StepikSplitEditor.kt           # FileEditor with JBSplitter
│   ├── StepSourcePanel.kt             # Left pane: form + HTML editor
│   ├── StepPreviewPanel.kt            # Right pane: JBCefBrowser
│   └── forms/                          # Step-type-specific form panels
│       ├── TextStepForm.kt            # Text step: HTML editor only
│       ├── ChoiceStepForm.kt          # Quiz: options table, correct checkboxes, feedbacks
│       ├── MatchingStepForm.kt        # Matching: pairs table
│       └── StringStepForm.kt          # String input: pattern, flags
├── navigator/                          # Course tree tool window
│   ├── StepikToolWindowFactory.kt     # ToolWindowFactory registration
│   └── CourseTreeModel.kt             # Tree model from StepikFileData
├── sync/                               # Push/pull/conflict logic
│   ├── StepikSyncService.kt           # Orchestrates push, pull, refresh
│   ├── ConflictDetector.kt            # Compares remote snapshots vs API
│   └── OrderPreserver.kt              # Step reordering after push
└── service/                            # Project-level services
    └── StepikProjectService.kt        # Manages loaded .stepik files, coordinates components

src/main/resources/
├── META-INF/
│   └── plugin.xml                      # Plugin descriptor: fileType, fileEditorProvider, toolWindow
└── icons/
    └── stepik.svg                      # Plugin icon for tool window

src/test/kotlin/org/example/stepik/
├── model/
│   └── StepikFileDataTest.kt          # Serialization/deserialization, validation
├── api/
│   ├── StepikAuthTest.kt              # Token caching, refresh
│   ├── StepikApiClientTest.kt         # API response parsing
│   └── EnvReaderTest.kt               # .env parsing edge cases
├── sync/
│   ├── ConflictDetectorTest.kt        # Conflict detection logic
│   └── OrderPreserverTest.kt          # Order comparison and restoration
└── editor/
    └── StepSourcePanelTest.kt          # Form ↔ model binding
```

**Structure Decision**: Single IntelliJ plugin project. Standard `src/main/kotlin` + `src/test/kotlin` layout following IntelliJ plugin conventions. Package root: `com.stellarflux.stepik`. Six sub-packages by responsibility: `model`, `api`, `editor`, `navigator`, `sync`, `service`.

## Complexity Tracking

No constitution violations to justify.
