# Problem-Driven Workflow

> Applies to all non-trivial tasks in this project. Enforces structured problem analysis,
> planning, and TDD implementation with full traceability.

## Lifecycle

```
PW-01  Problem Statement    →  problems/PB-XXX-slug/README.md
PW-02  Investigation        →  problems/PB-XXX-slug/investigation.md
PW-03  Decision Gate        →  problems/PB-XXX-slug/decision.md  (or abort)
PW-04  High-Level Plan      →  problems/PB-XXX-slug/high-level-plan.md
PW-05  Implementation Plan  →  problems/PB-XXX-slug/implementation-plan.md
PW-06  TDD Implementation   →  commit on every green
PW-07  Verification         →  test against plan, investigate drift
PW-08  Completion           →  mark done, cleanup, final commit
```

## Problem IDs

- Auto-incremented: `PB-001`, `PB-002`, ...
- Counter lives in `problems/.counter` (plain integer, next available ID)
- The ID appears everywhere:
  - **Folder**: `problems/PB-001-slug/`
  - **Branch**: `PB-001-slug`
  - **Commits**: `type(PB-001): description`

## Rules

### PW-01 — Problem Statement

When the user requests a non-trivial task:

1. Read `problems/.counter` to get the next ID (e.g. `PB-003`)
2. Derive a short slug from the problem (e.g. `fix-gibberish-threshold`)
3. Create branch: `git checkout -b PB-003-fix-gibberish-threshold`
4. Create `problems/PB-003-fix-gibberish-threshold/README.md` with this template:

```markdown
---
id: PB-003
status: open
created: YYYY-MM-DD
branch: PB-003-fix-gibberish-threshold
---

# Problem: Fix Gibberish Threshold

## Problem Statement

<Clear description of the problem, context, and motivation>

## Acceptance Criteria

- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3
```

5. Increment `problems/.counter`
6. Commit: `chore(PB-003): open problem statement`

### PW-02 — Investigation

1. Research the problem: read relevant code, check existing patterns, consider constraints
2. Identify **at least 2 options** (when applicable)
3. Create `problems/PB-XXX-slug/investigation.md`:

```markdown
# Investigation: PB-003 Fix Gibberish Threshold

## Context

<What was found during investigation>

## Options

### Option A: <Name>

**Description**: ...
**Trade-offs**: ...
**Risks**: ...
**Effort**: low | medium | high

### Option B: <Name>

**Description**: ...
**Trade-offs**: ...
**Risks**: ...
**Effort**: low | medium | high

## Recommendation

<Which option and why>
```

4. Commit: `docs(PB-003): investigation complete`

### PW-03 — Decision Gate

1. Present investigation to user
2. Ask: **"Which option do you want, or should I reinvestigate?"**
3. Wait for user response
4. Record in `problems/PB-XXX-slug/decision.md`:

```markdown
# Decision: PB-003

**Chosen option**: Option A — <Name>
**Rationale**: <User's reasoning or "per user preference">
**Date**: YYYY-MM-DD
```

5. Update README.md status: `status: planned`

**Abort Gate**: If the user says "won't do" or investigation reveals infeasibility:
- Record rationale in `decision.md` with `**Chosen option**: ABORT`
- Update README.md status: `status: won't-do`
- Commit: `docs(PB-003): closed — won't do`
- Stop workflow here

### PW-04 — High-Level Plan

1. Based on the chosen option, create `problems/PB-XXX-slug/high-level-plan.md`
2. Uses markdown-plan syntax (`[x]` for done, hierarchical lists):

```markdown
# High-Level Plan: PB-003

1. Understand current gibberish scoring logic
2. Design new threshold mechanism
   - Define configurable threshold parameter
   - Identify edge cases from real data
3. Implement with TDD
4. Validate with real model outputs
5. Update documentation
```

### PW-05 — Implementation Plan

1. Expand high-level plan into `problems/PB-XXX-slug/implementation-plan.md`
2. Each task is concrete and testable:

```markdown
# Implementation Plan: PB-003

1. Write test for new threshold boundary
   - [ ] test_gibberish_below_threshold_is_skipped
   - [ ] test_gibberish_above_threshold_passes
   - [ ] test_gibberish_at_exact_threshold_passes
2. Implement threshold parameter in triage_output
   - [ ] Add threshold parameter with default
   - [ ] Wire threshold into gibberish check
3. Integration tests
   - [ ] test_triage_with_real_model_output
   - [ ] test_sweep_uses_new_threshold
4. Update docs
   - [ ] Update scoring.feature if needed
   - [ ] Update CLAUDE.md if interface changed
```

3. Present to user: **"Here's the implementation plan. Confirm, or suggest changes?"**
4. Wait for confirmation
5. Update README.md status: `status: in-progress`
6. Commit: `docs(PB-003): implementation plan confirmed`

### PW-06 — TDD Implementation

1. Follow `mandatory/TDD.md` strictly (Red → Green → Commit → Refactor → Commit)
2. Work through `implementation-plan.md` tasks in order
3. **Commit on every green**: `test(PB-003): ...` then `feat(PB-003): ...`
4. Mark tasks `[x]` in `implementation-plan.md` as completed
5. Include plan file updates in implementation commits

### PW-07 — Verification

1. Run full test suite: `pytest tests/ -v`
2. Compare implementation against `implementation-plan.md`:
   - All tasks should be `[x]`
   - If any tasks were skipped or changed, document why
3. Check acceptance criteria in README.md — all should pass
4. **Real-data validation**: If new code files were created, ask the user to confirm they have validated with real data before proceeding
5. **Investigate drift**: if implementation diverged from plan, note it:
   - Minor drift: document in a comment in the plan file
   - Major drift: discuss with user before proceeding
6. Commit: `test(PB-003): verification complete`

### PW-08 — Completion

1. Mark all tasks `[x]` in both plan files
2. Update README.md:
   - `status: done`
   - Check all acceptance criteria boxes
3. Commit: `chore(PB-003): problem complete`
4. Report to user: summary of what was done, any drift noted

## What Triggers This Workflow

Any user request that involves:
- Implementing new functionality
- Fixing bugs
- Modifying existing behavior
- Refactoring with behavioral changes

## What Does NOT Trigger This Workflow

- Simple questions ("where is X?", "explain Y")
- Commits
- Reading/exploring code
- Documentation-only changes
- Configuration changes
- Trivial one-line fixes (user can override with "just do it")

## Exceptions

The user can bypass this workflow by saying:
- "just do it" — skip to implementation
- "skip investigation" — go straight from PW-01 to PW-04
- "won't do" — abort at any gate
