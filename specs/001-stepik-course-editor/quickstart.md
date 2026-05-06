# Quickstart: Stepik Course Editor Plugin

**Date**: 2026-05-06  
**Branch**: `001-stepik-course-editor`

## Prerequisites

- JDK 21 (JetBrains Runtime recommended)
- IntelliJ IDEA 2024.3+ or OpenIDE (for running/debugging the plugin)
- Stepik API credentials (`STEPIK_CLIENT_ID`, `STEPIK_CLIENT_SECRET`)

## Setup

1. Clone the repository and switch to the feature branch:
   ```bash
   git checkout 001-stepik-course-editor
   ```

2. The `build.gradle.kts` uses `org.jetbrains.intellij.platform` plugin. Gradle will download the IntelliJ Platform SDK on first build.

3. Build the plugin:
   ```bash
   ./gradlew build
   ```

4. Run the plugin in a sandboxed IDE instance:
   ```bash
   ./gradlew runIde
   ```

5. In the sandboxed IDE, create a `.env` file in the project root with:
   ```
   STEPIK_CLIENT_ID=your_client_id
   STEPIK_CLIENT_SECRET=your_client_secret
   ```

6. Create a file with `.stepik` extension and open it to begin.

## Test Course

Use https://stepik.org/course/286390/syllabus for validation.

## Build Commands

```bash
./gradlew build           # compile + test
./gradlew test            # run all tests
./gradlew runIde          # launch sandboxed IDE with plugin installed
./gradlew buildPlugin     # package plugin as .zip for distribution
./gradlew verifyPlugin    # validate plugin descriptor and compatibility
```

## Project Structure

```
src/main/kotlin/org/example/stepik/
├── StepikFileType.kt              # .stepik file type registration
├── model/                          # Data classes for .stepik JSON structure
│   ├── StepikFileData.kt
│   ├── SectionData.kt
│   ├── LessonData.kt
│   └── StepData.kt
├── api/                            # Stepik REST API client
│   ├── StepikApiClient.kt
│   ├── StepikAuth.kt
│   └── EnvReader.kt
├── editor/                         # Split editor components
│   ├── StepikEditorProvider.kt
│   ├── StepikSplitEditor.kt
│   ├── StepSourcePanel.kt         # Left pane: form + HTML editor
│   ├── StepPreviewPanel.kt        # Right pane: JCEF browser
│   └── forms/                      # Step-type-specific form UIs
│       ├── TextStepForm.kt
│       ├── ChoiceStepForm.kt
│       ├── MatchingStepForm.kt
│       └── StringStepForm.kt
├── navigator/                      # Course tree navigator
│   ├── StepikToolWindowFactory.kt
│   └── CourseTreeModel.kt
├── sync/                           # Push/pull/conflict logic
│   ├── StepikSyncService.kt
│   ├── ConflictDetector.kt
│   └── OrderPreserver.kt
└── service/                        # Project-level services
    └── StepikProjectService.kt

src/main/resources/META-INF/
└── plugin.xml                      # Plugin descriptor with extension points

src/test/kotlin/org/example/stepik/
├── api/
│   └── StepikApiClientTest.kt
├── model/
│   └── StepikFileDataTest.kt
├── sync/
│   ├── ConflictDetectorTest.kt
│   └── OrderPreserverTest.kt
└── editor/
    └── StepSourcePanelTest.kt
```
