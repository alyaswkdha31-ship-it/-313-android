# 313 Team Tasks

## Current phase
- [x] Create independent Termux team feature branch.
- [x] Add loopback-only Python gateway foundation.
- [x] Add Android Team Console screen and encrypted gateway-token storage.
- [x] Add GitHub read-only project review and persistent report/memory branch workflow.
- [x] Run Python policy tests and Android debug APK CI build successfully (workflow run 43629920804).
- [x] Remove Runcrate from the main-chat request path after confirmed HTTP 402 GitHub-link eligibility block.
- [x] Route main chat through Termux to OpenRouter `openrouter/free` only.
- [x] Add read-only `/api/diagnostics` for GitHub and OpenRouter credential checks.
- [x] Add Team Console diagnostics UI and shared encrypted gateway-token use.
- [x] Add policy tests for free-only chat and provider diagnostics.
- [ ] Verify latest workflow 43631953587 completes and retrieve its APK artifact.
- [ ] Update Termux local gateway on the phone and run `/api/diagnostics`.
- [ ] Test one main-chat prompt and one project-team review with the user's real local credentials.
- [ ] Install and test on the user's Android phone with Termux.
- [ ] Add approved patch proposal and diff review flow.
- [ ] Add user-approved source-file application on a dedicated branch.
- [ ] Run project builds/tests from Termux with explicit command allowlists.
- [ ] Add safe multi-repository selection.
- [ ] Evaluate OmniRoute integration only with a verifiable free-only route and no paid fallback.

## Acceptance criteria
- The user can ask 313 to review a configured GitHub project without uploading files again.
- The review reads persistent project memory and saves a report to GitHub.
- At least two independent reviewers run concurrently; failures are visible.
- The service refuses requests when free capacity or provider access is unavailable.
- No operation changes the source branch or merges without user approval.
