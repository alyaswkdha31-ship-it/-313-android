# 313 Architecture Decisions

## 2026-11-14 — Phone-local execution
Use Termux on the Android phone as the first local execution host. The Android app remains the UI and communicates with the local gateway on 127.0.0.1:3131.

## 2026-11-14 — Free-only inference
The first team backend uses OpenRouter's openrouter/free model router only. It must not include paid models in fallback arrays or switch to a paid endpoint. Free usage is rate-limited and availability can vary.

## 2026-11-14 — GitHub as persistent project memory
GitHub stores project memory, decisions, tasks, and agent review reports. Each review writes to a separate 313/team-review-* branch. The source branch is not modified by read-only review.

## 2026-11-14 — Credentials
GitHub and OpenRouter credentials are entered locally in Termux setup and stored in Termux private storage with restrictive file permissions. The app-to-gateway token is stored encrypted with Android Keystore. No secrets are committed.

## Deferred
- OmniRoute integration is deferred until its model routing can be constrained and verified as free-only.
- Claude and official OpenAI API endpoints are not enabled because free, no-card, no-billing access cannot be guaranteed for the user's accounts.
- Autonomous code editing is deferred until a patch preview, approval, allowlisted command runner, and rollback path are implemented.
