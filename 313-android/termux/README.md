# 313 Team Gateway for Termux

The 313 Android app talks to this phone-local Python gateway on `127.0.0.1:3131`. The gateway uses the user's existing local OmniRoute service on `127.0.0.1:20128` and reads the configured GitHub repository directly.

## Implemented

- Reads the configured GitHub repository and branch; no repeated source-file uploads are needed.
- Skips build output, binaries, and likely secret files when collecting project context.
- Routes 313 chat and the three independent review roles through the local OmniRoute OpenAI-compatible API.
- Uses OmniRoute's `auto` route and records the actual provider/model/cost headers when OmniRoute returns them.
- Runs the three review roles concurrently, then asks OmniRoute for a synthesis.
- Creates a separate `313/team-review-*` branch and saves the report under `313-android/AGENT_LOGS/`.
- Appends review history to `313-android/PROJECT_MEMORY.md`.
- Does not modify source code, merge branches, or run project build commands.

## Strict zero-cost protection

**Do not use 313 inference until OmniRoute's `freeAccessPolicy` is set to `strict`.**

OmniRoute documents this strict policy as a fail-closed routing guard: it only trusts free-access entries with an independently documented hard stop. If the free pool is empty or exhausted, the request fails rather than stepping up to a paid route. This is intentionally stricter than merely selecting a model whose catalogue price is shown as zero.

The setup script asks the operator to type `YES` after enabling strict free-access mode in OmniRoute. If the operator does not confirm, 313 blocks all chat and review inference. This is an explicit operator confirmation, not an automatic remote verification of OmniRoute's settings.

Important:
- OmniRoute itself is a free/open-source gateway, but the providers behind it have their own terms, quotas, and eligibility.
- A model appearing in the OmniRoute catalogue does not prove that its current account route is free.
- Free availability is not unlimited. If a provider rejects a request, reaches quota, or has no eligible strict-free route, 313 reports failure and does not switch to OpenRouter or another paid service.
- The team roles are separate review passes. OmniRoute may route more than one role to the same provider/model; the returned provider/model headers are recorded so this is visible.
- ChatGPT in this current ChatGPT conversation is not an app API credential. 313 cannot call this GPT-6 session as a hidden backend provider. It can be used as a reviewer when the user shares the report here.

## Setup / upgrade

1. Install or start OmniRoute in Termux. Its local API should listen on `http://127.0.0.1:20128`.
2. In OmniRoute, create/copy an API key from Dashboard → Endpoints.
3. Enable `freeAccessPolicy = strict` in OmniRoute before allowing inference.
4. Clone this branch or update an existing checkout:
   ```bash
   git clone --branch feature/313-real-provider-team https://github.com/alyaswkdha31-ship-it/-313-android.git 313-source
   cd ~/313-source/313-android/termux
   bash setup-termux.sh
   ```
5. The script preserves existing GitHub settings and gateway token. It reuses `~/.omniroute-key` if present; otherwise it asks for the OmniRoute key without echoing it.
6. Type `YES` only if strict free-access mode is enabled in OmniRoute. Otherwise 313 keeps inference blocked.
7. Start the gateway:
   ```bash
   python ~/313-team/313_team_server.py
   ```
8. If the script printed a new `GATEWAY_TOKEN`, paste it into 313 → Team settings. Existing tokens are preserved.
9. Check `http://127.0.0.1:3131/health`.

## Credentials

- GitHub credentials and the 313 gateway token remain in `~/.config/313/team.json` with mode `0600`.
- The OmniRoute API key is read from `~/.omniroute-key` with mode `0600`.
- Secrets are not committed to GitHub or embedded in the APK. Never paste keys into ChatGPT or a project report.

## API

- `GET /health`: gateway process health.
- `GET /api/status`: configuration status without returning credentials.
- `GET /api/diagnostics`: checks GitHub access and authenticated OmniRoute `GET /v1/models`. This does not consume a model completion and does not prove a model can answer.
- `POST /api/chat`: sends chat to OmniRoute's configured model (default `auto`), only after strict-free confirmation.
- `POST /api/team/review`: reads the configured repository, runs three review roles concurrently, and saves the report/memory to a new GitHub branch.

The repository name is locked to the repository configured during setup. A request cannot switch to another repository.

## Current limitations

- This is a read-only multi-agent review and GitHub memory writer, not autonomous source-code editing.
- The Android app calls the Termux gateway over loopback. The gateway token is stored using Android Keystore-backed encryption.
- The Termux service must be started manually; boot startup is not configured.
- GitHub fine-grained token needs Contents read/write for the intended repository.
- OmniRoute's free providers and quotas can change. Provider-level success is only confirmed by a real completion; the diagnostic endpoint checks API reachability/model visibility only.
- If OmniRoute does not return provider/model/cost headers, the report records that metadata as unavailable rather than guessing.
- This gateway does not provide a direct GPT-6 API connection. ChatGPT can review results in the current conversation, but the app cannot invoke this session as a backend model.

## Security

The gateway binds only to loopback, never `0.0.0.0`. It does not log request bodies or authorization headers. Do not expose port 3131 or OmniRoute's port 20128 through a public tunnel.
