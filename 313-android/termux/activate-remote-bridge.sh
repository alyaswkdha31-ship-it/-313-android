#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
REPO_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
ROOT="$HOME/313-team"
CONFIG="$HOME/.config/313/team.json"
mkdir -p "$ROOT"
if [ ! -f "$CONFIG" ]; then
  echo "313 config missing. Complete the existing 313 Termux setup first."
  exit 1
fi
cp "$REPO_DIR/termux/313_remote_worker.py" "$ROOT/313_remote_worker.py"
chmod 700 "$ROOT/313_remote_worker.py"
python - "$CONFIG" <<'PY'
import json, os, sys, tempfile
p=sys.argv[1]
with open(p, encoding="utf-8") as f: data=json.load(f)
data["remote_bridge_branch"]="feature/313-termux-remote-bridge"
fd,tmp=tempfile.mkstemp(prefix="team-",suffix=".tmp",dir=os.path.dirname(p))
with os.fdopen(fd,"w",encoding="utf-8") as f: json.dump(data,f,ensure_ascii=False,indent=2)
os.chmod(tmp,0o600)
os.replace(tmp,p)
PY
if pgrep -f "[p]ython .*313_remote_worker.py" >/dev/null 2>&1; then
  echo "313 remote bridge is already running."
else
  nohup python "$ROOT/313_remote_worker.py" > "$ROOT/remote-worker.log" 2>&1 < /dev/null &
  echo "$!" > "$ROOT/remote-worker.pid"
  sleep 2
  if kill -0 "$(cat "$ROOT/remote-worker.pid")" 2>/dev/null; then
    echo "313 remote bridge started successfully."
  else
    echo "Bridge did not stay running. Check $ROOT/remote-worker.log"
    exit 1
  fi
fi
echo "Only fixed actions are allowed: health, doctor, fix_cli_dependency."
