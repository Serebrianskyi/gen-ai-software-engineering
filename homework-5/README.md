# Homework 5: Configure MCP Servers

**Author: Roman Serebrianskyi**

## Description

This homework configures four MCP (Model Context Protocol) servers to extend Claude Code with external integrations:

1. **GitHub MCP** — connects Claude to GitHub to list PRs, summarize commits, and manage issues
2. **Filesystem MCP** — gives Claude read/write access to a local directory
3. **Notion MCP** — connects Claude to a Notion workspace to query pages and databases
4. **Custom Lorem Ipsum MCP** — a custom FastMCP server that exposes a `read` tool and a `lorem://text/{word_count}` resource

### Resources vs Tools

- **Resources** are URIs that Claude can read from (e.g., files, APIs). They follow a URI scheme and can be templated with parameters. Example: `lorem://text/50` returns 50 words from the lorem ipsum file.
- **Tools** are actions Claude can call to perform operations (e.g., reading a file, running a command). They accept typed parameters and return a result. Example: the `read` tool accepts an optional `word_count` and returns the corresponding words.

## Structure

```
homework-5/
├── README.md                          # This file
├── HOWTORUN.md                        # Setup and usage instructions
├── mcp.example.json                   # Template MCP config (committed)
├── mcp.json                           # Real MCP config with tokens (gitignored)
├── custom-mcp-server/
│   ├── server.py                      # Custom FastMCP server
│   ├── lorem-ipsum.md                 # Source text for the resource
│   └── requirements.txt               # Python dependencies (fastmcp)
└── screenshots/
    ├── allmcps.png
    ├── fastmcp.png
    ├── filesystem.png
    ├── gitmcp.png
    └── notionmcp.png
```