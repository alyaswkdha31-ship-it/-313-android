#!/usr/bin/env python3
"""313 safe remote command bridge. Only fixed, allowlisted maintenance actions."""
import base64, json, os, subprocess, time, urllib.error, urllib.parse, urllib.request
from pathlib import Path

CONFIG = Path.home() / ".config/313/team.json"
COMMAND_PATH = "313-android/REMOTE_COMMAND.json"
RESULT_PATH = "313-android/REMOTE_RESULT.json"
API = "https://api.github.com"
POLL_SECONDS = 12
MAX_OUTPUT = 5000

def request(url, token, payload=None, method=None):
    headers = {"Authorization": "Bearer " + token, "Accept": "application/vnd.github+json",
               "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "313-Termux-Remote-Bridge"}
    raw = json.dumps(payload).encode() if payload is not None else None
    if payload is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=raw, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=25) as response:
        return json.loads(response.read().decode())

def api_url(repo, path, branch):
    return API + "/repos/" + urllib.parse.quote(repo, safe="/") + "/contents/" + urllib.parse.quote(path, safe="/") + "?ref=" + urllib.parse.quote(branch, safe="")

def get_file(repo, path, branch, token):
    data = request(api_url(repo, path, branch), token)
    content = base64.b64decode(data.get("content", "")).decode("utf-8")
    return json.loads(content), data.get("sha")

def put_file(repo, path, branch, token, content, sha, message):
    url = API + "/repos/" + urllib.parse.quote(repo, safe="/") + "/contents/" + urllib.parse.quote(path, safe="/")
    payload = {"message": message, "branch": branch,
               "content": base64.b64encode(json.dumps(content, ensure_ascii=False, indent=2).encode()).decode()}
    if sha:
        payload["sha"] = sha
    return request(url, token, payload, "PUT")

def run_fixed(action, command=None, cfg=None):
    command = command or {}
    cfg = cfg or {}
    if action == "chat":
        messages = command.get("messages")
        if not isinstance(messages, list) or not messages or len(messages) > 20:
            return {"ok": False, "error": "Invalid chat messages; expected 1-20 messages."}
        clean = []
        for item in messages:
            if not isinstance(item, dict) or item.get("role") not in ("user", "assistant"):
                return {"ok": False, "error": "Invalid chat message role."}
            content = item.get("content")
            if not isinstance(content, str) or not content.strip() or len(content) > 12000:
                return {"ok": False, "error": "Invalid chat message content."}
            clean.append({"role": item["role"], "content": content})
        gateway_token = str(cfg.get("gateway_token") or "").strip()
        if not gateway_token:
            return {"ok": False, "error": "313 gateway token is not configured."}
        payload = json.dumps({"messages": clean}).encode("utf-8")
        req = urllib.request.Request(
            "http://127.0.0.1:3131/api/chat", data=payload,
            headers={"Content-Type": "application/json", "X-313-Token": gateway_token},
            method="POST")
        try:
            with urllib.request.urlopen(req, timeout=170) as response:
                data = json.loads(response.read().decode("utf-8"))
            if not data.get("ok"):
                return {"ok": False, "error": str(data.get("error", "Chat request failed"))[:500]}
            return {"ok": True, "answer": str(data.get("answer", ""))[:MAX_OUTPUT],
                    "provider": data.get("provider", "OmniRoute local gateway"),
                    "paid_fallback": data.get("paid_fallback", False)}
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", errors="replace")[:500]
            return {"ok": False, "error": "Gateway HTTP " + str(e.code) + ": " + detail}
        except Exception as e:
            return {"ok": False, "error": type(e).__name__ + ": " + str(e)[:300]}
    elif action == "doctor":
        cmd = ["omniroute", "doctor"]
    elif action == "fix_cli_dependency":
        npm_root = subprocess.check_output(["npm", "root", "-g"], text=True, timeout=15).strip()
        package_dir = str(Path(npm_root) / "omniroute")
        cmd = ["npm", "install", "--prefix", package_dir, "node-machine-id"]
    elif action == "health":
        return {"ok": True, "checks": {
            "313_server_3131": probe("http://127.0.0.1:3131/health"),
            "313_server_3132": probe("http://127.0.0.1:3132/health"),
            "omniroute_models": probe_omni()
        }}
    else:
        return {"ok": False, "error": "Action is not in the allowlist."}
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, timeout=180)
        output = (p.stdout + "\\n" + p.stderr)[-MAX_OUTPUT:]
        output = output.replace(str(Path.home()), "~")
        return {"ok": p.returncode == 0, "exit_code": p.returncode, "output": output}
    except Exception as e:
        return {"ok": False, "error": type(e).__name__ + ": " + str(e)[:500]}

def probe(url):
    try:
        with urllib.request.urlopen(url, timeout=3) as r:
            return {"reachable": True, "status": r.status}
    except Exception as e:
        return {"reachable": False, "error": type(e).__name__}

def probe_omni():
    try:
        keyfile = Path.home() / ".omniroute-key"
        if not keyfile.exists():
            return {"reachable": False, "reason": "local key file missing"}
        req = urllib.request.Request("http://127.0.0.1:20128/v1/models",
            headers={"Authorization": "Bearer " + keyfile.read_text().strip()})
        with urllib.request.urlopen(req, timeout=5) as r:
            data = json.loads(r.read().decode())
            return {"reachable": True, "models_visible": len(data.get("data", []))}
    except Exception as e:
        return {"reachable": False, "error": type(e).__name__}

def main():
    if not CONFIG.exists():
        raise SystemExit("313 config not found. Run the 313 Termux setup first.")
    cfg = json.loads(CONFIG.read_text())
    repo = cfg.get("github_repo")
    branch = cfg.get("remote_bridge_branch") or cfg.get("github_branch")
    token = cfg.get("github_token")
    if not repo or not branch or not token:
        raise SystemExit("GitHub repo, bridge branch, or token missing in private 313 config.")
    print("313 remote bridge active. Only allowlisted actions: health, doctor, fix_cli_dependency, chat.")
    while True:
        try:
            command, command_sha = get_file(repo, COMMAND_PATH, branch, token)
            if command.get("status") == "pending":
                command_id = str(command.get("id", ""))[:80]
                action = str(command.get("action", ""))
                print("Executing approved maintenance action:", action)
                result = {"id": command_id, "action": action, "completed_at": int(time.time()),
                          "result": run_fixed(action, command, cfg)}
                try:
                    _, result_sha = get_file(repo, RESULT_PATH, branch, token)
                except Exception:
                    result_sha = None
                put_file(repo, RESULT_PATH, branch, token, result, result_sha,
                         "313 bridge: publish command result " + command_id)
                command["status"] = "completed"
                command["completed_at"] = int(time.time())
                put_file(repo, COMMAND_PATH, branch, token, command, command_sha,
                         "313 bridge: acknowledge command " + command_id)
                print("Completed:", action)
        except urllib.error.HTTPError as e:
            if e.code != 404:
                print("Bridge API error:", e.code)
        except Exception as e:
            print("Bridge cycle error:", type(e).__name__, str(e)[:180])
        time.sleep(POLL_SECONDS)

if __name__ == "__main__":
    main()
