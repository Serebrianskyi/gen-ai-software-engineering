# How To Run

## Prerequisites

- Node.js 18+ and npm
- Python 3.10+
- Claude Code CLI

---

## 1. Install Dependencies

### Custom MCP server (Python)

```bash
cd homework-5/custom-mcp-server
pip3 install -r requirements.txt
```

### External MCP servers (Node) — installed on first use via npx

GitHub and Filesystem servers are installed automatically by `npx` the first time they run. No manual install needed.

---

## 2. Configure Credentials

`mcp.json` is **gitignored** — it holds your real tokens and is never committed.
`mcp.example.json` is the committed template showing the required structure.

```bash
cp homework-5/mcp.example.json homework-5/mcp.json
```

Then edit `mcp.json` and fill in the placeholders:

| Placeholder | Real value | Where to get it |
|-------------|-----------|----------------|
| `YOUR_GITHUB_PERSONAL_ACCESS_TOKEN` | `github_pat_...` | GitHub → Settings → Developer settings → Personal access tokens (needs `repo` scope) |
| `YOUR_NOTION_API_TOKEN` | `secret_...` | Notion → Settings → Connections → Develop or manage integrations |
| `/path/to/your/project` | absolute path to this repo | e.g. `/Users/you/IdeaProjects/ai-workshops` |
| `/path/to/your/homework-5/custom-mcp-server/server.py` | absolute path to server.py | e.g. `/Users/you/IdeaProjects/ai-workshops/homework-5/custom-mcp-server/server.py` |

---

## 3. Connect MCP Configuration to Claude Code

Copy or symlink `mcp.json` to your project root as `.mcp.json`:

```bash
cp homework-5/mcp.json .mcp.json
```

Or add the servers directly via Claude Code CLI:

```bash
# GitHub
claude mcp add github -e GITHUB_PERSONAL_ACCESS_TOKEN=your_token -- npx -y @modelcontextprotocol/server-github

# Filesystem
claude mcp add filesystem -- npx -y @modelcontextprotocol/server-filesystem /path/to/directory

# Notion
claude mcp add notion -e OPENAPI_MCP_HEADERS='{"Authorization": "Bearer your_token", "Notion-Version": "2022-06-28"}' -- npx -y @notionhq/notion-mcp-server

# Custom Lorem Ipsum
claude mcp add custom-lorem-ipsum -- python3 /absolute/path/to/homework-5/custom-mcp-server/server.py
```

---

## 4. About the Custom MCP Server

**Do not run `server.py` directly** — it speaks JSON-RPC over stdio, not plain text, so typing into it will produce errors.

Claude Code starts and stops the server automatically based on the MCP config. To verify it starts without errors:

```bash
python3 homework-5/custom-mcp-server/server.py 2>&1 | head -5
# Should print the FastMCP banner to stderr, then wait — Ctrl+C to exit
```

---

## 5. Test the `read` Tool

Once Claude Code has the `custom-lorem-ipsum` server configured and running, use it in a Claude Code session:

```
Use the read tool with word_count=10
```

Expected output: the first 10 words from `lorem-ipsum.md`.

```
Use the read tool with word_count=50
```

Expected output: the first 50 words from `lorem-ipsum.md`.

You can also read the resource directly:

```
Read the resource lorem://text/20
```

---

## 6. Verify All Servers Are Running

In Claude Code, run:

```
/mcp
```

All four servers (`github`, `filesystem`, `notion`, `custom-lorem-ipsum`) should show as connected.