---
name: app-documenter
description:
  Generate and maintain module-level README documentation for any application,
  with automatic language detection (Go, TypeScript, Python, Rust, Java) and
  CLAUDE.md documentation index management. Use when users want to document
  their codebase, generate READMEs for modules/packages, create a documentation
  index, update existing docs to match current code, or run "document this
  project". Triggers on requests like "document the app", "generate docs",
  "update documentation", "create doc index", "add docs to CLAUDE.md".
---

# App Documenter

Generate or update README.md documentation for application modules and maintain
a documentation index in CLAUDE.md.

## Usage

```
/app-documenter [target]
```

| Target               | Description                                             |
| -------------------- | ------------------------------------------------------- |
| `all`                | Document all modules + update CLAUDE.md index (default) |
| `index`              | Only update the CLAUDE.md documentation index           |
| `src/path/to/module` | Document a specific module/package                      |

---

## Process Overview

Three phases, using parallel agents for speed:

| Phase           | Goal                                             | Agents |
| --------------- | ------------------------------------------------ | ------ |
| **1. Discover** | Detect language, find documentable modules       | 1      |
| **2. Document** | Create/update README.md per module               | 4-6    |
| **3. Index**    | Generate/update documentation index in CLAUDE.md | 1      |

---

## ⚠️ CRITICAL: No Speculation Policy

**DO NOT INVENT DOCUMENTATION.** When generating or updating READMEs:

1. **Document only what exists:** Base all documentation strictly on actual code in the module
2. **If code is unclear:** Use WebSearch to find standard documentation patterns or consult official language docs
3. **If functionality cannot be determined:** State "Unable to document this function - purpose unclear from code" rather than guessing
4. **NEVER:** Invent function purposes, make up usage examples that don't match the code, or describe behavior that isn't implemented

This applies to ALL agents launched in parallel during Phase 2. Each agent must document only verified, existing functionality.

---

## ⚠️ CRITICAL: Read-Only Policy

**DO NOT MODIFY SOURCE CODE.** This skill is documentation-only:

1. **READ-ONLY:** Only read source files - NEVER modify them
2. **WRITE ONLY READMEs:** The ONLY files you can write are README.md and CLAUDE.md
3. **NO CODE CHANGES:** Do NOT:
   - Re-export interfaces or types
   - Add missing exports
   - Fix code structure
   - Refactor anything
   - Add comments to code
   - Modify imports/exports
4. **If code is missing exports:** Document what IS exported, not what SHOULD be exported

This is MANDATORY. Agents that modify source code will fail the task.

---

## ⚠️ CRITICAL: No Uncommitted Work Policy

**DO NOT COMMIT WORK UNLESS EXPLICITLY REQUESTED BY THE USER.**

Follow this policy strictly:

1. **Never auto-commit**: This skill should NEVER create git commits automatically
2. **User must request it**: Only commit when the user explicitly asks
3. **Prepare but don't commit**: Generate/update README files but inform the user and ask if they want to commit
4. **NEVER**: Commit as part of the workflow, commit "to save progress", or commit without explicit user approval

This applies to ALL agents launched in parallel during Phase 2. Agents must never commit their documentation changes automatically.

---

## Phase 1: Discover Modules

### Step 1.1: Detect Project Languages

Scan the project root to identify languages and structure:

```bash
# Detect by file extensions
find . -maxdepth 4 -type f \( -name "*.go" -o -name "*.ts" -o -name "*.tsx" -o -name "*.py" -o -name "*.rs" -o -name "*.java" \) | head -50

# Check for known config files
ls go.mod package.json pyproject.toml setup.py Cargo.toml pom.xml build.gradle 2>/dev/null
```

Language detection rules:

| Indicator                              | Language   |
| -------------------------------------- | ---------- |
| `go.mod`                               | Go         |
| `package.json` + `*.ts`/`*.tsx`        | TypeScript |
| `pyproject.toml` / `setup.py` / `*.py` | Python     |
| `Cargo.toml`                           | Rust       |
| `pom.xml` / `build.gradle` / `*.java`  | Java       |

A project may contain multiple languages (e.g. Go backend + TypeScript
frontend).

### Step 1.2: Find Documentable Modules

A module is documentable if it has **public exports** or is a **significant
package**:

**Go**: Any directory containing `.go` files with exported symbols (uppercase
names).

```bash
find . -name "*.go" -not -name "*_test.go" -not -path "*/vendor/*" | xargs dirname | sort -u
```

**TypeScript/React**: Directories with `index.ts`/`index.tsx` or multiple
exported `.ts` files.

```bash
find src -name "index.ts" -o -name "index.tsx" | xargs dirname | sort -u
```

**Python**: Directories with `__init__.py` or significant `.py` files.

```bash
find . -name "__init__.py" -not -path "*/venv/*" -not -path "*/.venv/*" | xargs dirname | sort -u
```

**Rust**: Directories with `mod.rs` or `lib.rs`.

```bash
find . -name "mod.rs" -o -name "lib.rs" | xargs dirname | sort -u
```

**Java**: Directories containing `.java` files with public classes.

```bash
find . -name "*.java" -not -path "*/test/*" | xargs dirname | sort -u
```

### Step 1.3: Classify Modules

Split discovered modules into two lists:

- **Missing README**: Modules without a README.md
- **Existing README**: Modules with a README.md that may need updating

---

## Phase 1 → Phase 2 Validation Gate

After Phase 1 completes, present a summary and wait for confirmation before writing any file:

```
📋 Documentation plan
   Language(s): <detected languages>
   Modules to create: <N> (<list>)
   Modules to update: <N> (<list>)
   Modules unchanged: <N>
   Files to write: README.md in <N> locations + CLAUDE.md index

Proceed? [yes] [adjust target] [abort]
```

**Validation gate:** Do not write any README.md or modify CLAUDE.md until the user confirms the plan.

---

## Phase 2: Create/Update READMEs

Use parallel agents (Task tool, `subagent_type: 'general-purpose'`,
`model: 'haiku'`) in batches of 4-6.

Read the appropriate template from
[references/templates.md](references/templates.md) based on detected language
before spawning agents.

### Agent Prompt for Creating a README

```text
Create a README.md for {module_path}/

**Advisor tool:** You have access to an `advisor` tool backed by a stronger reviewer model. For modules with complex or ambiguous code, call `advisor()` before writing the README. Give the advice serious weight.

⚠️ CRITICAL - No Speculation Policy:
- Document ONLY what exists in the code - do NOT invent functionality
- If unclear about code purpose, use WebSearch for documentation standards
- If you cannot determine what a function does, state that explicitly
- All examples must be based on actual code, not hypothetical usage

⚠️ CRITICAL - Read-Only Policy:
- NEVER modify source code files - this is READ-ONLY documentation task
- ONLY write to README.md - do NOT touch .go, .ts, .tsx, .py, .rs, .java files
- Do NOT re-export interfaces, add exports, refactor code, or fix imports
- Document what IS exported, not what SHOULD be exported

1. Read all source files in this directory (exclude tests, generated files)
2. Identify all public exports (functions, classes, structs, interfaces, components)
3. Detect deprecated/backward compatibility code by searching for comments:
   - @deprecated, // Deprecated:, # Deprecated:
   - backward compatibility, backward compat, back compat, backwards compatibility
   - TODO: remove, FIXME: remove, TODO: delete, should be removed
   - legacy, legacy code, temporary fix, temporary workaround
   - Any comment indicating future removal, deprecation, or temporary backward compatibility
4. Generate README.md following the {language} template:
   - Title and import/use example
   - Summary table of all exports
   - Each export documented with: signature, parameters, returns, example
   - If deprecated code found: add "## Deprecated / To Be Removed" section listing:
     * Function/class name
     * Deprecation reason (from comment)
     * Recommended alternative (if mentioned in comment)
5. Write the README.md to {module_path}/README.md
```

### Agent Prompt for Updating a README

```text
Check if README needs update for {module_path}/

**Advisor tool:** You have access to an `advisor` tool backed by a stronger reviewer model. For modules with complex or ambiguous code, call `advisor()` before rewriting the README. Give the advice serious weight.

⚠️ CRITICAL - No Speculation Policy:
- Document ONLY what exists in the code - do NOT invent functionality
- If unclear about code purpose, use WebSearch for documentation standards
- If you cannot determine what a function does, state that explicitly
- All examples must be based on actual code, not hypothetical usage

⚠️ CRITICAL - Read-Only Policy:
- NEVER modify source code files - this is READ-ONLY documentation task
- ONLY write to README.md - do NOT touch .go, .ts, .tsx, .py, .rs, .java files
- Do NOT re-export interfaces, add exports, refactor code, or fix imports
- Document what IS exported, not what SHOULD be exported

1. Read the existing README.md
2. Read all source files (exclude tests, generated files)
3. Detect deprecated/backward compatibility code by searching for comments:
   - @deprecated, // Deprecated:, # Deprecated:
   - backward compatibility, backward compat, back compat, backwards compatibility
   - TODO: remove, FIXME: remove, TODO: delete, should be removed
   - legacy, legacy code, temporary fix, temporary workaround
   - Any comment indicating future removal, deprecation, or temporary backward compatibility
4. Compare: Are all public exports documented? Are signatures current? Is deprecated list current?
5. If changes needed:
   - Update README.md with current code
   - Preserve any custom "Notes", "Gotchas", "Architecture" sections
   - Add new exports, remove deleted ones, update changed signatures
   - Add/update "## Deprecated / To Be Removed" section if deprecated code exists
   - Remove "## Deprecated / To Be Removed" section if no deprecated code remains
6. If no changes needed:
   - Report "No update needed"

Only write the file if actual changes are required.
```

### Batching & Parallelism Strategy

Maximize parallel agent usage for speed. The number of agents per batch scales
with project size:

| Project Size  | Agents per Batch |
| ------------- | ---------------- |
| < 10 modules  | 4-6 agents       |
| 10-25 modules | 6-8 agents       |
| 25+ modules   | 8-10 agents      |

1. **Batch 1**: Launch N agents for missing READMEs (creation)
2. **Wait** for batch completion
3. **Batch 2**: Launch N agents for existing READMEs (update check)
4. **Repeat** until all modules processed

**IMPORTANT**: Always launch as many agents in parallel as possible within a
single message. Each agent call via the Task tool in the same message runs
concurrently. Do not serialize agent calls unnecessarily — group all independent
work into a single parallel batch.

---

## Phase 3: Update CLAUDE.md Documentation Index

After documentation is complete, update or create a `## Documentation Index`
section in the project's CLAUDE.md.

### Step 3.1: Read Current CLAUDE.md

Read the existing CLAUDE.md file. If it doesn't exist, this phase only generates
the index content and reports it — do not create a CLAUDE.md from scratch.

### Step 3.2: Generate Index Content

Build the documentation index with this structure:

```markdown
## Documentation Index

Project documentation generated by `app-documenter`.

### Directory Structure

\`\`\` project-root/ ├── src/ │ ├── module-a/ │ │ └── README.md │ ├── module-b/
│ │ └── README.md │ └── ... └── ... \`\`\`

### Modules

| Module   | Path                                               | Language   | Description       |
| -------- | -------------------------------------------------- | ---------- | ----------------- |
| module-a | [`src/module-a/README.md`](src/module-a/README.md) | Go         | Brief description |
| module-b | [`src/module-b/README.md`](src/module-b/README.md) | TypeScript | Brief description |

### By Category

#### Backend

- [`src/handlers/README.md`](src/handlers/README.md) - HTTP/gRPC handlers
- [`src/services/README.md`](src/services/README.md) - Business logic

#### Frontend

- [`src/components/forms/README.md`](src/components/forms/README.md) - Form
  components
- [`src/hooks/README.md`](src/hooks/README.md) - React hooks
```

### Step 3.3: Insert/Replace in CLAUDE.md

- If `## Documentation Index` section exists: **replace** it entirely
- If it doesn't exist: **append** it at the end of the file
- Preserve all other CLAUDE.md content untouched

### Category Detection

Categorize modules automatically based on path and content:

| Pattern                                    | Category       |
| ------------------------------------------ | -------------- |
| `handlers`, `controllers`, `routes`, `api` | API / Handlers |
| `services`, `usecases`, `domain`           | Business Logic |
| `models`, `entities`, `schemas`            | Data Models    |
| `repositories`, `store`, `db`, `database`  | Data Access    |
| `components`, `views`, `pages`             | Frontend / UI  |
| `hooks`                                    | React Hooks    |
| `utils`, `helpers`, `lib`, `pkg`           | Utilities      |
| `middleware`, `interceptors`               | Middleware     |
| `config`, `settings`                       | Configuration  |
| `cmd`, `main`                              | Entry Points   |

---

## Exclusions

Skip these when discovering modules:

- Test directories (`*_test.go`, `*.test.ts`, `*.spec.ts`, `test/`, `tests/`,
  `__tests__/`)
- Generated code (`*.gen.go`, `*.generated.ts`, `node_modules/`, `dist/`,
  `build/`)
- Vendor/dependencies (`vendor/`, `venv/`, `.venv/`, `target/`)
- Hidden directories (`.git/`, `.github/`, `.vscode/`)
- Type-only files (document types inline with their usage)

---

## Summary Output

After completion, display:

```
Documentation Summary
=====================
Language(s): Go, TypeScript
Modules found: 15
READMEs created: 8
READMEs updated: 4
READMEs unchanged: 3
CLAUDE.md index: Updated (15 modules indexed)

Created:
  - src/handlers/README.md
  - src/services/auth/README.md
  ...

Updated:
  - src/models/README.md
  - src/utils/README.md
  ...
```
