---
name: function-documenter
description: >
  Add idiomatic doc-comments above every function in a project to help both
  humans and AI assistants understand the code. A short functional summary is
  written above each function, followed by a one-line description per parameter,
  using the language's native doc format (godoc, Python docstrings, JSDoc/TSDoc,
  rustdoc, Javadoc). Use when user asks to "document the functions",
  "add function comments", "function-doc", "documente les fonctions",
  "ajoute des commentaires aux fonctions", or "commente le code". Existing
  comments are evaluated and only rewritten when they are wrong, outdated, or
  insufficient. Supports Go, Python, Rust, TypeScript/React, and Java.
allowed-tools: Read, Glob, Grep, Bash, Task
---

# Function Documenter

Add idiomatic, per-language doc-comments above every function in the project.
Comments serve both human readers and AI assistants reading the code, so they
should describe **what the function does functionally** (not how it is
implemented) and give a **one-line description for each parameter**.

The skill orchestrates a two-phase workflow:

1. **Exploration** (Haiku) — cheap, fast pass that maps the project: which
   files exist, which functions live in them, which already have acceptable
   doc-comments.
2. **Documentation** (language-expert agent) — the official agent for each
   detected language (`go-expert`, `python-expert`, `rust-expert`,
   `react-expert`, `java-expert`) writes the comments in the
   **language's official standard format**.

The comment format is **strictly enforced** — see
`references/comment-formats.md`. There is exactly one mandated format per
language (the official idiomatic standard: godoc for Go, Google-style
docstrings for Python, rustdoc for Rust, TSDoc for TypeScript, Javadoc for
Java). Agents must not adapt to the project's existing style, must not
invent variants, and must not mix styles. Uniformity on the language's
official standard across the whole codebase is the entire point.

---

## Execution Budget

| Resource                      | Limit  | On exceeded                                          |
| ----------------------------- | ------ | ---------------------------------------------------- |
| Total workflow steps          | 20     | Stop, report progress, ask user to continue or abort |
| Parallel documentation agents | 10 max | Queue remaining directories for next batch           |
| Per-agent timeout             | 5 min  | Mark directory as failed, continue others            |
| Files per agent               | 20 max | Split directory into smaller groups                  |

If any limit is reached: report completed directories, pending directories, and ask whether to continue or stop.

---

## ⚠️ Policies

### No behavior changes

This skill **only adds or rewrites comments**. It must never modify executable
code, signatures, imports, or formatting beyond the comment blocks themselves.

### Evaluate before overwriting

For every function with an existing doc-comment, evaluate whether it is
correct, current, and complete (covers the function purpose **and** all
parameters). Keep it if it is good. Improve it if it is partial. Replace it
only if it is wrong or misleading. Never delete a useful comment to "rewrite
from scratch" reflexively.

### No commits

Never create a git commit. Leave the changes in the working tree for the user
to review with `git diff`.

---

## Workflow

### Step 1 — Detect project languages

Use Glob to identify which languages are present:

| Language         | Markers                                                |
| ---------------- | ------------------------------------------------------ |
| Go               | `go.mod`, `**/*.go`                                    |
| Python           | `pyproject.toml`, `requirements.txt`, `**/*.py`        |
| Rust             | `Cargo.toml`, `**/*.rs`                                |
| TypeScript/React | `package.json`, `tsconfig.json`, `**/*.ts`, `**/*.tsx` |
| Java             | `pom.xml`, `build.gradle`, `**/*.java`                 |

Exclude vendored / generated paths: `node_modules/`, `vendor/`, `target/`,
`dist/`, `build/`, `.next/`, `__pycache__/`, `*_pb.go`, `*.pb.go`,
`*_generated.*`.

**Also skip test files and test functions** — their names already describe
intent (`TestFooReturnsErrorWhenBarIsNil`) and adding doc-comments adds noise
without information. Excluded patterns:

| Language         | Skip                                                                                                                                   |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| Go               | `*_test.go` files; functions named `Test*`, `Benchmark*`, `Fuzz*`, `Example*`                                                          |
| Python           | `test_*.py`, `*_test.py`, `tests/` and `test/` directories; functions named `test_*`; pytest fixtures decorated with `@pytest.fixture` |
| Rust             | files/modules under `#[cfg(test)]`, `tests/` directories; functions decorated with `#[test]`, `#[bench]`, `#[tokio::test]`             |
| TypeScript/React | `*.test.ts(x)`, `*.spec.ts(x)`, `__tests__/` directories; calls inside `describe/it/test` blocks                                       |
| Java             | files under `src/test/`; methods annotated with `@Test`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`, etc.                       |

If no language is detected, ask the user which directory to document.

---

### Step 2 — Exploration pass (Haiku)

For each detected language, launch a single exploration subagent with model
`haiku` whose job is to **only inventory**, not to write comments.

Use the `Task` tool with `subagent_type: general-purpose` and `model: haiku`.

Prompt template:

```
Explore the {LANGUAGE} code in this project. For every source file:
- list the file path
- list every function/method declared in that file (name + line number)
- for each function, read the signature (parameter names + types, return
  types) and the existing doc-comment, then classify the comment as:

    OK      — ALL of the following are true:
              1. A functional summary is present (describes what/why, not how).
              2. Every parameter in the signature has a DEDICATED line in the
                 comment (not just mentioned in passing within prose).
              3. The return value is documented if the function returns
                 something non-trivial (not void/unit/nothing).
              4. The comment does not contradict the current signature.
              Use OK only when every condition above is met. When in doubt,
              use PARTIAL.

    PARTIAL — A comment exists but at least one condition for OK is not met:
              a summary is missing or too vague, one or more parameters lack
              a dedicated line, or the return value is undocumented.

    WRONG   — The comment contradicts the code (wrong parameter names, wrong
              return type, describes removed behavior, etc.).

    MISSING — No doc-comment at all.

Do NOT modify any files. Do NOT write comments. Output a compact JSON array:
[{ "file": "...", "functions": [{ "name": "...", "line": N, "status": "OK|PARTIAL|WRONG|MISSING" }] }]

Skip vendored/generated paths: node_modules, vendor, target, dist, build,
__pycache__, *_pb.go, *_generated.*

Also skip test files and test functions entirely (do not list them in the
inventory):
- Go: *_test.go; Test*, Benchmark*, Fuzz*, Example* functions
- Python: test_*.py, *_test.py, tests/, test/; test_* functions; pytest fixtures
- Rust: #[cfg(test)] modules, tests/; #[test], #[bench], #[tokio::test] fns
- TypeScript: *.test.ts(x), *.spec.ts(x), __tests__/
- Java: src/test/; @Test, @ParameterizedTest, @BeforeEach, @AfterEach methods
```

Collect the JSON inventory per language. Files where every function is `OK`
can be skipped entirely in Step 3.

---

### Step 2 → Step 3 Validation Gate

After the exploration pass, present the inventory summary and wait for confirmation before writing any file:

```
📋 Documentation plan
   Language(s): <detected languages>
   Directories to process: <N>
   Functions to document: <N> (MISSING: X, PARTIAL: Y, WRONG: Z)
   Functions already OK: <N> (will be skipped)
   Files that will be modified: <list>

Proceed? [yes] [select specific directories] [abort]
```

**Validation gate:** Do not launch any documentation agent until the user confirms the plan.

---

### Step 3 — Documentation pass (one agent per package/path)

Rather than one agent per language, dispatch **one agent per package or
directory** so agents are small, focused, and fully parallel. An agent that
owns a single directory can read all its files in context and write consistent
comments across them without being overwhelmed by an entire codebase.

#### Agent-to-language mapping

| Language         | Agent           | Model |
| ---------------- | --------------- | ----- |
| Go               | `go-expert`     | haiku |
| Python           | `python-expert` | haiku |
| Rust             | `rust-expert`   | haiku |
| TypeScript/React | `react-expert`  | haiku |
| Java             | `java-expert`   | haiku |

#### Grouping rules

From the Step 2 inventory, group files by **immediate parent directory**
(one directory = one agent). Use the following language-specific grouping
when it maps naturally to the project structure:

- **Go**: group by Go package (files sharing the same `package` declaration and
  directory).
- **Python**: group by module directory (files under the same `__init__.py`
  parent, or top-level directory if no `__init__.py`).
- **Rust**: group by `mod` directory or `src/` sub-directory.
- **TypeScript/React**: group by feature/component directory.
- **Java**: group by Java package directory (`com/example/foo/`).

**Skip a group entirely** if every function in all its files is already `OK` —
no agent needed.

**Minimum threshold**: only spawn an agent for a group that contains at least
one function with status `MISSING`, `PARTIAL`, or `WRONG`.

#### Parallelism

Launch **all agents in a single message** (multiple Task tool calls). Agents
across different languages and different directories are always independent —
never wait for one to finish before launching others.

#### Prompt template (per agent / per directory group)

```
You will add doc-comments to {LANGUAGE} source files in the directory
`{DIRECTORY}`. The comment format is STRICTLY MANDATED — use the official
{LANGUAGE} standard documented in
skills/function-documenter/references/comment-formats.md, section "{LANGUAGE}".

Do not adapt to the project's existing style, do not invent variants, do not
mix styles. The whole point of this pass is to converge the codebase onto the
language's official standard.

For each function:
1. Write a short functional summary (1-3 lines) describing WHAT the function
   does and WHY it exists — not how it is implemented. Avoid restating the
   function name. Mention non-obvious behavior, side effects, and error modes
   when they exist.
2. Document EVERY parameter with a one-line description. Include the receiver
   (Go), self/cls (Python), and generic type parameters when meaningful.
3. Document the return value(s) when the function returns something
   non-trivial.
4. Document errors/exceptions when the function can raise/return them.

Rules:
- Use ONLY the format defined in comment-formats.md for {LANGUAGE}. No
  exceptions, even if the surrounding file uses a different style — the goal
  is to converge the whole codebase onto one format.
- Do NOT modify code, signatures, imports, or unrelated formatting.
- For functions marked OK in the inventory, leave the existing comment alone.
- For PARTIAL, augment the existing comment in place rather than rewriting it
  from scratch — but reformat it to match the mandated format if it deviates.
- For WRONG, replace the comment entirely.
- For MISSING, add a new comment.
- Apply to ALL functions including private/unexported ones.
- Never auto-commit.

Inventory of files and functions to process (this directory only):
{JSON_SUBSET_FROM_STEP_2}
```

---

### Step 4 — Validation

After all agents complete, verify nothing else changed:

```bash
git diff --stat
```

Then per language run a syntax/build check to catch any accidental damage:

| Language         | Check                                     |
| ---------------- | ----------------------------------------- |
| Go               | `go build ./...`                          |
| Python           | `python -m compileall .`                  |
| Rust             | `cargo check`                             |
| TypeScript/React | `bunx tsc --noEmit` or `npx tsc --noEmit` |
| Java             | `mvn -q compile` or `gradle compileJava`  |

If a check fails, report the failing files and suggest `git restore <file>`
to revert that file. Do not attempt to "fix" the build — comments should never
break it.

---

### Step 5 — Summary

Brief summary (4–6 lines max):

```
Function documentation pass completed for: [languages]

Documented:
- Go: X functions across Y files (Z left untouched as already OK)
- Python: ...

Review: git diff
```

Do not write a report file. Do not commit.

---

## Error Handling

| Situation                            | Action                                                  |
| ------------------------------------ | ------------------------------------------------------- |
| No language detected                 | Ask user which directory/language to document           |
| Exploration subagent returns no JSON | Retry once with stricter instructions, then report      |
| Expert agent fails on a language     | Continue with the others, report the failure at the end |
| Validation build breaks              | Identify the offending file, suggest `git restore`      |

---

## Notes

- The skill is purely additive: it only writes comments.
- Existing good comments are preserved — see "Evaluate before overwriting".
- Generated files and vendored dependencies are always skipped.
- For very large projects, the exploration pass keeps the orchestrator's
  context small by handing each expert agent only the files that need work.
