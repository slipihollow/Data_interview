#!/bin/bash
# TDD enforcement hook — fires before Edit/Write on source files
# Reminds Claude to follow test-first discipline for all languages

INPUT=$(cat)
FILE=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# No file path → allow
[[ -z "$FILE" ]] && exit 0

# ── Allow test files (that's what you should be writing first) ──
[[ "$FILE" == *"/tests/"* ]] && exit 0
[[ "$FILE" == *"/test/"* ]] && exit 0
[[ "$FILE" == *"/__tests__/"* ]] && exit 0
[[ "$FILE" == *".test."* ]] && exit 0
[[ "$FILE" == *".spec."* ]] && exit 0
[[ "$FILE" == *"_test."* ]] && exit 0
[[ "$FILE" == *"test_"* ]] && exit 0
[[ "$FILE" == *"Test."* ]] && exit 0
[[ "$FILE" == *"Tests."* ]] && exit 0

# ── Allow non-source files (docs, configs, data, etc.) ──
BASENAME=$(basename "$FILE")
EXT="${BASENAME##*.}"

SOURCE_EXTS="py js ts jsx tsx rs go java kt swift c cpp cc h hpp cs rb php scala ex exs clj zig lua dart"
IS_SOURCE=false
for e in $SOURCE_EXTS; do
  if [[ "$EXT" == "$e" ]]; then
    IS_SOURCE=true
    break
  fi
done

[[ "$IS_SOURCE" == false ]] && exit 0

# ── Allow spec/feature files ──
[[ "$FILE" == *".feature" ]] && exit 0
[[ "$FILE" == *".stories."* ]] && exit 0

# ── Detect new function/method definitions across languages ──
NEW_CODE=$(echo "$INPUT" | jq -r '.tool_input.new_string // .tool_input.content // empty')
HAS_NEW_DEF=false
if echo "$NEW_CODE" | grep -qE '^\s*(def |fn |func |function |pub fn |pub func |public |private |protected |static |async function|async fn|const \w+ = \(|let \w+ = \(|export (default )?(function|const|class)|class )'; then
  HAS_NEW_DEF=true
fi

# Build reason message
REASON="TDD check: You are editing a source file ($EXT). Test-first rules:\n- Every new function or behaviour change starts with a test\n- Test commit lands before implementation commit\n- Run the test suite before committing\nHave you written a failing test first?"

if [ "$HAS_NEW_DEF" = true ]; then
  REASON="$REASON\n\nNew function/class detected. Before writing custom code:\n1. Search the codebase for existing implementations\n2. Check standard library and well-known packages\n3. Ask the user before doing any web search\nOnly write custom code if nothing fits."
fi

# Source file being edited → enforce TDD
cat <<EOF
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "ask",
    "permissionDecisionReason": "$REASON"
  }
}
EOF
exit 0
