# Feature Specification: Stepik Course Editor Plugin

**Feature Branch**: `001-stepik-course-editor`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Develop a Stepik plugin for OpenIDE (IntelliJ IDEA fork) IDE that allows editing Stepik courses via .stepik files with split editor, live preview, and API sync"

## Clarifications

### Session 2026-05-06

- Q: How should the author edit structured step fields (quiz choices, matching pairs, points) in the left pane? → A: Form-based UI for structured fields (choices, correct answers, points) + HTML editor for content text.
- Q: Should "UPDATE IN STEPIK" push all changes at once or allow granular (per-lesson/per-step) push? → A: All changes at once — one button pushes every modified step across the course.
- Q: What should happen when remote course has changed since last fetch? → A: Warn the user, then on confirmation re-fetch all steps from remote (overwriting local), notify user that conflicting steps were fetched, and require the user to re-edit and click "Update all" again.
- Q: Should the plugin support creating new steps/lessons/sections or only editing existing content? → A: Edit-only for v1 — creation and deletion happen on Stepik's web UI.
- Q: Can the author manually refresh/pull course data from Stepik after initialization? → A: Yes — provide a "Refresh from Stepik" action that re-fetches the full course structure and updates local cache (discarding local edits).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Initialize a .stepik File with a Course Link (Priority: P1)

A course author creates a new file with the `.stepik` extension in their project. When the file is opened for the first time, the plugin detects it is uninitialized and prompts the author to provide a Stepik course URL. The plugin reads `STEPIK_CLIENT_ID` and `STEPIK_CLIENT_SECRET` from the project's `.env` file, authenticates with Stepik, fetches the course structure, and stores the course link and cached data into the `.stepik` file for future use.

**Why this priority**: Without initialization, no other functionality is possible. This is the entry point for all subsequent workflows.

**Independent Test**: Create a `.stepik` file, open it, provide a valid course URL, and verify the file is populated with the course link and cached structure.

**Acceptance Scenarios**:

1. **Given** a new empty `.stepik` file is opened, **When** the user provides a valid Stepik course URL, **Then** the plugin authenticates using `.env` credentials, fetches the course structure, and writes it into the `.stepik` file.
2. **Given** a new `.stepik` file is opened, **When** the `.env` file is missing or lacks `STEPIK_CLIENT_ID`/`STEPIK_CLIENT_SECRET`, **Then** the plugin displays a clear error message indicating the missing credentials.
3. **Given** a new `.stepik` file is opened, **When** the user provides an invalid or inaccessible course URL, **Then** the plugin displays an error indicating the course could not be fetched.
4. **Given** a new `.stepik` file is opened, **When** the user cancels the course link prompt, **Then** the file remains empty and can be initialized later.

---

### User Story 2 - Browse Course Structure in the Navigator (Priority: P1)

A course author opens an already-initialized `.stepik` file. The plugin reads the cached data and displays a hierarchical tree navigator showing all modules (sections), lessons, and steps. The author can expand/collapse levels and click on any step to open it.

**Why this priority**: Browsing course structure is the prerequisite for editing any content. Without navigation, the editor is useless.

**Independent Test**: Open an initialized `.stepik` file and verify the full course hierarchy (modules > lessons > steps) appears in a navigable tree view within the IDE.

**Acceptance Scenarios**:

1. **Given** an initialized `.stepik` file is opened, **When** the plugin loads, **Then** a tree navigator displays all modules, lessons, and steps in their correct order.
2. **Given** the navigator is displayed, **When** the author clicks on a step, **Then** the step content opens in the editor area.
3. **Given** the navigator is displayed, **When** the course has many modules/lessons, **Then** all levels are collapsible and expandable.
4. **Given** the navigator is displayed, **When** the author triggers "Refresh from Stepik", **Then** the plugin re-fetches the full course structure, updates the local cache (discarding local edits), and refreshes the navigator tree.

---

### User Story 3 - Edit Steps in a Split Editor with Live Preview (Priority: P1)

When a course author selects a step from the navigator, the editor splits into two panes. The left pane shows the step's source code (HTML, CSS, JavaScript) and metadata (step type, point value). The right pane renders a live preview of the step as it would appear to students on Stepik. Edits in the left pane are reflected in the right-pane preview in real time.

**Why this priority**: The split editor with live preview is the core value proposition — it enables course authors to see changes instantly without switching to a browser.

**Independent Test**: Open a step, modify its HTML in the left pane, and verify the right-pane preview updates immediately to reflect the change.

**Acceptance Scenarios**:

1. **Given** a step is selected in the navigator, **When** it opens, **Then** the editor splits into a left source pane and a right preview pane.
2. **Given** the split editor is open with a text-type step, **When** the left pane displays step source, **Then** it shows an HTML editor for the step's content.
3. **Given** the split editor is open with a structured step (quiz, matching, string), **When** the left pane displays the step, **Then** it shows a form-based UI for structured fields (choices, correct answers, matching pairs, points) above an HTML editor for the content text.
4. **Given** the split editor is open, **When** the author modifies content in the left pane, **Then** the right preview pane updates in real time to show the rendered result.
5. **Given** the split editor is showing a quiz-type step, **When** the author views the form fields, **Then** they include: step type, whether it is multi-turn, the number of points, answer choices, and correct answer indicators.

---

### User Story 4 - Save Changes Locally (Priority: P2)

The course author edits step content and saves the changes. The modified data is persisted into the `.stepik` file as local cache. No data is sent to Stepik at this point. The author can close and reopen the file later and see their unsaved-to-Stepik edits preserved.

**Why this priority**: Local save is a safety net — authors need confidence that their work won't be lost before pushing to Stepik.

**Independent Test**: Edit a step, save locally, close the file, reopen it, and verify the edits are preserved in the local cache.

**Acceptance Scenarios**:

1. **Given** a step has been edited, **When** the author saves (standard IDE save), **Then** changes are persisted to the `.stepik` file.
2. **Given** changes have been saved locally, **When** the file is closed and reopened, **Then** the locally modified content is displayed (not the original from Stepik).
3. **Given** changes have been saved locally but not pushed to Stepik, **When** the author views the step, **Then** a visual indicator shows that local changes differ from Stepik.

---

### User Story 5 - Push Changes to Stepik with Order Preservation (Priority: P2)

After editing locally, the course author clicks an "UPDATE IN STEPIK" button visible in the editor. The plugin sends all local changes to the Stepik API. Before updating, the plugin records the original step order within each affected lesson. After updates are sent, it checks whether step ordering has changed and restores the original order if needed.

**Why this priority**: This is the final step in the editing workflow — syncing local edits back to the live course. Order preservation is critical because the Stepik API may reorder steps as a side effect of updates.

**Independent Test**: Edit multiple steps across lessons, click "UPDATE IN STEPIK", and verify all changes appear on Stepik with the original step ordering preserved.

**Acceptance Scenarios**:

1. **Given** local changes exist, **When** the author clicks "UPDATE IN STEPIK", **Then** all modified steps are sent to the Stepik API.
2. **Given** the update is in progress, **When** the Stepik API reorders steps as a side effect, **Then** the plugin detects the reordering and restores the original step positions.
3. **Given** the update succeeds, **When** the sync is complete, **Then** the local cache is refreshed to match the remote state and the "UPDATE IN STEPIK" button is no longer highlighted.
4. **Given** the update fails for any step, **When** an API error occurs, **Then** the plugin reports which steps failed and preserves local changes so the author can retry.
5. **Given** the author clicks "UPDATE IN STEPIK", **When** the remote course has been modified since the last fetch, **Then** the plugin warns the user, re-fetches all steps from remote (overwriting local cache), reports which steps had conflicts, and requires the author to re-edit and push again.

---

### Edge Cases

- What happens when the Stepik API credentials expire mid-session? The plugin should re-authenticate transparently using `.env` credentials (client credentials flow).
- What happens when another user edits the same course on Stepik while the author has local changes? The plugin warns the author that remote changes were detected. On confirmation, the plugin re-fetches all steps from remote (overwriting local cache), notifies the user which steps had conflicts, and requires the author to re-edit and click "UPDATE IN STEPIK" again.
- What happens when a `.stepik` file references a course that has been deleted on Stepik? The plugin should display an error and offer to reinitialize the file.
- What happens when the `.stepik` file is corrupted or manually edited into an invalid state? The plugin should detect invalid data and offer to re-fetch from Stepik.
- What happens when the network is unavailable during "UPDATE IN STEPIK"? The plugin should report a connection error and preserve all local changes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST recognize `.stepik` as a custom file type and open it with the plugin's editor.
- **FR-002**: System MUST prompt for a Stepik course URL when an uninitialized `.stepik` file is opened.
- **FR-003**: System MUST read `STEPIK_CLIENT_ID` and `STEPIK_CLIENT_SECRET` from the project's `.env` file for authentication.
- **FR-004**: System MUST authenticate with Stepik using OAuth2 client credentials grant and cache the token for the session.
- **FR-005**: System MUST fetch and display the full course hierarchy: sections (modules) > lessons (via units) > steps.
- **FR-006**: System MUST display a tree navigator for browsing the course structure within the IDE.
- **FR-007**: System MUST open a split editor when a step is selected: source on the left, rendered preview on the right.
- **FR-008**: The left editor pane MUST display a form-based UI for structured step fields (choices, correct answers, matching pairs, point cost, multi-turn flag) and an HTML editor for the step's content text.
- **FR-009**: The right preview pane MUST render the step content using an embedded browser (JCEF/Chromium).
- **FR-010**: The preview pane MUST update in real time as the author edits the source in the left pane.
- **FR-011**: System MUST persist all local changes to the `.stepik` file when the author saves.
- **FR-012**: System MUST display an "UPDATE IN STEPIK" action in the editor that pushes all locally modified steps across the entire course in a single operation.
- **FR-013**: Before pushing updates, system MUST check whether remote steps have changed since the last fetch. If changes are detected, the system MUST warn the user, re-fetch all steps from remote (overwriting local cache), report which steps had conflicts, and require the user to re-edit and push again.
- **FR-014**: Before pushing updates, system MUST record the original step order for each affected lesson.
- **FR-015**: After pushing updates, system MUST compare the new step order with the original and restore ordering if it changed (using the position-aware update pattern from the Stepik MCP server).
- **FR-016**: System MUST handle all step types returned by the Stepik API (text, choice, matching, string, code, etc.) for display and editing.
- **FR-017**: System MUST provide a "Refresh from Stepik" action that re-fetches the full course structure from the remote, updates the local cache (discarding any unsaved local edits), and refreshes the navigator and editor.

### Key Entities

- **Course**: Top-level entity identified by a Stepik course URL/ID. Contains sections.
- **Section (Module)**: A group of lessons within a course, ordered by position.
- **Lesson**: A group of steps within a section, linked via units. Titles limited to 64 characters on Stepik.
- **Unit**: A join entity connecting a lesson to a section at a specific position.
- **Step**: The atomic content unit — text, quiz, matching, string input, code, etc. Has a type, HTML content, optional point cost, and position within a lesson.
- **StepikFile (.stepik)**: Local file that stores the course URL, cached course structure, and locally modified step content.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Course authors can initialize a `.stepik` file and see the full course structure within 10 seconds of providing a course URL.
- **SC-002**: Authors can navigate from file open to editing a specific step in 3 clicks or fewer (open file > expand tree > click step).
- **SC-003**: Preview updates are visible within 1 second of editing source content.
- **SC-004**: Authors can complete a full edit-save-push cycle (edit a step, save locally, push to Stepik) in under 2 minutes.
- **SC-005**: Step ordering is preserved after pushing updates to Stepik in 100% of cases.
- **SC-006**: All step types present in the test course (https://stepik.org/course/286390/syllabus) can be viewed and edited through the plugin.

## Assumptions

- The plugin targets OpenIDE (IntelliJ IDEA fork) which supports the IntelliJ Platform Plugin SDK and JCEF (embedded Chromium).
- The `.env` file is located in the project root directory where the `.stepik` file resides.
- The Stepik API uses OAuth2 client credentials grant (not user login), meaning the plugin operates under the API client's permissions.
- The `.stepik` file format uses a structured data format (e.g., JSON) for storing course link, cached structure, and local edits.
- Course hierarchy follows the Stepik model: Course > Sections > Units > Lessons > Steps.
- The embedded browser (JCEF) is available in the target OpenIDE distribution.
- Only one course is linked per `.stepik` file.
- The plugin is edit-only for v1: creating, deleting, or reordering steps/lessons/sections is out of scope and done via Stepik's web UI.
- The test course at https://stepik.org/course/286390/syllabus is used for validation.
