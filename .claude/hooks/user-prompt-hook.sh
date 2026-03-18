#!/bin/bash
# Merged UserPromptSubmit hook — commit workflow + real-data check + problem workflow
# Single process spawn instead of 3.

INPUT=$(cat)
PROMPT=$(echo "$INPUT" | jq -r '.prompt // empty' | tr '[:upper:]' '[:lower:]')
CONTEXT_PARTS=()

# ── 1. Commit workflow ──
if echo "$PROMPT" | grep -qE '\bcommit\b'; then
  CONTEXT_PARTS+=("COMMIT WORKFLOW: Run ./scripts/commit.sh \"type: description\" — pulls, tests, stages, commits, pushes. Do NOT use manual git commands.")

  # ── 2. Real-data check (only on commit) ──
  UNTRACKED=$(git ls-files --others --exclude-standard 2>/dev/null | grep -E '\.(py|rs|go|js|ts)$' || true)
  STAGED_NEW=$(git diff --cached --name-only --diff-filter=A 2>/dev/null | grep -E '\.(py|rs|go|js|ts)$' || true)
  if [[ -n "${UNTRACKED}${STAGED_NEW}" ]]; then
    CONTEXT_PARTS+=("REAL DATA VALIDATION (PW-07): New code files detected. Confirm user has validated with real data before committing.")
  fi
fi

# ── 3. Problem workflow ──
if ! echo "$PROMPT" | grep -qE '\bcommit\b'; then
  if ! echo "$PROMPT" | grep -qE '^(where|what|how|why|explain|show|list|describe|which|can you tell)\b'; then
    if ! echo "$PROMPT" | grep -qE '^(remember|forget|/|yes|no|ok|sure|go ahead|confirm|approved|lgtm)\b'; then
      WORD_COUNT=$(echo "$PROMPT" | wc -w | tr -d ' ')
      if [ "$WORD_COUNT" -ge 4 ]; then
        if ! echo "$PROMPT" | grep -qiE 'PB-[0-9]+'; then
          if echo "$PROMPT" | grep -qE '\b(implement|add|build|create|fix|change|refactor|modify|update|write|make|setup|set up|configure|integrate|migrate|replace|remove|delete|redesign|rework|introduce|extend|extract|move|split|merge|convert|upgrade|downgrade)\b'; then
            COUNTER_FILE="$CLAUDE_PROJECT_DIR/problems/.counter"
            NEXT_ID="001"
            if [ -f "$COUNTER_FILE" ]; then
              RAW=$(cat "$COUNTER_FILE" | tr -d '[:space:]')
              if [ "$RAW" -eq "$RAW" ] 2>/dev/null; then
                NEXT_ID=$(printf '%03d' "$RAW")
              fi
            fi
            CONTEXT_PARTS+=("PROBLEM WORKFLOW: This looks like a task. Next ID: PB-${NEXT_ID}. Follow mandatory/problem-workflow.md (PW-01–PW-08). Bypass: 'just do it'.")
          fi
        fi
      fi
    fi
  fi
fi

# ── Emit combined context ──
if [ ${#CONTEXT_PARTS[@]} -gt 0 ]; then
  COMBINED=$(printf '%s ' "${CONTEXT_PARTS[@]}")
  cat <<EOF
{
  "additionalContext": "$COMBINED"
}
EOF
fi

exit 0
