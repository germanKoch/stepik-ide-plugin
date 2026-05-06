# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Stepik Course Editor — an IntelliJ Platform plugin that lets users edit Stepik courses directly in the IDE. Supports .stepik files, split editor with JCEF preview, form-based editing for quiz types, and batch push to Stepik API with step order preservation.

## Build & Test Commands

```bash
./gradlew build          # compile + test
./gradlew test           # run all tests (JUnit 5 via useJUnitPlatform())
./gradlew runIde         # launch sandbox IDE with the plugin installed
./gradlew buildPlugin    # build distributable plugin ZIP
./gradlew verifyPlugin   # run IntelliJ plugin verifier
```

## Tech Stack

- **Language:** Kotlin 2.2, JVM toolchain 21 (JetBrains Runtime)
- **Build:** Gradle 8.14 (Kotlin DSL), `org.jetbrains.intellij.platform` plugin v2.6.0
- **Platform:** IntelliJ Platform SDK 2024.3 (Community)
- **Serialization:** kotlinx.serialization 1.8.1
- **Preview:** JCEF (JBCefBrowser, bundled)
- **HTTP:** com.intellij.util.io.HttpRequests (bundled)
- **Testing:** kotlin-test with JUnit Platform, JUnit 4 (required by IntelliJ test framework)

## Project Structure

```
src/main/kotlin/org/example/stepik/
├── StepikFileType.kt          # .stepik file type registration
├── model/                     # Data classes (StepikFileData, StepData, etc.)
├── api/                       # EnvReader, StepikAuth (OAuth2), StepikApiClient
├── editor/                    # StepikEditorProvider, StepikSplitEditor
│   └── forms/                 # StepForm interface + TextStepForm, ChoiceStepForm, etc.
├── navigator/                 # CourseTreeModel, StepikToolWindowFactory
├── service/                   # StepikProjectService, StepikSaveListener
└── sync/                      # OrderPreserver, ConflictDetector, StepikSyncService
```

## Key Concepts

- `.stepik` files are JSON, one per course, containing the full course hierarchy with remote/local snapshots per step
- `StepData.remote` = last known server state; `StepData.local` = user's edits (null if unchanged)
- Push flow: detect conflicts -> record step order -> push changes -> restore order
- Step order preservation follows the same protocol as the Python MCP server at `/Users/germankochnev/Desktop/projects/utils/mcp/stepik-api-mcp/stepik_mcp_server.py`
