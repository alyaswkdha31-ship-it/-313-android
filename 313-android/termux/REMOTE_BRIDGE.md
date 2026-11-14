# 313 Termux Remote Bridge

This bridge lets the 313 team request a small set of fixed maintenance checks through the private GitHub repository. It does **not** provide arbitrary shell access.

## Supported actions
- `health`: checks the local 313 services and OmniRoute model-list endpoint without printing credentials.
- `doctor`: runs `omniroute doctor`.
- `fix_cli_dependency`: installs the missing `node-machine-id` package inside the existing global OmniRoute installation.

The worker reads `313-android/REMOTE_COMMAND.json` every 12 seconds and writes the result to `313-android/REMOTE_RESULT.json`. Commands must be committed to the configured branch. It uses the existing private 313 GitHub token from `~/.config/313/team.json`; no key is copied into the repository.

## First-time activation
After the worker file is installed on the phone, run it once in Termux. Keep the process alive (Termux must not be force-stopped). It only executes the three allowlisted actions above; it never accepts arbitrary shell text, source-file edits, or deletion commands.
