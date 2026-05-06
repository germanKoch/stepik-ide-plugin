# Data Model: Stepik Course Editor Plugin

**Date**: 2026-05-06  
**Branch**: `001-stepik-course-editor`

## Entities

### StepikFileData (root of .stepik JSON file)

| Field | Type | Description |
|-------|------|-------------|
| courseUrl | String | Original Stepik course URL provided by user |
| courseId | Int | Extracted course ID from URL |
| courseTitle | String | Course title from Stepik |
| lastFetched | String (ISO 8601) | Timestamp of last full fetch from Stepik |
| sections | List\<SectionData\> | Ordered list of course sections |

### SectionData

| Field | Type | Description |
|-------|------|-------------|
| id | Int | Stepik section ID |
| title | String | Section title |
| position | Int | Position within course |
| lessons | List\<LessonData\> | Ordered list of lessons in this section |

### LessonData

| Field | Type | Description |
|-------|------|-------------|
| id | Int | Stepik lesson ID |
| unitId | Int | Stepik unit ID (links lesson to section) |
| title | String | Lesson title (max 64 chars on Stepik) |
| position | Int | Unit position within section |
| steps | List\<StepData\> | Ordered list of steps in this lesson |

### StepData

| Field | Type | Description |
|-------|------|-------------|
| id | Int | Stepik step ID (used as step-source ID for API calls) |
| position | Int | Position within lesson |
| type | String | Step type: "text", "choice", "matching", "string", "number", "code", etc. |
| cost | Int | Point cost (0 = ungraded) |
| remote | StepContent | Last-fetched content from Stepik (immutable between fetches) |
| local | StepContent? | Locally edited content (null = no local changes) |

### StepContent

| Field | Type | Description |
|-------|------|-------------|
| text | String | HTML content of the step |
| source | Map\<String, Any\>? | Type-specific structured data (quiz options, matching pairs, etc.). Null for text steps. |

#### source field by step type

**choice (quiz)**:
| Field | Type | Description |
|-------|------|-------------|
| options | List\<ChoiceOption\> | Answer choices |
| is_multiple_choice | Boolean | Whether multiple answers are correct |
| preserve_order | Boolean | Whether choice order is fixed |
| sample_size | Int | Number of options shown |
| feedback_correct | String | Feedback for correct answer |
| feedback_wrong | String | Feedback for wrong answer |

**ChoiceOption**:
| Field | Type | Description |
|-------|------|-------------|
| text | String | Choice text (HTML) |
| is_correct | Boolean | Whether this is a correct answer |
| feedback | String | Per-option feedback |

**matching**:
| Field | Type | Description |
|-------|------|-------------|
| pairs | List\<MatchPair\> | Matching pairs |
| preserve_firsts_order | Boolean | Whether left-column order is fixed |

**MatchPair**:
| Field | Type | Description |
|-------|------|-------------|
| first | String | Left-side item |
| second | String | Right-side item |

**string**:
| Field | Type | Description |
|-------|------|-------------|
| pattern | String | Correct answer or regex |
| use_re | Boolean | Whether pattern is a regex |
| match_substring | Boolean | Accept substring match |
| case_sensitive | Boolean | Case-sensitive matching |

## Relationships

```
StepikFileData 1──* SectionData 1──* LessonData 1──* StepData
                                                       ├── remote: StepContent (always present)
                                                       └── local: StepContent (null if unchanged)
```

## State Transitions

### StepData dirty state

```
[Clean] ──(user edits)──> [Dirty: local != null]
[Dirty] ──(save to .stepik file)──> [Dirty: persisted locally]
[Dirty] ──(push to Stepik succeeds)──> [Clean: local = null, remote = pushed content]
[Dirty] ──(refresh from Stepik)──> [Clean: local = null, remote = fetched content]
[Dirty] ──(conflict detected on push)──> [Clean: local = null, remote = re-fetched content]
```

### StepikFile lifecycle

```
[Empty file] ──(user provides course URL)──> [Initialized]
[Initialized] ──(open file)──> [Loaded: tree visible, steps browsable]
[Loaded] ──(edit step)──> [Has local changes]
[Has local changes] ──(save)──> [Persisted locally]
[Persisted locally] ──(UPDATE IN STEPIK)──> [Synced: all local = null]
[Any state] ──(Refresh from Stepik)──> [Loaded: all local = null, fresh remote]
[Any state] ──(file corrupted)──> [Error: offer re-fetch]
```

## Validation Rules

- `courseUrl` must match pattern `https://stepik.org/course/{id}/*`
- `courseId` must be a positive integer
- `sections` must be ordered by `position`
- `lessons` within each section must be ordered by `position`
- `steps` within each lesson must be ordered by `position`
- `step.type` must be one of the known Stepik step types
- `step.cost` must be >= 0
- `step.local` is null when step has no local edits (clean state)
- `step.remote` is never null (always contains last-fetched state)
