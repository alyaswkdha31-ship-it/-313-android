# 313 development roadmap

## Milestone 0 — source baseline
- [x] Add the existing Android starter project to GitHub.
- [x] Document that provider cards are currently website shortcuts, not connected APIs.
- [ ] Verify a clean Android build in CI.

## Milestone 1 — redesign
- Replace the current simple dashboard with a premium Arabic-first command-center layout.
- Add bottom navigation for Chat, Models, Team, Projects, and Settings.
- Add accessible states for loading, errors, offline mode, and empty history.
- Keep provider connection state truthful.

## Milestone 2 — provider gateway
- Define a provider-neutral chat interface and model catalogue.
- Add OpenAI, Anthropic, and Gemini adapters behind a backend or secure credential boundary.
- Support user-managed credentials and OAuth where officially supported.
- Add streaming responses, cancellation, timeouts, retries, and usage/error reporting.
- Never embed secrets in the Android client.

## Milestone 3 — team orchestration
- Implement explicit task planning, worker execution, independent review, and final synthesis.
- Show which model performed each step and preserve the transcript/audit trail.
- Add user approval gates for external writes and costly operations.
- Support fallback when a provider is unavailable.

## Milestone 4 — GitHub projects
- Authenticate through GitHub OAuth/device flow with minimal scopes.
- List authorized repositories and inspect branches, commits, issues, and pull requests.
- Propose diffs before writing; require confirmation before push/PR/merge.
- Add tests for permissions, error handling, and stale branch state.

## Milestone 5 — Linux workspace
- Design a secure, opt-in bridge to a user-controlled Linux host.
- Use scoped commands, visible execution logs, and confirmation for destructive actions.
- Never expose a shell or credentials to model output without policy checks.

## Immediate next task
Start with Milestone 1: propose a new screen architecture and implement the redesigned dashboard. Do not imply that API/team integrations already exist. Keep changes small, buildable, and documented.
