# DAML MCP Server

Model Context Protocol server for DAML/Canton ledger integration.

## Prerequisites

- JDK 17+
- sbt 1.10+

## Project structure

```
modules/
├── ledger-client/   # Canton Ledger API client
├── mcp-tools/       # MCP tool definitions (create, exercise, query, etc.)
├── mcp-resources/   # MCP resource definitions (contracts, templates, parties)
├── mcp-prompts/     # MCP prompt templates
└── server/          # Entry point, wiring, configuration
```

## Build & run

```bash
sbt compile        # compile all modules
sbt server/run     # run the MCP server
sbt test           # run tests
```
