#!/bin/bash
# Responses guard — enforces "responses/ is append-only" rule
# Blocks any Edit/Write/Bash that would delete or overwrite files in responses/

INPUT=$(cat)
TOOL=$(echo "$INPUT" | jq -r '.tool_name // empty')
FILE=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# ── Edit / Write targeting responses/ ──
if [[ "$TOOL" == "Edit" || "$TOOL" == "Write" ]]; then
  if [[ "$FILE" == *"/responses/"* || "$FILE" == responses/* ]]; then
    cat <<EOF
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "BLOCKED: responses/ is append-only. Cannot edit or overwrite existing files. See CLAUDE.md."
  }
}
EOF
    exit 0
  fi
fi

# ── Bash commands targeting responses/ ──
if [[ "$TOOL" == "Bash" ]]; then
  if echo "$CMD" | grep -qE '(rm|mv|cp\s.*>)\s+.*responses/'; then
    cat <<EOF
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "BLOCKED: responses/ is append-only. Cannot rm/mv/overwrite files in responses/. See CLAUDE.md."
  }
}
EOF
    exit 0
  fi
fi

exit 0
