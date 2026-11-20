# ONYX CONNECT integrations

This directory contains optional client-side integrations. It does not deploy or modify the Cloudflare Worker.

## Architecture

`[User] -> [Interface: Telegram | Custom GPT Actions | MCP] -> [Worker] -> [onyx_connect_worker.py] -> [Android]`

The Cloudflare Worker remains the existing gateway:
`https://onyx-connect.alyaswkdha31.workers.dev`

The gateway accepts authenticated jobs and the Termux worker polls and executes supported job types. The client integrations in this directory only submit jobs and read their status.

## Connection methods

| Method | Requirements | Works from phone? | Works without Plus? |
|---|---|---:|---:|
| Telegram bot | Telegram bot token, authorized chat ID, Python + requests, gateway token | Yes; can run in Termux | Yes |
| Custom GPT Actions | A ChatGPT plan/workspace with GPT creation and Actions enabled; paste the OpenAPI schema and configure Bearer authentication in the GPT editor | Can be configured from a supported ChatGPT web interface; availability may vary by app | No, requires an eligible paid plan/workspace (for example Plus/Team/Enterprise, subject to current plan availability) |
| MCP server | Python 3.10+, `mcp` and `requests`, environment variables, an MCP-compatible client | Can run on a phone, but a compatible client must connect to its stdio process | Does not depend on Plus; this server is intended for ChatGPT Desktop's MCP support or another MCP-compatible client |

**MCP note:** MCP is a separate integration path; do not assume a Plus subscription by itself enables MCP in ChatGPT. Use ChatGPT Desktop with MCP support, or another compatible MCP host.

## Security

**Never put `ONYX_TOKEN` in any source file, documentation containing real credentials, screenshot, Git commit, or public issue.** Supply it only through the environment or the authentication field in the GPT editor. Keep real `.env` files out of Git. Do not share the token in chat.

The Telegram bot rejects messages from any chat ID other than `TELEGRAM_CHAT_ID`. Keep the bot token private as well.

## API contract

- `POST /v1/jobs` with JSON `{"type":"device.info"}` and Bearer authentication submits a job.
- `GET /v1/jobs/{jobId}` reads the job envelope.
- Successful envelopes contain `ok` and `job`; job status is `pending`, `done`, or `error`.
- A missing/invalid token returns HTTP 401. A missing job returns HTTP 404.

Only submit job types supported by the installed Termux worker. A gateway accepting a job does not guarantee the worker implements that type.

## Setup

- Custom GPT Actions: see [actions-setup.md](actions-setup.md).
- MCP: install [mcp-server/requirements.txt](mcp-server/requirements.txt), set `ONYX_BASE` and `ONYX_TOKEN` in the process environment, then launch `mcp-server/onyx_mcp.py` from an MCP host using stdio.
- Telegram: install `requests`, set the four variables in [.env.example](.env.example) in your local environment (do not commit a real .env file), then run `telegram-bot/onyx_telegram.py`.

## API checks

- `./check_api.sh` checks public routing, unauthenticated behavior and CORS without a token.
- `ONYX_TOKEN=... ./check_api_with_token.sh` checks the same routes with optional Bearer authentication. The script prints only HTTP status codes and response lengths; it never prints the token or response body.
