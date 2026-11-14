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
- Android chat screen exists with a Runcrate free-model allowlist and Android Keystore-backed credential storage.
- A Termux Python gateway is being added under 313-android/termux.
- First team phase is read-only multi-agent review via OpenRouter's free-only router.
- Review reports and this memory are saved to a separate review branch.
- Autonomous source-code edits, build execution from the phone gateway, OmniRoute provider integration, and multi-repository selection are not yet implemented.

## Working rules
- Read this file and recent AGENT_LOGS before starting a new task.
- Record decisions and completed work here.
- Store review reports under 313-android/AGENT_LOGS.
- Keep project source changes separate from review/memory commits.
