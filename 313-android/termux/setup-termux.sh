#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$HOME/313-team"
CONFIG_DIR="$HOME/.config/313"
CONFIG="$CONFIG_DIR/team.json"
OMNI_KEY="$HOME/.omniroute-key"
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
pkg update -y
pkg install -y python git
mkdir -p "$ROOT" "$CONFIG_DIR"
chmod 700 "$CONFIG_DIR"
cp "$SOURCE_DIR/313_team_server.py" "$ROOT/313_team_server.py"

echo
echo "313 Team setup - OmniRoute local gateway"
echo "No OpenRouter key is required by 313."
echo "Inference stays blocked unless you confirm OmniRoute strict free-access mode."
python - "$CONFIG" "$OMNI_KEY" <<'PY'
import getpass, json, os, secrets, sys
config_path, key_path = sys.argv[1:]
data = {}
if os.path.exists(config_path):
    try:
        with open(config_path, encoding="utf-8") as f:
            data = json.load(f)
    except Exception:
        print("Existing 313 config is invalid; refusing to overwrite it.")
        raise SystemExit(1)

repo = data.get("github_repo") or input("GitHub repository (owner/repo): ").strip()
if "/" not in repo or len(repo.split("/")) != 2:
    print("Invalid repository format.")
    raise SystemExit(1)
branch = data.get("github_branch") or input("GitHub branch [feature/313-real-provider-team]: ").strip()
branch = branch or "feature/313-real-provider-team"
github_token = data.get("github_token") or getpass.getpass("GitHub fine-grained token (Contents read/write): ")
if not github_token:
    print("GitHub token is required.")
    raise SystemExit(1)

if not os.path.exists(key_path):
    omni_key = getpass.getpass("OmniRoute local API key (from Dashboard > Endpoints): ")
    if not omni_key:
        print("OmniRoute API key is required.")
        raise SystemExit(1)
    fd = os.open(key_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write(omni_key.strip())
else:
    print("Using the existing local OmniRoute key file.")

confirm = input("Have you enabled Free Access Policy = Strict in OmniRoute? Type YES to allow AI requests: ").strip()
strict_confirmed = confirm == "YES"

data.update({
    "github_repo": repo,
    "github_branch": branch,
    "github_token": github_token,
    "gateway_token": data.get("gateway_token") or secrets.token_urlsafe(32),
    "omniroute_base_url": data.get("omniroute_base_url") or "http://127.0.0.1:20128",
    "omniroute_model": "auto",
    "omniroute_strict_free_confirmed": strict_confirmed,
    "provider": "omniroute",
    "free_only": True,
    "paid_fallback": False
})
fd = os.open(config_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
with os.fdopen(fd, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
print("GATEWAY_TOKEN=" + data["gateway_token"])
print("STRICT_FREE_CONFIRMED=" + str(strict_confirmed))
PY
chmod 600 "$CONFIG" "$OMNI_KEY" 2>/dev/null || true
echo
echo "Config saved privately: $CONFIG"
echo "OmniRoute key saved privately: $OMNI_KEY"
echo "No credentials are committed to GitHub or embedded in the APK."
echo "Paste GATEWAY_TOKEN into 313 Team settings only if it is a new token."
echo "Start the service with:"
echo "python $ROOT/313_team_server.py"
