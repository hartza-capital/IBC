---
name: dead-code-cleaner
description: >
  Automatically detect and remove dead code from any project by analyzing the
  codebase and dispatching to the appropriate language expert agent. Use when
  user asks to "clean dead code", "remove unused code", "find dead code",
  "nettoie le code mort", "supprime le code inutilisé", or "cleanup unused".
  Supports Go, Python, Rust, TypeScript/React, and Java projects.
allowed-tools: Read, Glob, Grep, Bash, Task
---

# Dead Code Cleaner

Automatically detect project languages and dispatch to expert agents for dead
code cleanup.

---

## Execution Budget

| Resource                   | Limit  | On exceeded                                          |
| -------------------------- | ------ | ---------------------------------------------------- |
| Total workflow steps       | 20     | Stop, report progress, ask user to continue or abort |
| Parallel expert agents     | 4 max  | Queue remaining languages sequentially               |
| Per-agent timeout          | 10 min | Mark language as timed out, report partial findings  |
| Dead code items to process | 100    | Truncate to top 100 by confidence, note truncation   |

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

**Call `advisor()` before Step 2** — With the detected languages and project structure in your context, call `advisor()` to validate the cleanup strategy and tool selection before dispatching agents.

---

### Step 2: Dispatch to Expert Agents

For each detected language, launch the appropriate agent using the Task tool.

**Agent mapping:**

| Language         | Agent           | Task Description                                           |
| ---------------- | --------------- | ---------------------------------------------------------- |
| Go               | `go-expert`     | "Analyze and remove dead code in Go project"               |
| Python           | `python-expert` | "Analyze and remove dead code in Python project"           |
| Rust             | `rust-expert`   | "Analyze and remove dead code in Rust project"             |
| TypeScript/React | `react-expert`  | "Analyze and remove dead code in TypeScript/React project" |
| Java             | `java-expert`   | "Analyze and remove dead code in Java project"             |

**Instructions to provide to each agent:**

See `references/agent-dispatch-guide.md` for detailed instructions per language.

**Key points for agents:**

- **Advisor tool:** You have access to an `advisor` tool backed by a stronger reviewer model. Call `advisor()` before removing code (after the analysis phase), and when you believe the cleanup is complete. Give the advice serious weight.
- Use language-specific tools (cargo clippy, knip, go vet, etc.)
- Identify unused code: functions, imports, types, files, dependencies
- Remove dead code safely (verify before deleting)
- Run validation after each batch of changes

**Multi-language projects:**

- Launch agents in parallel when languages are independent
- Launch sequentially if there are cross-language dependencies (rare)

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

| Error                             | Action                                   |
| --------------------------------- | ---------------------------------------- |
| No languages detected             | Ask user to specify project type         |
| Agent fails                       | Report error, continue with other agents |
| Validation fails                  | Alert user, suggest git restore          |
| Tools missing (knip, cargo, etc.) | Inform user, skip that language          |

---

## Important Notes

- This skill orchestrates agents but does NOT perform cleanup itself
- Agents are responsible for running language-specific tools
- Always validate after cleanup (Step 3 is mandatory)
- For safety, never auto-commit — let the user review and commit manually
