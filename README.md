# Stepik Course Editor

An IntelliJ Platform plugin for editing [Stepik](https://stepik.org) courses directly inside the IDE. Browse the course hierarchy, edit step content with a live HTML preview, manage quiz settings, and push changes back to Stepik -- all without leaving your editor.

## Features

- **`.stepik` file format** -- one JSON file per course that stores the full course hierarchy locally, including both the last-known remote state and your local edits.
- **Course navigator** -- a tree-based tool window (Sections > Lessons > Steps) that appears in the left panel. Click any step to open it in the editor. Dirty steps are marked with `*`.
- **Split editor with live preview** -- the left pane shows an HTML code editor (with syntax highlighting and line numbers); the right pane renders the HTML in an embedded Chromium browser (JCEF). Changes appear in the preview instantly.
- **Form-based editing for quiz types** -- structured UI for Choice, Matching, and String step types, with collapsible settings panels so you can maximize code space when needed.
- **Local save** -- edits are persisted to the `.stepik` file on Ctrl+S. Reopen the file later and your changes are still there.
- **Batch push to Stepik** -- the "Update in Stepik" button pushes all local changes to the Stepik API in one operation, with automatic step-order preservation and conflict detection.
- **Conflict detection** -- before pushing, the plugin compares your stored remote snapshot with the current API state. If someone edited the same steps on Stepik, you get a warning and a fresh copy of the course.
- **Step order preservation** -- Stepik's API can reorder steps when you update them. The plugin records step positions before pushing, then restores the original order afterward.
- **Transparent auth** -- OAuth2 client credentials with automatic token refresh on 401.

## Requirements

- IntelliJ IDEA 2024.3+ (Community or Ultimate), or any compatible JetBrains IDE / OpenIDE fork
- JDK 21 (JetBrains Runtime recommended)
- Gradle 8.14+
- A Stepik API application (client ID + client secret) -- create one at https://stepik.org/oauth2/applications/

## Getting Started

### 1. Configure Stepik credentials

Create a `.env` file in your project root:

```
STEPIK_CLIENT_ID=your_client_id_here
STEPIK_CLIENT_SECRET=your_client_secret_here
```

You can obtain these by registering an OAuth2 application at https://stepik.org/oauth2/applications/ (choose "Confidential" client type, "client_credentials" grant type).

### 2. Build and install the plugin

```bash
# Build the plugin
./gradlew buildPlugin

# The distributable ZIP is at:
# build/distributions/stepik-plugin-1.0-SNAPSHOT.zip
```

Install in your IDE: **Settings > Plugins > Gear icon > Install Plugin from Disk...** and select the ZIP.

Alternatively, for development:

```bash
# Launch a sandbox IDE with the plugin pre-installed
./gradlew runIde
```

### 3. Initialize a course

1. Create a new file with the `.stepik` extension (e.g., `my-course.stepik`)
2. Open it -- the plugin shows an initialization panel
3. Enter the course URL (e.g., `https://stepik.org/course/286390/syllabus`)
4. Click **Fetch Course** -- the plugin downloads the full course structure via the Stepik API

### 4. Edit steps

1. Open the **Stepik Navigator** tool window (left sidebar, or via **View > Tool Windows > Stepik Navigator**)
2. Expand the tree: Sections > Lessons > Steps
3. Click a step to open it in the split editor
4. Edit the HTML in the left pane -- the preview updates in real time
5. For quiz steps (choice, matching, string), use the form fields above the HTML editor to configure options, answers, and point values. Click **Hide Settings** to collapse the form and get more code space.
6. Press **Ctrl+S** to save locally

### 5. Push changes to Stepik

1. After editing, click the **Update in Stepik** button in the editor toolbar
2. The plugin will:
   - Check for conflicts (remote changes since your last fetch)
   - Record the current step order in each affected lesson
   - Push all modified steps to the Stepik API
   - Restore the original step order if it was disrupted
3. If conflicts are detected, the plugin fetches a fresh copy of the course and asks you to re-edit

### 6. Refresh from Stepik

Click the **Refresh from Stepik** button in the navigator toolbar to re-fetch the entire course from the API. This overwrites all local changes.

## Supported Step Types

| Type | Editor | Fields |
|------|--------|--------|
| **Text** | HTML editor only | -- |
| **Choice** (quiz) | Form + HTML editor | Cost, multiple choice, preserve order, options table (text, correct, feedback), correct/wrong feedback |
| **Matching** | Form + HTML editor | Cost, preserve left-column order, pairs table (first, second) |
| **String** | Form + HTML editor | Cost, pattern, regex, match substring, case sensitive |
| **Other** (code, number, etc.) | Falls back to HTML editor | -- |

## Project Structure

```
src/main/kotlin/org/example/stepik/
+-- StepikFileType.kt              # .stepik file type registration + icon
+-- model/
|   +-- StepContent.kt             # Step content (text, source JSON, cost)
|   +-- StepData.kt                # Step entity with remote/local snapshots
|   +-- LessonData.kt              # Lesson entity
|   +-- SectionData.kt             # Section entity
|   +-- StepikFileData.kt          # Root model for .stepik JSON file
+-- api/
|   +-- EnvReader.kt               # Reads .env file for credentials
|   +-- StepikAuth.kt              # OAuth2 client credentials + token cache
|   +-- StepikApiClient.kt         # Stepik REST API client
+-- editor/
|   +-- StepikEditorProvider.kt    # FileEditorProvider for .stepik files
|   +-- StepikSplitEditor.kt       # Main split editor (source + preview)
|   +-- StepSourcePanel.kt         # Selects the right form by step type
|   +-- StepPreviewPanel.kt        # JCEF browser wrapper for live preview
|   +-- forms/
|       +-- StepForm.kt            # Interface for step editing forms
|       +-- TextStepForm.kt        # HTML-only editor
|       +-- ChoiceStepForm.kt      # Multiple choice quiz form
|       +-- MatchingStepForm.kt    # Matching pairs quiz form
|       +-- StringStepForm.kt      # String input quiz form
+-- navigator/
|   +-- CourseTreeModel.kt         # Builds tree model from StepikFileData
|   +-- StepikToolWindowFactory.kt # Tool window with course tree + refresh
+-- service/
|   +-- StepikProjectService.kt    # Loads/saves .stepik files, in-memory cache
|   +-- StepikSaveListener.kt      # Auto-saves on IDE save (Ctrl+S)
+-- sync/
    +-- OrderPreserver.kt           # Records and restores step positions
    +-- ConflictDetector.kt         # Compares local snapshot vs remote state
    +-- StepikSyncService.kt        # Orchestrates the full push workflow
```

## How the `.stepik` File Works

The `.stepik` file is a JSON document containing the full course hierarchy:

```json
{
  "courseUrl": "https://stepik.org/course/286390/syllabus",
  "courseId": 286390,
  "courseTitle": "Test Course",
  "lastFetched": "2025-05-07T12:00:00Z",
  "sections": [
    {
      "id": 123, "title": "Section 1", "position": 1,
      "lessons": [
        {
          "id": 456, "unitId": 789, "title": "Lesson 1", "position": 1,
          "steps": [
            {
              "id": 1001, "position": 1, "type": "text", "cost": 0,
              "remote": { "text": "<p>Hello world</p>", "source": null, "cost": 0 },
              "local": null
            }
          ]
        }
      ]
    }
  ]
}
```

Key concepts:

- **`remote`** -- the last-known state from the Stepik server (set on fetch/push)
- **`local`** -- your edits (null if the step hasn't been modified)
- A step is **dirty** when `local != null`
- On push, `local` content replaces `remote` and `local` is cleared

## Build Commands

| Command | Description |
|---------|-------------|
| `./gradlew build` | Compile + run tests |
| `./gradlew test` | Run unit tests only |
| `./gradlew runIde` | Launch sandbox IDE with the plugin |
| `./gradlew buildPlugin` | Build distributable plugin ZIP |
| `./gradlew verifyPlugin` | Run IntelliJ plugin verifier |

## Tech Stack

- **Kotlin 2.2** with JVM toolchain 21 (JetBrains Runtime)
- **Gradle 8.14** with Kotlin DSL
- **IntelliJ Platform SDK 2024.3** via `org.jetbrains.intellij.platform` plugin v2.6.0
- **kotlinx.serialization 1.8.1** for JSON serialization
- **JCEF** (JBCefBrowser) for embedded Chromium preview
- **HttpRequests** (IntelliJ built-in) for REST API calls

## Troubleshooting

**"Missing STEPIK_CLIENT_ID / STEPIK_CLIENT_SECRET in .env"**
Create a `.env` file in the project root with your Stepik API credentials. See the [Getting Started](#1-configure-stepik-credentials) section.

**JCEF preview shows "JCEF is not available"**
Your IDE build doesn't include JCEF. Use a JetBrains Runtime that ships with JCEF, or install the JCEF plugin if available.

**"Conflicts Detected" when pushing**
Someone edited the same steps on Stepik since your last fetch. The plugin automatically re-fetches the course. Review the changes and push again.

**Step order changed after push**
This shouldn't happen -- the plugin records and restores step order automatically. If it does, use **Refresh from Stepik** and report the issue.

## License

This project is part of the ai-sec-lab workspace.
