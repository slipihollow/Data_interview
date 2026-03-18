#!/bin/bash
# Full commit workflow for project_Epistemic
# Pull → Test → Stage → Commit → Push — all in one shot.
#
# Usage:
#   ./scripts/commit.sh "type: short description"
#   ./scripts/commit.sh -n "type: description"   # dry run (no commit, no push)
#
# Types: feat, fix, refactor, docs, experiment, chore, test

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()   { echo -e "${GREEN}[OK]${NC}    $1"; }
warn()   { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error()  { echo -e "${RED}[FAIL]${NC}  $1"; }
header() { echo -e "${BLUE}=== $1 ===${NC}"; }

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# ── Parse arguments ─────────────────────────────────────────────
DRY_RUN=false
MESSAGE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--dry-run) DRY_RUN=true; shift ;;
        -h|--help)
            echo "Usage: $(basename "$0") [OPTIONS] \"type: commit message\""
            echo ""
            echo "Options:"
            echo "  -n, --dry-run    Show what would happen without committing"
            echo "  -h, --help       Show this help"
            echo ""
            echo "Types: feat, fix, refactor, docs, experiment, chore, test"
            echo ""
            echo "Workflow: pull → test → stage → commit → push"
            exit 0
            ;;
        -*) error "Unknown option: $1"; exit 1 ;;
        *)  MESSAGE="$1"; shift ;;
    esac
done

if [[ -z "$MESSAGE" ]]; then
    error "Commit message required"
    echo "Usage: $(basename "$0") \"type: short description\""
    exit 1
fi

# ── Validate commit message format ──────────────────────────────
VALID_TYPES="feat|fix|refactor|docs|experiment|chore|test"
if ! echo "$MESSAGE" | grep -qE "^($VALID_TYPES): "; then
    error "Commit message must start with a type: $VALID_TYPES"
    echo "  Example: \"feat: add probe for cybernetics framing\""
    exit 1
fi

BRANCH=$(git branch --show-current)

# ── Step 1: Check for changes ──────────────────────────────────
header "Step 1/5: Checking for changes"

if git diff --quiet && git diff --cached --quiet && [[ -z "$(git ls-files --others --exclude-standard)" ]]; then
    warn "No changes to commit"
    exit 0
fi

git status -s
echo ""

# ── Step 2: Run tests ──────────────────────────────────────────
header "Step 2/5: Running tests"

if [[ -d "tests" ]] && ls tests/**/*.py &>/dev/null; then
    if source .venv/bin/activate 2>/dev/null && pytest tests/ -v --tb=short; then
        info "All tests passed"
    else
        error "Tests failed — fix before committing"
        exit 1
    fi
else
    warn "No tests found, skipping"
fi

echo ""

# ── Step 3: Stage & Commit ──────────────────────────────────────
header "Step 3/5: Staging & committing"

# Stage tracked files (modified/deleted)
git add -u

# Stage new files, excluding artifacts
# Use -z for NUL-delimited output to handle Unicode filenames correctly
git ls-files --others --exclude-standard -z \
    | tr '\0' '\n' \
    | grep -v '\.coverage$' \
    | grep -v '__pycache__' \
    | grep -v '\.pyc$' \
    | grep -v '\.egg-info' \
    | grep -v 'gemma-env/' \
    | while IFS= read -r f; do
        if [[ -n "$f" ]]; then
            git add -- "$f"
            echo "  + $f"
        fi
    done

if [[ "$DRY_RUN" == "true" ]]; then
    warn "DRY RUN — would commit & push:"
    echo ""
    echo "  $MESSAGE"
    echo "  Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
    echo ""
    git diff --cached --stat
    exit 0
fi

git commit -m "$(cat <<EOF
$MESSAGE

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"

info "Committed"
echo ""

# ── Step 4: Pull --rebase ───────────────────────────────────────
header "Step 4/5: Pulling latest (rebase on origin/$BRANCH)"

if git rev-parse --abbrev-ref '@{u}' &>/dev/null; then
    git pull --rebase 2>&1 || {
        error "Rebase conflict — resolve manually, then push"
        exit 1
    }
    info "Up to date"
else
    warn "No upstream branch yet — will set on push"
fi

echo ""

# ── Step 5: Push ────────────────────────────────────────────────
header "Step 5/5: Pushing to origin/$BRANCH"

if git rev-parse --abbrev-ref '@{u}' &>/dev/null; then
    git push 2>&1
else
    git push -u origin "$BRANCH" 2>&1
fi

info "Pushed"
echo ""

# ── Summary ─────────────────────────────────────────────────────
header "Done"
git log --oneline -1
