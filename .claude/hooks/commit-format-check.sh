#!/bin/bash
# Fires on PreToolUse for Bash — validates git commit message format
# Enforces: type(PB-XXX): description when on a PB branch

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Only check git commit commands
if ! echo "$COMMAND" | grep -q 'git commit'; then
  exit 0
fi

# Get current branch
BRANCH=$(git branch --show-current 2>/dev/null || echo "")

# If on a PB- branch, enforce commit message format
if echo "$BRANCH" | grep -qE '^PB-[0-9]{3}'; then
  PB_ID=$(echo "$BRANCH" | grep -oE 'PB-[0-9]{3}')

  # Check if the commit message contains the proper type(PB-XXX): format
  if ! echo "$COMMAND" | grep -qE "(feat|fix|test|docs|chore|refactor)\($PB_ID\):"; then
    echo "BLOCKED: Commit message must follow format: type($PB_ID): description" >&2
    echo "Valid types: feat, fix, test, docs, chore, refactor" >&2
    echo "Current branch: $BRANCH" >&2
    exit 2
  fi

  exit 0
fi

# If NOT on a PB branch, warn if this looks like non-trivial work
if echo "$COMMAND" | grep -qiE '(feat|fix|implement|refactor)\('; then
  echo "WARNING: Feature/fix commit detected on branch '$BRANCH' which is not a PB-XXX branch." >&2
  echo "Non-trivial changes should follow the problem-driven workflow (PW-01 through PW-08)." >&2
  echo "If this is intentional, have the user say 'just do it' to bypass." >&2
  exit 2
fi

exit 0
