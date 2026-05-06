# Research: Stepik Course Editor Plugin

**Date**: 2026-05-06  
**Branch**: `001-stepik-course-editor`

## R-001: IntelliJ Platform Plugin SDK Setup

**Decision**: Use `org.jetbrains.intellij.platform` Gradle plugin v2.16.0 with Kotlin 2.2, targeting IntelliJ Platform 2024.3.

**Rationale**: The 2.x Gradle plugin is the actively maintained successor to the 1.x plugin. Platform 2024.3 provides stable JCEF support and Kotlin 2.x compatibility. JVM toolchain must be lowered from 24 to 21 (JetBrains Runtime is JBR 21).

**Alternatives considered**:
- Old `org.jetbrains.intellij` 1.x plugin — deprecated, no longer actively developed.
- Targeting 2025.1+ — would restrict user base unnecessarily; 2024.3 covers the needed features.

## R-002: Custom File Type and Editor Registration

**Decision**: Register `.stepik` via `com.intellij.fileType` extension point with a `FileType` implementation. Use `com.intellij.fileEditorProvider` with `AsyncFileEditorProvider` to create the custom editor.

**Rationale**: Standard IntelliJ extension points for file type recognition and custom editor binding. `AsyncFileEditorProvider` allows non-blocking editor creation (useful since we may parse/validate the `.stepik` JSON on open).

**Alternatives considered**:
- `LanguageFileType` — unnecessary since `.stepik` is a data file, not a programming language.
- Default text editor with a panel overlay — wouldn't provide the split editor UX required.

## R-003: Split Editor Architecture

**Decision**: Implement a custom `FileEditor` using `JBSplitter` with two panels. Left panel: custom Swing form (metadata fields) + `EditorTextField` (HTML editing with syntax highlighting). Right panel: `JBCefBrowser` for rendered preview.

**Rationale**: `TextEditorWithPreview` assumes a standard text editor on the left, but we need a form-based UI for structured step fields above the HTML editor. A custom `FileEditor` with `JBSplitter` gives full control over both panes.

**Alternatives considered**:
- `TextEditorWithPreview` — too restrictive; doesn't support a form above the editor.
- Raw `JSplitPane` — would work but `JBSplitter` integrates better with IntelliJ L&F and provides persisted splitter position.

## R-004: JCEF Browser for Live Preview

**Decision**: Use `JBCefBrowser` for the right-side preview pane. Load step HTML content directly via `loadHTML()`. Guard with `JBCefApp.isSupported()`.

**Rationale**: JCEF is the official embedded Chromium in IntelliJ Platform. `JBCefBrowser.loadHTML()` accepts raw HTML strings, ideal for rendering step content. The `JBCefClient` provides JS-to-Java bridge for future interactivity.

**Alternatives considered**:
- JavaFX WebView — deprecated in IntelliJ Platform, not bundled.
- External browser preview — violates the "rendered in IDE" requirement.

## R-005: Course Navigator (Tool Window with Tree)

**Decision**: Register a `com.intellij.toolWindow` with a `ToolWindowFactory` implementation. Use `com.intellij.ui.treeStructure.Tree` with `DefaultTreeModel` for the course hierarchy.

**Rationale**: Standard IntelliJ approach for tool windows. `Tree` (JBTree) provides consistent IDE look and feel. `DefaultTreeModel` with `DefaultMutableTreeNode` is sufficient for the 3-level hierarchy (Section > Lesson > Step).

**Alternatives considered**:
- `AbstractTreeStructure` + `AsyncTreeModel` — overkill for a static tree loaded from cached data.
- Custom `JPanel` with nested lists — non-standard, poor UX.

## R-006: Stepik API Client

**Decision**: Use `com.intellij.util.io.HttpRequests` for REST API calls. Implement OAuth2 client credentials grant manually (simple token endpoint call).

**Rationale**: Built into the platform, zero extra dependencies, respects IDE proxy settings. The Stepik API is straightforward REST with JSON — no WebSocket or streaming needed.

**Alternatives considered**:
- ktor-client — adds dependency weight for no benefit given the simple API surface.
- OkHttp — similar concern; unnecessary given `HttpRequests` sufficiency.

## R-007: .env File Reading

**Decision**: Read `.env` manually from `project.basePath` using basic line parsing (`KEY=VALUE` format). No external library.

**Rationale**: The `.env` file only needs two variables (`STEPIK_CLIENT_ID`, `STEPIK_CLIENT_SECRET`). A 10-line parser is simpler than adding a dependency. Use `LocalFileSystem` to resolve the file path relative to the project root.

**Alternatives considered**:
- `io.github.cdimascio:dotenv-kotlin` library — adds a dependency for trivial functionality.
- `java.util.Properties` — works but `.env` files aren't true `.properties` files (no escaping rules).

## R-008: .stepik File Format

**Decision**: Use JSON format for the `.stepik` file. Structure: top-level object with `courseUrl`, `courseId`, `lastFetched` timestamp, and nested `sections` > `lessons` > `steps` arrays mirroring the Stepik hierarchy. Each step stores both `remote` (last-fetched state) and `local` (edited state, null if unchanged) snapshots.

**Rationale**: JSON is natively supported in IntelliJ (syntax highlighting, validation). Dual remote/local snapshots enable dirty-state detection and conflict checking. The `lastFetched` timestamp enables conflict detection against remote.

**Alternatives considered**:
- YAML — harder to parse without additional library in Kotlin/JVM.
- SQLite — too heavy for a single-file cache; not human-readable for debugging.
- Separate files per step — complicates the single-file requirement from the spec.

## R-009: Order Preservation Strategy

**Decision**: Before pushing updates, for each affected lesson: (1) fetch current step positions via `GET /api/steps?lesson={id}`, (2) record `[(step_id, position)]` list, (3) push updates, (4) re-fetch positions, (5) if order changed, restore original using `PUT /api/step-sources/{id}` with the original position and block data. Identical to the pattern in `stepik_mcp_server.py` (`_get_step_positions` / `_restore_step_order`).

**Rationale**: This is a proven pattern already validated in the MCP server. The Stepik API sometimes reorders steps when a step-source is updated — this pattern detects and corrects it.

**Alternatives considered**:
- Preemptive position pinning — not supported by the API.
- Ignoring reordering — violates SC-005 (100% order preservation).

## R-010: Conflict Detection Strategy

**Decision**: Before pushing, for each modified step, fetch the current `step-source` from the API and compare the `block` content with the `remote` snapshot stored in the `.stepik` file. If any field differs, the remote has been modified since last fetch.

**Rationale**: Comparing the stored `remote` snapshot against the current API state detects third-party edits without requiring a version field (which the Stepik API doesn't provide). Per-step comparison catches granular changes.

**Alternatives considered**:
- Timestamp-based comparison — Stepik API doesn't expose reliable last-modified timestamps on steps.
- Hash-based comparison — equivalent to field comparison but adds hashing complexity.
