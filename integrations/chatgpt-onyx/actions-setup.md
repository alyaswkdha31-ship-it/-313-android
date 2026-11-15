# Custom GPT Actions setup

1. Open ChatGPT and go to **Explore GPTs → Create**.
2. Open **Configure → Actions → Create new action**.
3. Paste the contents of [openapi.yaml](openapi.yaml) into the Schema field.
4. Under **Authentication**, choose **API Key** and set **Auth Type** to **Bearer**. Paste your ONYX_TOKEN into the authentication field in the ChatGPT interface only.
5. Save the GPT and test it from **Preview** with a job type supported by your Termux worker, such as `device.info`.

## Important

- Never paste the token into this document, the OpenAPI schema, a prompt, or a Git file. Enter it only in the protected authentication UI.
- GPT creation and Actions availability depend on the current ChatGPT plan and workspace settings. Eligible paid plans/workspaces such as Plus, Team, or Enterprise may support this feature; check the capabilities shown in your account.
- This is the Custom GPT Actions route. It is not the MCP route. A Plus plan alone should not be assumed to enable MCP; use ChatGPT Desktop with MCP support for the MCP server.
- The gateway can accept a job even if the local worker does not implement its type. Use only supported job types.
