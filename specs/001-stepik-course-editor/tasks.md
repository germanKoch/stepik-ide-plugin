# Tasks: Stepik Course Editor Plugin

**Input**: Design documents from `specs/001-stepik-course-editor/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/stepik-api.md, research.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Convert the scaffolded Kotlin project into an IntelliJ Platform plugin project with all dependencies configured.

- [x] T001 Reconfigure `build.gradle.kts` — replace `kotlin("jvm")` with `org.jetbrains.intellij.platform` v2.16.0, add `kotlinx.serialization` plugin, set JVM toolchain to 21, add IntelliJ Platform SDK 2024.3 dependency
- [x] T002 Update `settings.gradle.kts` — remove foojay resolver (not needed for IntelliJ plugin), configure plugin repositories
- [x] T003 Create plugin descriptor at `src/main/resources/META-INF/plugin.xml` with plugin ID, name, vendor, and description (extension points added in later tasks)
- [x] T004 [P] Create package directory structure under `src/main/kotlin/org/example/stepik/` with sub-packages: `model`, `api`, `editor`, `editor/forms`, `navigator`, `sync`, `service`
- [x] T005 [P] Create test package directory structure under `src/test/kotlin/org/example/stepik/` with sub-packages: `model`, `api`, `sync`, `editor`
- [x] T006 [P] Add plugin icon at `src/main/resources/icons/stepik.svg`
- [x] T007 Verify build compiles successfully with `./gradlew build`

**Checkpoint**: Plugin project builds and can be launched with `./gradlew runIde` (empty plugin, no functionality yet)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data model, API client, and file type registration that ALL user stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T008 [P] Implement data model classes with kotlinx.serialization in `src/main/kotlin/org/example/stepik/model/StepContent.kt` — `StepContent` data class with `text: String` and `source: JsonObject?` fields
- [x] T009 [P] Implement `StepData` in `src/main/kotlin/org/example/stepik/model/StepData.kt` — fields: `id`, `position`, `type`, `cost`, `remote: StepContent`, `local: StepContent?`
- [x] T010 [P] Implement `LessonData` in `src/main/kotlin/org/example/stepik/model/LessonData.kt` — fields: `id`, `unitId`, `title`, `position`, `steps: List<StepData>`
- [x] T011 [P] Implement `SectionData` in `src/main/kotlin/org/example/stepik/model/SectionData.kt` — fields: `id`, `title`, `position`, `lessons: List<LessonData>`
- [x] T012 [P] Implement `StepikFileData` in `src/main/kotlin/org/example/stepik/model/StepikFileData.kt` — fields: `courseUrl`, `courseId`, `courseTitle`, `lastFetched`, `sections: List<SectionData>`; add `isDirty` computed property checking any step has non-null `local`
- [x] T013 [P] Implement `EnvReader` in `src/main/kotlin/org/example/stepik/api/EnvReader.kt` — reads `.env` file from project root via `project.basePath`, parses `KEY=VALUE` lines, returns `Map<String, String>`; handles missing file, blank lines, comments
- [x] T014 [P] Implement `StepikAuth` in `src/main/kotlin/org/example/stepik/api/StepikAuth.kt` — OAuth2 client credentials grant to `https://stepik.org/oauth2/token/`, caches token with expiry, auto-refreshes on expiry (per contract)
- [x] T015 Implement `StepikApiClient` in `src/main/kotlin/org/example/stepik/api/StepikApiClient.kt` — uses `HttpRequests` and `StepikAuth`; methods: `getCourse(courseId)`, `getSections(sectionIds)`, `getUnits(unitIds)`, `getLessons(lessonIds)`, `getSteps(lessonId)`, `getStepSource(stepId)`, `updateStepSource(stepId, block, position?)` per API contract; all calls on background thread via `ApplicationManager.getApplication().executeOnPooledThread`
- [x] T016 Implement `StepikFileType` in `src/main/kotlin/org/example/stepik/StepikFileType.kt` — register `.stepik` as custom file type; add `<fileType>` extension in `plugin.xml`
- [x] T017 Implement `StepikProjectService` in `src/main/kotlin/org/example/stepik/service/StepikProjectService.kt` — project-level service that loads/saves `StepikFileData` from/to `.stepik` file using kotlinx.serialization JSON; provides `loadFile(VirtualFile): StepikFileData?` and `saveFile(VirtualFile, StepikFileData)`; register as `<projectService>` in `plugin.xml`
- [x] T018 Write unit test for model serialization roundtrip in `src/test/kotlin/org/example/stepik/model/StepikFileDataTest.kt` — serialize `StepikFileData` to JSON and deserialize back, verify all fields preserved including nested steps with both `remote` and `local` content
- [x] T019 [P] Write unit test for `EnvReader` in `src/test/kotlin/org/example/stepik/api/EnvReaderTest.kt` — test valid parsing, missing file, comments, blank lines, malformed lines

**Checkpoint**: Foundation ready — data model, API client, file type, and project service all functional. User story implementation can begin.

---

## Phase 3: User Story 1 — Initialize a .stepik File with a Course Link (Priority: P1) MVP

**Goal**: User creates a `.stepik` file, opens it, provides a course URL, and the plugin fetches the course structure and persists it.

**Independent Test**: Create a `.stepik` file in the sandboxed IDE, open it, enter `https://stepik.org/course/286390/syllabus`, verify the file is populated with course structure JSON.

### Implementation for User Story 1

- [x] T020 [US1] Implement course-fetching logic in `StepikApiClient` — add `fetchFullCourse(courseId): StepikFileData` method that fetches course → sections → units → lessons → steps → step-sources, assembles into `StepikFileData` with all `remote` snapshots populated and `local` fields null
- [x] T021 [US1] Implement `StepikEditorProvider` in `src/main/kotlin/org/example/stepik/editor/StepikEditorProvider.kt` — `AsyncFileEditorProvider` that accepts `.stepik` files; on `createEditor`: if file is empty/uninitialized, show initialization dialog; if initialized, delegate to full editor (Phase 4); register `<fileEditorProvider>` in `plugin.xml`
- [x] T022 [US1] Implement initialization dialog in `StepikEditorProvider` — modal dialog prompting for course URL; validates URL format (`https://stepik.org/course/{id}/...`), extracts course ID; reads `.env` via `EnvReader`; shows error if credentials missing; on OK: calls `fetchFullCourse` in background, writes result to file via `StepikProjectService.saveFile`, then re-opens the editor
- [x] T023 [US1] Handle initialization error cases — invalid URL format (show validation error), missing `.env`/credentials (show error with instructions), API fetch failure (show error with details), user cancels dialog (leave file empty)

**Checkpoint**: User Story 1 complete — `.stepik` files can be initialized with a course URL. File contains full course structure JSON.

---

## Phase 4: User Story 2 — Browse Course Structure in the Navigator (Priority: P1)

**Goal**: Opening an initialized `.stepik` file shows a tree navigator with sections > lessons > steps.

**Independent Test**: Open an initialized `.stepik` file, verify tree shows all sections, expand to see lessons and steps, click a step (opens placeholder editor in this phase).

### Implementation for User Story 2

- [x] T024 [US2] Implement `CourseTreeModel` in `src/main/kotlin/org/example/stepik/navigator/CourseTreeModel.kt` — builds `DefaultTreeModel` from `StepikFileData` with three node levels: section nodes (with title + position), lesson nodes (with title), step nodes (with type icon + position + title preview); expose `rebuildFromData(StepikFileData)` for refresh
- [x] T025 [US2] Implement `StepikToolWindowFactory` in `src/main/kotlin/org/example/stepik/navigator/StepikToolWindowFactory.kt` — registers tool window on left side with `Tree` component inside `SimpleToolWindowPanel` + `JBScrollPane`; listens for `.stepik` file open events via `FileEditorManagerListener`; loads `StepikFileData` and populates tree; register `<toolWindow>` in `plugin.xml`
- [x] T026 [US2] Add tree selection listener in `StepikToolWindowFactory` — on step node click, open the step in the split editor (calls into `StepikEditorProvider` to navigate to that step); on section/lesson node click, expand/collapse only
- [x] T027 [US2] Add "Refresh from Stepik" action — toolbar button in the tool window that re-fetches full course via `StepikApiClient.fetchFullCourse`, overwrites `StepikFileData` (clearing all `local` fields), saves to file, and rebuilds tree via `CourseTreeModel.rebuildFromData`
- [x] T028 [US2] Update `StepikEditorProvider` to show the full editor (not just init dialog) when `.stepik` file is already initialized — load `StepikFileData`, display a placeholder panel ("Select a step from the navigator") until a step is selected

**Checkpoint**: User Story 2 complete — course tree is navigable, "Refresh from Stepik" works. Clicking a step shows a placeholder (split editor comes next).

---

## Phase 5: User Story 3 — Edit Steps in a Split Editor with Live Preview (Priority: P1)

**Goal**: Selecting a step opens a split view — left: form + HTML editor, right: JCEF live preview.

**Independent Test**: Click a step in the navigator, verify split editor opens. For a text step: edit HTML, see preview update. For a quiz step: see form fields (choices, correct answers, points) + HTML editor + live preview.

### Implementation for User Story 3

- [x] T029 [US3] Implement `StepPreviewPanel` in `src/main/kotlin/org/example/stepik/editor/StepPreviewPanel.kt` — wraps `JBCefBrowser`; guard with `JBCefApp.isSupported()`; expose `updatePreview(html: String)` that calls `loadHTML()` with the step's rendered HTML; show fallback message if JCEF unavailable
- [x] T030 [US3] Implement `TextStepForm` in `src/main/kotlin/org/example/stepik/editor/forms/TextStepForm.kt` — simple form with only an `EditorTextField` for HTML content (with HTML syntax highlighting via `HtmlFileType`); exposes `getContent(): StepContent` and `setContent(StepContent)`; fires change listener on edits
- [x] T031 [US3] Implement `ChoiceStepForm` in `src/main/kotlin/org/example/stepik/editor/forms/ChoiceStepForm.kt` — form fields: step type label, cost spinner, is_multiple_choice checkbox, preserve_order checkbox, feedback_correct/feedback_wrong text fields; editable JBTable for options (columns: text, is_correct checkbox, feedback); add/remove option buttons; HTML editor below for question text; fires change listener
- [x] T032 [P] [US3] Implement `MatchingStepForm` in `src/main/kotlin/org/example/stepik/editor/forms/MatchingStepForm.kt` — form fields: cost spinner, preserve_firsts_order checkbox; editable JBTable for pairs (columns: first, second); add/remove pair buttons; HTML editor for question text; fires change listener
- [x] T033 [P] [US3] Implement `StringStepForm` in `src/main/kotlin/org/example/stepik/editor/forms/StringStepForm.kt` — form fields: cost spinner, pattern text field, use_re checkbox, match_substring checkbox, case_sensitive checkbox; HTML editor for question text; fires change listener
- [x] T034 [US3] Implement `StepSourcePanel` in `src/main/kotlin/org/example/stepik/editor/StepSourcePanel.kt` — container panel that selects the correct form based on step type (TextStepForm for "text", ChoiceStepForm for "choice", MatchingStepForm for "matching", StringStepForm for "string", TextStepForm as fallback for other types); loads step data into form; collects edits from form into `StepContent`
- [x] T035 [US3] Implement `StepikSplitEditor` in `src/main/kotlin/org/example/stepik/editor/StepikSplitEditor.kt` — `FileEditor` implementation using `JBSplitter` (0.5 ratio); left = `StepSourcePanel`, right = `StepPreviewPanel`; on form change events, update preview with current HTML content; expose `loadStep(StepData)` to populate both panels
- [x] T036 [US3] Wire step selection from navigator to split editor — when user clicks a step node in the tree, call `StepikSplitEditor.loadStep(stepData)` to show the step's content in both panes; update `StepikEditorProvider` to host `StepikSplitEditor` as the main editor component
- [x] T037 [US3] Implement live preview update — on any change in `StepSourcePanel` (form field change or HTML edit), debounce 300ms, then call `StepPreviewPanel.updatePreview()` with current HTML content

**Checkpoint**: User Story 3 complete — split editor works for all step types. Form-based UI for structured steps, HTML editor for content, JCEF preview updates in real time.

---

## Phase 6: User Story 4 — Save Changes Locally (Priority: P2)

**Goal**: Edits are persisted to the `.stepik` file. Dirty state is visually indicated.

**Independent Test**: Edit a step, save (Ctrl+S), close file, reopen — verify the edit is preserved. Verify a visual indicator shows which steps have local changes.

### Implementation for User Story 4

- [x] T038 [US4] Implement local save logic in `StepikSplitEditor` — on form change, update the `StepData.local` field with current `StepContent` from `StepSourcePanel.getContent()`; mark the `FileEditor` as modified so IDE save (Ctrl+S) triggers persist
- [x] T039 [US4] Implement file persistence in `StepikEditorProvider` — override `saveState` / use `FileDocumentManagerListener` to serialize current `StepikFileData` (with updated `local` fields) to the `.stepik` file via `StepikProjectService.saveFile` on IDE save
- [x] T040 [US4] Add dirty-state indicator in navigator tree — step nodes with non-null `local` show a modified icon (e.g., blue dot or asterisk); update tree node rendering when step dirty state changes via `CourseTreeModel`
- [x] T041 [US4] Add dirty-state indicator in editor tab — show modified marker (*) in the editor tab title when any step has local changes; clear when all changes are pushed or discarded

**Checkpoint**: User Story 4 complete — local save works, dirty state visible in navigator and editor tab.

---

## Phase 7: User Story 5 — Push Changes to Stepik with Order Preservation (Priority: P2)

**Goal**: "UPDATE IN STEPIK" button pushes all local changes to Stepik. Step order is preserved. Conflicts are detected.

**Independent Test**: Edit steps, click "UPDATE IN STEPIK", verify changes appear on `https://stepik.org/course/286390/syllabus` with correct ordering. Test conflict: edit a step on Stepik web, then push from plugin — verify conflict warning and re-fetch.

### Implementation for User Story 5

- [x] T042 [US5] Implement `OrderPreserver` in `src/main/kotlin/org/example/stepik/sync/OrderPreserver.kt` — `recordOrder(lessonId): List<Pair<Int, Int>>` fetches current `(stepId, position)` pairs via `StepikApiClient.getSteps`; `restoreOrder(lessonId, original)` compares current vs original and issues `updateStepSource` calls to fix displaced steps per the order preservation protocol in `contracts/stepik-api.md`
- [x] T043 [US5] Implement `ConflictDetector` in `src/main/kotlin/org/example/stepik/sync/ConflictDetector.kt` — for each modified step, fetches current `step-source` from API, compares `block` content with `StepData.remote` snapshot; returns list of step IDs that have remote changes
- [x] T044 [US5] Implement `StepikSyncService` in `src/main/kotlin/org/example/stepik/sync/StepikSyncService.kt` — orchestrates the full push flow: (1) collect all dirty steps, (2) run `ConflictDetector` — if conflicts found, show warning dialog, on OK re-fetch full course and abort push, (3) for each affected lesson call `OrderPreserver.recordOrder`, (4) push each dirty step via `StepikApiClient.updateStepSource`, (5) for each affected lesson call `OrderPreserver.restoreOrder`, (6) on success: clear all `local` fields, update `remote` with pushed content, save file; all API work on background thread with progress indicator
- [x] T045 [US5] Add "UPDATE IN STEPIK" button — editor toolbar action (or floating action button) visible when `StepikFileData.isDirty`; on click invokes `StepikSyncService.pushAll`; show progress bar during push; disable button when no local changes
- [x] T046 [US5] Handle push error cases — API errors per step: collect failures, show summary dialog listing failed steps with error messages, preserve local changes for retry; network errors: show connection error, preserve all local changes
- [x] T047 [US5] Handle conflict flow — when `ConflictDetector` finds conflicts: show dialog listing conflicting step IDs and their lesson/section context; on "OK": re-fetch entire course via `fetchFullCourse`, overwrite `StepikFileData`, save file, rebuild navigator tree, show notification "Course refreshed — please re-edit and push again"
- [ ] T048 Write unit test for `OrderPreserver` in `src/test/kotlin/org/example/stepik/sync/OrderPreserverTest.kt` — test: no reordering needed (positions unchanged), reordering detected and fixed, partial overlap
- [ ] T049 [P] Write unit test for `ConflictDetector` in `src/test/kotlin/org/example/stepik/sync/ConflictDetectorTest.kt` — test: no conflicts (remote matches snapshot), conflicts detected (remote differs), mixed dirty and clean steps

**Checkpoint**: User Story 5 complete — full edit-save-push cycle works with order preservation and conflict detection.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Error handling, UX improvements, and final validation

- [x] T050 Implement transparent token refresh in `StepikAuth` — on 401 response, clear cached token and retry request once; ensure all `StepikApiClient` methods handle re-auth transparently
- [x] T051 Implement `.stepik` file corruption handling in `StepikProjectService.loadFile` — catch JSON parse errors, show notification offering to re-fetch from Stepik or create new file
- [ ] T052 Implement deleted course handling — if `fetchFullCourse` returns 404, show error dialog offering to reinitialize the `.stepik` file with a different course URL
- [ ] T053 [P] Add step type icons in navigator tree — distinct icons per step type (text, choice, matching, string, code) for quick visual identification
- [ ] T054 [P] Add keyboard navigation support in navigator tree — Enter to open step, arrow keys to navigate, Ctrl+R to refresh
- [x] T055 Update `CLAUDE.md` with new build commands (`./gradlew runIde`, `./gradlew buildPlugin`, `./gradlew verifyPlugin`) and updated project structure
- [ ] T056 Run full end-to-end validation against test course `https://stepik.org/course/286390/syllabus` — initialize, browse, edit text step, edit quiz step, save locally, push to Stepik, verify on web, refresh from Stepik

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — first MVP increment
- **US2 (Phase 4)**: Depends on Phase 2 + Phase 3 (needs `fetchFullCourse` and `StepikEditorProvider` from US1)
- **US3 (Phase 5)**: Depends on Phase 4 (needs navigator and editor hosting from US2)
- **US4 (Phase 6)**: Depends on Phase 5 (needs split editor from US3)
- **US5 (Phase 7)**: Depends on Phase 6 (needs local save from US4)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: First story — no story dependencies. MVP foundation.
- **US2 (P1)**: Depends on US1 — needs initialized `.stepik` file and `fetchFullCourse`.
- **US3 (P1)**: Depends on US2 — needs navigator to select steps.
- **US4 (P2)**: Depends on US3 — needs split editor to produce edits.
- **US5 (P2)**: Depends on US4 — needs local save to have dirty steps to push.

### Within Each User Story

- Models/utilities before services
- Services before UI components
- Core implementation before error handling
- Story complete before moving to next priority

### Parallel Opportunities

**Phase 1**: T004, T005, T006 can run in parallel (directory creation + icon)
**Phase 2**: T008–T014 can all run in parallel (independent model/utility files); T018, T019 can run in parallel (independent test files)
**Phase 5**: T032, T033 can run in parallel (independent form implementations)
**Phase 7**: T048, T049 can run in parallel (independent test files)
**Phase 8**: T053, T054 can run in parallel (independent UI enhancements)

---

## Parallel Example: Phase 2 (Foundational)

```
# Launch all model classes in parallel:
T008: StepContent.kt
T009: StepData.kt
T010: LessonData.kt
T011: SectionData.kt
T012: StepikFileData.kt

# Launch all utility classes in parallel:
T013: EnvReader.kt
T014: StepikAuth.kt

# Then sequentially (depends on above):
T015: StepikApiClient.kt (depends on T014 StepikAuth)
T016: StepikFileType.kt (independent but sequential for plugin.xml updates)
T017: StepikProjectService.kt (depends on T012 StepikFileData)

# Launch tests in parallel:
T018: StepikFileDataTest.kt
T019: EnvReaderTest.kt
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 — Initialize `.stepik` file
4. **STOP and VALIDATE**: Create `.stepik` file, enter test course URL, verify JSON populated
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational → Plugin builds and runs
2. US1 → File initialization works (MVP!)
3. US2 → Course navigation works
4. US3 → Split editor with live preview works
5. US4 → Local save with dirty indicators works
6. US5 → Push to Stepik with order preservation works
7. Polish → Error handling, UX improvements, final validation

---

## Notes

- All API calls must run on background threads (not EDT) — use `ApplicationManager.getApplication().executeOnPooledThread` or `Task.Backgroundable`
- The `.stepik` file is the single source of truth for local state — all components read/write through `StepikProjectService`
- JCEF availability must be checked before creating preview panel — provide graceful fallback
- Step types beyond text/choice/matching/string (e.g., code, number, math) use `TextStepForm` as fallback (HTML-only editing)
- Test course for validation: https://stepik.org/course/286390/syllabus
