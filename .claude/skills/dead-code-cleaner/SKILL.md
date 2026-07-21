---
name: dead-code-cleaner
description: >
  Automatically detect and remove dead code from any project by analyzing the
  codebase and dispatching to the appropriate language expert agent. Use when
  user asks to "clean dead code", "remove unused code", "find dead code",
  "nettoie le code mort", "supprime le code inutilisé", or "cleanup unused".
  Supports Go, Python, Rust, TypeScript/React, and Java projects.
allowed-tools: Read, Glob, Grep, Bash, Workflow, mcp__codebase-memory-mcp__index_status, mcp__codebase-memory-mcp__list_projects, mcp__codebase-memory-mcp__index_repository, mcp__codebase-memory-mcp__search_graph, mcp__codebase-memory-mcp__trace_path, mcp__codebase-memory-mcp__get_code_snippet
---

# Dead Code Cleaner

Automatically detect project languages and dispatch to expert agents for dead
code cleanup.

---

## Execution Budget

| Resource                   | Limit  | On exceeded                                          |
| -------------------------- | ------ | ---------------------------------------------------- |
| Total workflow steps       | 20     | Stop, report progress, ask user to continue or abort |
| Per-agent timeout          | 10 min | Mark language as timed out, report partial findings  |
| Dead code items to process | 100    | Truncate to top 100 by confidence, note truncation   |

Agent concurrency (how many run at once) is capped automatically by the
`Workflow` tool — do not hand-roll a "N max, queue the rest" batching rule
here; pass every detected language to `parallel()` in one call and let the
tool schedule them.

If any limit is reached: report completed languages, pending languages, and ask whether to continue or stop.

---

## ⚠️ CRITICAL: No Speculation Policy

**DO NOT INVENT DEAD CODE OR REMOVAL STRATEGIES.**

Follow this policy strictly:

1. **Base on verified analysis**: Use language-specific tools (cargo clippy, knip, go vet) to identify dead code
2. **If uncertain about safety**: Use WebSearch to find best practices for dead code removal in the specific language
3. **If still uncertain**: Mark code as potentially dead but do NOT remove it without verification
4. **NEVER**: Guess which code is unused, remove code without tool verification, or invent cleanup strategies

This applies to ALL expert agents launched. Each agent must verify dead code through tools before removal.

---

## ⚠️ CRITICAL: No Uncommitted Work Policy

**DO NOT COMMIT WORK UNLESS EXPLICITLY REQUESTED BY THE USER.**

Follow this policy strictly:

1. **Never auto-commit**: This skill should NEVER create git commits automatically
2. **User must request it**: Only commit when the user explicitly asks with phrases like "commit this", "create a commit"
3. **Prepare but don't commit**: Remove dead code and inform the user, then ask if they want to commit
4. **NEVER**: Commit as part of the workflow, commit "to save progress", or commit without explicit user approval

This applies to ALL expert agents launched. Agents must never commit their changes automatically.

---

## Workflow

### Step 1: Detect Project Languages

Analyze the project structure to identify all languages present.

**Detection patterns:**

1. Use Glob to find language-specific files:
   - Go: `go.mod`, `**/*.go`
   - Python: `requirements.txt`, `pyproject.toml`, `setup.py`, `**/*.py`
   - Rust: `Cargo.toml`, `**/*.rs`
   - TypeScript/React: `package.json`, `tsconfig.json`, `**/*.ts`, `**/*.tsx`
   - Java: `pom.xml`, `build.gradle`, `**/*.java`

2. Build a list of detected languages and their confidence level.

3. If no languages detected, ask the user to specify the project type.

**Output:** List of languages to process (e.g., `["rust", "typescript"]`)

---

### Step 1b: Graph Pre-Analysis (codebase-memory-mcp)

Before dispatching agents, build a dead-code candidate list from the knowledge
graph.

**Resolve & index the project:**

Call `list_projects` and find the entry matching this repo. If none matches,
run `index_repository(repo_path=".")` — this indexes the repo and returns its
derived project **name**. Every graph call below takes a required
`project=<name>` argument.

**Build the candidate list:**

1. Use `search_graph(project=<name>, label="Function")` to enumerate functions.
   Prefer `min_degree`/`max_degree` filters: functions with in-degree 0 are the
   primary dead-code candidates — page with `offset`/`limit` while `has_more`.

2. Confirm each zero-in-degree function with
   `trace_path(project=<name>, function_name=symbol, direction=inbound)` — an
   empty inbound trace means no callers.

3. Filter out false positives before passing to agents:
   - Entry points: `main`, `init`, Lambda handlers, HTTP handlers, CLI commands
   - Exported symbols in public packages / API surfaces (flag as "verify" not "remove")
   - Test functions and test helpers
   - Symbols annotated with framework decorators (`@Override`, `#[tauri::command]`, `@Bean`, etc.)

4. For each remaining candidate, call
   `trace_path(project=<name>, function_name=symbol, direction=outbound)` to find
   its transitive dead subtree — code that would also become dead once the
   candidate is removed (cascade analysis).

5. Produce a **graph candidate list** structured as:

   ```
   ZERO-CALLERS (safe to remove):
   - pkg/utils.formatInternal (Go, internal, 0 callers)
   - src/helpers/unused.ts (TypeScript, not exported, 0 callers)

   CASCADE DEAD (dead if parent removed):
   - pkg/utils.helperForFormatInternal (only called by formatInternal)

   EXPORTED-VERIFY (exported, 0 external callers — confirm before removing):
   - pkg/api.LegacyEndpoint (exported, 0 callers in this repo)
   ```

6. Pass this list to agents in Step 2 as a **starting point** — agents must still verify with language tools but focus their effort on these candidates first.

**If the graph server is unavailable:**

Skip this step. Agents will rely on language-specific tools only (go vet, knip, cargo clippy, etc.).

---

**Call `advisor()` before Step 2** — With the detected languages, project structure, and graph candidates (if any) in your context, call `advisor()` to validate the cleanup strategy and tool selection before dispatching agents.

---

### Step 2: Dispatch to Expert Agents (via Workflow)

Run one `Workflow` call that fans out one cleanup agent per detected
language, all in parallel, in a single tool call — the tool handles fan-out
concurrency and result collection, so there is no manual "N max, queue the
rest" batching rule to apply here.

**Agent mapping:**

| Language         | Agent           |
| ---------------- | --------------- |
| Go               | `go-expert`     |
| Python           | `python-expert` |
| Rust             | `rust-expert`   |
| TypeScript/React | `react-expert`  |
| Java             | `java-expert`   |

**Instructions to provide to each agent:**

See `references/agent-dispatch-guide.md` for detailed instructions per
language — the per-language prompt below tells the agent to follow that
guide's steps verbatim.

**Key points for agents (baked into every prompt):**

- **Advisor tool:** the agent has access to an `advisor` tool backed by a stronger reviewer model. Call `advisor()` before removing code (after the analysis phase), and when the cleanup is believed complete. Give the advice serious weight.
- **Graph candidates:** if Step 1b produced a candidate list, it is passed to the agent as a prioritized starting point. The agent must still verify each item with language tools before removing.
- Use language-specific tools (cargo clippy, knip, go vet, etc.)
- Use codebase-memory-mcp tools (`trace_path direction=inbound` for callers, add `risk_labels=true` for impact) when the repo is indexed — these provide cross-file call graph verification that language tools miss
- Identify unused code: functions, imports, types, files, dependencies
- Remove dead code safely (verify before deleting)
- Never auto-commit

Call the `Workflow` tool with
`args: { languages: [<detected languages>], candidates: <Step 1b candidate list or null> }`
and this script:

```js
export const meta = {
  name: "dead-code-cleaner-cleanup",
  description: "Analyze and remove dead code per detected language",
  phases: [{ title: "Cleanup" }],
}

const AGENT_MAP = {
  Go: "go-expert",
  Python: "python-expert",
  Rust: "rust-expert",
  "TypeScript/React": "react-expert",
  Java: "java-expert",
}

function cleanupPrompt(language, candidates) {
  return `Analyze and remove dead code in this ${language} project.

Follow the detailed steps for ${language} in
skills/dead-code-cleaner/references/agent-dispatch-guide.md exactly — tool
commands to run, what counts as dead code, and the safe-removal order.

No Speculation Policy: base every removal on verified analysis from
language-specific tools (and codebase-memory-mcp graph tools when available).
If uncertain about safety, mark the code as potentially dead but do NOT remove
it without verification. Never guess which code is unused.

Advisor tool: call advisor() before removing code (after the analysis
phase), and again when you believe the cleanup is complete. Give the advice
serious weight.

${
  candidates
    ? `Graph candidate list from the orchestrator's pre-analysis (starting
point only — verify every item with language tools before removing):
${JSON.stringify(candidates)}`
    : "No graph candidate list was produced — rely on language-specific tools only."
}

Never auto-commit. Leave changes in the working tree for the user to review.`
}

phase("Cleanup")
const results = await parallel(
  args.languages.map(
    (language) => () =>
      agent(cleanupPrompt(language, args.candidates), {
        label: `cleanup:${language}`,
        agentType: AGENT_MAP[language],
        phase: "Cleanup",
      })
  )
)

return args.languages.map((language, i) => ({ language, result: results[i] }))
```

A `results[i]` of `null` means that language's agent failed or timed out;
report it in Step 4 rather than silently dropping it.

---

### Step 3: Validation

After all agents complete, verify the project still works.

**For each language:**

1. **Go:**

   ```bash
   go build ./...
   go test ./...
   ```

2. **Python:**

   ```bash
   python -m pytest
   # or
   python -m unittest discover
   ```

3. **Rust:**

   ```bash
   cargo check
   cargo test
   ```

4. **TypeScript/React:**

   ```bash
   npm run build  # or bun run build
   npm test       # or bun test
   ```

5. **Java:**
   ```bash
   mvn clean compile
   mvn test
   # or
   gradle build
   ```

**If validation fails:**

- Report the error to the user
- Suggest reverting changes: `git diff` to review, `git restore .` if needed
- Do NOT proceed to summary

**If validation passes:**

- Proceed to Step 4

---

### Step 4: Summary

Provide a concise summary to the user:

**Format:**

```
Dead code cleanup completed for: [list of languages]

Removed:
- [Language 1]: X unused functions, Y unused imports, Z unused files
- [Language 2]: A unused exports, B unused dependencies

Validation: ✅ All tests pass

Next steps:
- Review changes: git diff
- Commit if satisfied: git add -A && git commit -m "chore: remove dead code"
```

**Do NOT:**

- Generate documentation files
- Create detailed reports
- Add excessive commentary

**Keep it brief:** 4-6 lines maximum.

---

## Error Handling

| Error                             | Action                                                                                               |
| --------------------------------- | ---------------------------------------------------------------------------------------------------- |
| No languages detected             | Ask user to specify project type                                                                     |
| Agent fails                       | Report error, continue with other agents                                                             |
| Validation fails                  | Alert user, suggest git restore                                                                      |
| Tools missing (knip, cargo, etc.) | Inform user, skip that language                                                                      |
| Workflow run aborted mid-way      | Resume with `Workflow({ scriptPath, resumeFromRunId })` — completed languages are cached and skipped |

---

## Important Notes

- This skill orchestrates agents but does NOT perform cleanup itself
- Agents are responsible for running language-specific tools
- Always validate after cleanup (Step 3 is mandatory)
- For safety, never auto-commit — let the user review and commit manually
