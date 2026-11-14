# 313 Project Memory

## Purpose
313 is intended to be a phone-first AI project workspace. The user wants to give a project name and task; the system should retrieve the project from GitHub, coordinate free-eligible AI agents, review work, and preserve project context in GitHub.

## User constraints
- Android phone workflow using Termux as the local execution environment.
- 100% free usage only: no payment, no credit card, no top-ups, no paid fallback, and no bypassing provider terms.
- Never put API tokens or credentials in GitHub, source code, APK assets, logs, or ChatGPT messages.
- Keep work on independent feature branches. Never merge to main without explicit approval.
- Report what was actually tested; do not claim success without evidence.

## Current repository
- Repository: alyaswkdha31-ship-it/-313-android
- Current development branch: feature/313-termux-agent-team
- Previous app branch: feature/runcrate-free-only
- Android package: com.thirteenhub.app

## Current implementation status
- Android main chat now routes through the Termux loopback gateway to OpenRouter's `openrouter/free` router; it no longer sends requests to Runcrate.
- The app and Team Console share the encrypted Termux gateway token stored with Android Keystore.
- Termux gateway exposes `/api/diagnostics` to validate GitHub repository access and OpenRouter key validity without consuming a model completion.
- Main chat uses `/api/chat`; team review remains a read-only multi-agent GitHub review using OpenRouter's free-only router.
- Review reports and this memory are saved to a separate review branch.
- Autonomous source-code edits, build execution from the phone gateway, OmniRoute provider integration, and multi-repository selection are not yet implemented.

## Working rules
- Read this file and recent AGENT_LOGS before starting a new task.
- Record decisions and completed work here.
- Store review reports under 313-android/AGENT_LOGS.
- Keep project source changes separate from review/memory commits.


## 2026-11-14 provider failure and remediation
- User supplied a Runcrate HTTP 402 screenshot: `github_link_required`; Runcrate stated free usage starts on 2026-12-03 and suggested adding credit. The user must not add credit or payment details.
- Decision: remove Runcrate from the main chat execution path. Do not infer model availability from a locally saved Runcrate key.
- New main-chat path: Android app -> 127.0.0.1:3131 `/api/chat` -> OpenRouter `openrouter/free` only.
- New diagnostics path: `/api/diagnostics` validates configured GitHub repository access and OpenRouter key using read-only API calls; it does not spend a completion request.
- CI policy tests passed in workflow 43631953587; Android build was still running when this note was written. Physical-phone and live-provider tests remain pending.
