# 313 Team Gateway for Termux

This is the first phone-local backend slice for the 313 multi-agent project workflow.
It uses Python's standard library only and listens on 127.0.0.1:3131.

## Implemented in this phase

- Reads the configured GitHub repository directly; no repeated file uploads are needed.
- Reads text source and project notes from the configured branch while skipping build output, binaries, and likely secret files.
- Runs three independent review roles concurrently through OpenRouter's openrouter/free router.
- Produces a lead summary from the independent reports.
- Creates a separate 313/team-review-* branch.
- Saves the report under 313-android/AGENT_LOGS/ and appends review history to 313-android/PROJECT_MEMORY.md.
- Does not modify source code, merge branches, or run project build commands in this first phase.

## Free-only guarantee

The only model target currently enabled is OpenRouter's openrouter/free router. Its documented contract routes only to free models. There is no paid-model fallback list in this gateway. If the provider rejects the request, is rate-limited, or has no free capacity, the task fails rather than switching to a paid model.

The OpenRouter free tier is rate-limited and its available models can change. Free availability is not unlimited. No credit purchase, top-up, billing setup, or card is part of this project.

OmniRoute is not auto-enabled yet. Its model catalogue can include paid routes, so it must not be trusted as free merely because it appears in /v1/models. An optional OmniRoute adapter will only be added after its exact free-only routing contract is verified and explicitly configured.

## Setup

1. Install Termux from its official F-Droid or GitHub distribution.
2. In Termux, clone this repository and enter its Termux folder:
   git clone --branch feature/313-termux-agent-team https://github.com/alyaswkdha31-ship-it/-313-android.git 313-source
   cd ~/313-source/313-android/termux
3. Run:
   bash setup-termux.sh
4. During setup, provide:
   - A GitHub fine-grained personal access token restricted to the single repository, with Contents read/write.
   - An OpenRouter API key from a free account. Do not add credits or enable paid billing.
5. Start the service with:
   python ~/313-team/313_team_server.py
6. Open 313 → فريق المشاريع → إعداد, then paste the one-time gateway token printed by setup.
7. Check http://127.0.0.1:3131/health locally.

Credentials are stored only in Termux's private home directory in ~/.config/313/team.json with mode 0600. Never commit this file, paste credentials into ChatGPT, or share it.

## API

- GET /health: process health.
- GET /api/status: configuration status (never returns credentials).
- POST /api/team/review with JSON:
  {"task":"Review project 313 for build and runtime defects"}

The repository name is locked to the repository configured during setup. A request cannot switch to another repository.

## Current limitations

- This phase is a read-only multi-agent review and GitHub memory writer, not autonomous source-code editing.
- The Android app now includes a Team Console screen that calls this local API over loopback. The gateway token is stored in Android Keystore-backed encryption.
- The server must be started in Termux; persistent boot startup is not configured.
- GitHub fine-grained token setup is a one-time manual step. It must be limited to the intended repository.
- OpenRouter's openrouter/free may select different free models on different requests. It does not guarantee Claude or official GPT models.
- A failed agent is reported; the gateway does not silently retry against a paid model.

## Security notes

The server binds only to loopback, never 0.0.0.0. It does not log request bodies or authorization headers. The current gateway is intended for one trusted device user. Do not expose port 3131 through a public tunnel.
