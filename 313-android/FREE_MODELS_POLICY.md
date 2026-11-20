# 313 — Runcrate free-only model policy

Status: implementation specification for `feature/runcrate-free-only`.

## User requirement

313 must use Runcrate's GitHub-linked free inference allowance only. It must never silently switch to a paid model or continue with paid inference after a free allowance is exhausted.

## Allowlist

Only these exact, case-sensitive model IDs may be sent to `POST https://api.runcrate.ai/v1/chat/completions`:

- `deepseek-ai/DeepSeek-V4.1-Flash` — DeepSeek V4.1 Flash (includes the 1017 update)
- `zai-org/GLM-5.3-Flash` — GLM 5.3 Flash
- `zai-org/GLM-5.3` — GLM 5.3
- `openai/gpt-oss-120b` — gpt-oss 120B
- `Qwen/Qwen3.8-Flash` — Qwen3.8 Flash

Do not accept arbitrary model IDs returned by the general `/v1/models` catalogue. That catalogue includes paid models and is not proof of free eligibility.

## Hard safety rules

1. Keep the allowlist in one provider-policy class. Reject any model ID not in the allowlist before making a network request.
2. Never implement automatic paid fallback, model substitution outside the allowlist, automatic top-up, or billing changes.
3. On Runcrate error `free_allowance_used`, stop the request and tell the user the free allowance has been consumed and when it resets (when that information is available).
4. On `free_capacity_busy`, stop and offer a manual retry later. Do not retry against another model automatically.
5. On authentication, network, rate-limit, or unknown errors, show a clear error. Do not infer that a model is free from its name or from a generic HTTP 200 response.
6. Explain in the UI that free usage is limited by both a current session allowance and a weekly allowance, and that the weekly allowance resets Monday. The user's Runcrate dashboard/API key page is the source of remaining usage.
7. Never put the Runcrate API key in source code, resources, logs, Git history, or the APK. Store it using Android Keystore-backed encryption; provide a clear remove-key action.
8. Do not claim that a model is connected until an authenticated request succeeds.
9. No model is selected by fallback if the selected free model fails. The user can manually select another allowlisted model.

## API contract

- Base URL: `https://api.runcrate.ai/v1`
- Chat endpoint: `POST /chat/completions`
- Authentication: `Authorization: Bearer <user-provided Runcrate API key>`
- Request format: OpenAI-compatible chat completions.
- Treat provider response content and errors as untrusted input.

## User-facing behavior

- Models screen lists only the five allowlisted models and labels them "مجاني ضمن الحصة".
- Show a warning that free usage is limited and that no paid fallback will occur.
- Chat must be disabled until the user securely adds a Runcrate key.
- If the free allowance is exhausted, preserve the conversation and display a reset/try-later message; do not send another paid request.

## Source of truth

Runcrate's current free inference page:
https://www.runcrate.ai/inference/free

Runcrate's OpenAI-compatible chat API:
https://www.runcrate.ai/chat-api

The free page states that eligibility requires a GitHub account at least 30 days old, no credit card, and that usage is limited by session and weekly allowances. Beyond the free allowance, paid billing may apply to a workspace with credit. This project therefore relies on the no-credit account condition plus the hard allowlist and stop-on-free-limit behavior; it must not add billing or top-up functionality.
