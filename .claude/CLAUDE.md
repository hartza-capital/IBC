## Codebase Memory

`codebase-memory-mcp` builds a semantic knowledge graph of codebases for faster,
smarter code exploration. Its state lives in `~/.cache/codebase-memory-mcp/`
(nothing is stored in the repo itself). Every query tool takes a **required
`project`** argument — a project name, not the current directory.

### At the start of a session

Probe with `index_status` (or `list_projects`). **If this repo is not indexed
yet, run `index_repository(repo_path=.)` first**, then use the graph tools below
for exploration.

### Use these tools first (before grep/glob)

| Tool               | Use For                                                                                                        |
| ------------------ | -------------------------------------------------------------------------------------------------------------- |
| `search_graph`     | Find symbols by name/pattern (`query` for natural language, `name_pattern` for regex, `label`, `file_pattern`) |
| `get_code_snippet` | Get a symbol's source by `qualified_name` (find it first with `search_graph`)                                  |
| `get_architecture` | High-level overview — packages, routes, hotspots, clusters                                                     |
| `trace_path`       | Callers (`direction=inbound`) / callees (`direction=outbound`); add `risk_labels=true` for impact              |
| `search_code`      | Graph-augmented text search (grep enriched with structure)                                                     |
| `detect_changes`   | Blast radius of a git diff (use only for diff-oriented impact)                                                 |

**When spawning Explore agents:** tell them to prefer these graph tools for
faster exploration, passing `project=<name>` on every call.

**For quick lookups in the main session:**

- Use `search_graph` instead of grep for finding symbols
- Use `trace_path` (inbound/outbound) to trace code flow
- Use `trace_path direction=inbound risk_labels=true` before changing a symbol to
  see what's affected
