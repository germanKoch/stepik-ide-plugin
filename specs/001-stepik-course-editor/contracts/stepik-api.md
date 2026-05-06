# Stepik API Contract

**Date**: 2026-05-06  
**Base URL**: `https://stepik.org/api`  
**Auth**: OAuth2 client credentials → `POST https://stepik.org/oauth2/token/`

## Authentication

```
POST /oauth2/token/
Authorization: Basic base64(CLIENT_ID:CLIENT_SECRET)
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials

Response: { "access_token": "...", "expires_in": 3600, "token_type": "Bearer" }
```

All subsequent requests use `Authorization: Bearer {access_token}`.

## Endpoints Used by Plugin

### GET /api/courses/{course_id}
Returns course with `sections` array (list of section IDs).

### GET /api/sections?ids[]={id1}&ids[]={id2}...
Returns sections with `units` array (list of unit IDs) and `position`, `title`.

### GET /api/units?ids[]={id1}&ids[]={id2}...
Returns units with `lesson` (lesson ID), `position`, `section`.

### GET /api/lessons/{lesson_id}
Returns lesson with `title`, `steps_count`.

### GET /api/steps?lesson={lesson_id}
Returns all steps in a lesson with `position`, `block` (contains `name` (type) and `text` (HTML content)).

### GET /api/step-sources/{step_id}
Returns full step-source with `block` (complete content including `source` for structured types), `cost`, `lesson`, `position`.

### PUT /api/step-sources/{step_id}
Updates step content. Body: `{ "step-source": { "block": {...}, "position": N } }`.
**Warning**: May reorder other steps in the lesson as a side effect.

## Order Preservation Protocol

1. Before update: `GET /api/steps?lesson={id}` → record `[(step_id, position)]`
2. Perform `PUT /api/step-sources/{id}` for each modified step
3. After update: `GET /api/steps?lesson={id}` → compare with original
4. If order changed: for each displaced step, `PUT /api/step-sources/{id}` with original position + current block

## Known Constraints

- Lesson titles: max 64 characters
- Step types: text, choice, matching, string, number, math, code, sorting, fill-blanks
- Graded types (have cost): choice, matching, string, number, math, code, sorting, fill-blanks
- API rate limits: not officially documented; use reasonable delays between batch operations
