#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$HOME/313-team"
CONFIG_DIR="$HOME/.config/313"
CONFIG="$CONFIG_DIR/team.json"
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
pkg update -y
pkg install -y python git
mkdir -p "$ROOT" "$CONFIG_DIR"
chmod 700 "$CONFIG_DIR"
cp "$SOURCE_DIR/313_team_server.py" "$ROOT/313_team_server.py"
echo
echo "313 Team setup - free-only mode"
echo "Only OpenRouter's free-model router is enabled."
echo "No paid models, billing, credits, top-up, or paid fallback."
read -r -p "GitHub repository (owner/repo): " REPO
if [[ ! "$REPO" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]]; then
  echo "Invalid repository format."
  exit 1
fi
read -r -p "GitHub branch to review [feature/313-termux-agent-team]: " GH_BRANCH
GH_BRANCH="${GH_BRANCH:-feature/313-termux-agent-team}"
read -r -s -p "GitHub fine-grained token (Contents read/write for this repo): " GH_TOKEN
echo
read -r -s -p "OpenRouter API key (free account; do not add credits): " OR_KEY
echo
if [[ -z "$GH_TOKEN" || -z "$OR_KEY" ]]; then
  echo "Both credentials are required."
  exit 1
fi
python - "$CONFIG" "$REPO" "$GH_BRANCH" "$GH_TOKEN" "$OR_KEY" <<'PY'
import json, os, secrets, sys
path, repo, branch, gh, orkey = sys.argv[1:]
data = {"github_repo": repo, "github_branch": branch,
        "github_token": gh, "openrouter_api_key": orkey,
        "gateway_token": secrets.token_urlsafe(32),
        "provider": "openrouter/free", "free_only": True,
        "paid_fallback": False}
fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
with os.fdopen(fd, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
print(data["gateway_token"])
PY
chmod 600 "$CONFIG"
echo
echo "Setup saved in Termux private storage: $CONFIG"
echo "Credentials are not stored in GitHub or the APK."
echo "Copy the one-time gateway token printed above into 313's Team settings."
echo "Start the local service with:"
echo "python $ROOT/313_team_server.py"
