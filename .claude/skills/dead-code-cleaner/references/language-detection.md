# Language Detection Guide

Patterns for automatically detecting project languages.

---

## Detection Strategy

Use Glob to search for language-specific marker files and source files.

**Priority order:**

1. Check whether the repo is indexed in codebase-memory-mcp — if so (or after auto-indexing), graph pre-analysis is available (see SKILL.md Step 1b)
2. Check for build/config files (highest confidence)
3. Check for source files (medium confidence)
4. Check directory structure (lowest confidence)

---

## Codebase Memory graph

**Detection:**

Call `list_projects` (or `index_status` with the repo's project name). If the
repo is not indexed yet, run `index_repository(repo_path=".")` — indexing
happens automatically, so the graph is effectively always available.

**Confidence boost:** When the graph is available, all language confidence levels are bumped one tier because the call graph provides cross-file reachability that static analysis tools cannot see. For example, a Go project detected at Medium confidence (source files only, no go.mod) with the graph available is treated as High confidence for the purposes of dead code detection.

---

## Go

**High confidence:**

- `go.mod` exists at project root
- `go.sum` exists

**Medium confidence:**

- `*.go` files in project
- `vendor/` directory

**Tools to detect:**

- `go version` command available
- `staticcheck`, `golangci-lint` installed (optional)

**Typical structure:**

```
project/
  go.mod
  go.sum
  main.go
  cmd/
  internal/
  pkg/
```

---

## Python

**High confidence:**

- `requirements.txt` exists
- `pyproject.toml` exists
- `setup.py` exists
- `Pipfile` exists

**Medium confidence:**

- `*.py` files in project
- `__pycache__/` directories
- `venv/`, `.venv/` directories

**Tools to detect:**

- `python --version` or `python3 --version`
- `pytest`, `ruff`, `mypy` installed (optional)

**Typical structure:**

```
project/
  requirements.txt
  setup.py
  src/
    __init__.py
    module.py
  tests/
```

---

## Rust

**High confidence:**

- `Cargo.toml` exists at project root
- `Cargo.lock` exists

**Medium confidence:**

- `*.rs` files in project
- `target/` directory

**Tools to detect:**

- `cargo --version` command available
- `cargo-udeps` installed (optional)

**Typical structure:**

```
project/
  Cargo.toml
  Cargo.lock
  src/
    main.rs
    lib.rs
  target/
```

---

## TypeScript/React

**High confidence:**

- `package.json` exists AND contains TypeScript dependencies
- `tsconfig.json` exists
- `next.config.js` or `vite.config.ts` exists

**Medium confidence:**

- `*.ts`, `*.tsx` files in project
- `node_modules/` directory
- `src/` or `app/` directory with TypeScript files

**Tools to detect:**

- `npm --version` or `bun --version`
- `knip` available (check with `npx knip --version`)
- `i18n-check` for translation keys (optional)

**Typical structure:**

```
project/
  package.json
  tsconfig.json
  src/
    components/
    pages/
    App.tsx
  node_modules/
```

---

## Java

**High confidence:**

- `pom.xml` exists (Maven)
- `build.gradle` or `build.gradle.kts` exists (Gradle)

**Medium confidence:**

- `*.java` files in project
- `src/main/java/` directory structure

**Tools to detect:**

- `java --version` command available
- `mvn --version` or `gradle --version`

**Typical structure:**

```
project/
  pom.xml
  src/
    main/
      java/
        com/company/
    test/
      java/
  target/
```

---

## Multi-Language Projects

**Common combinations:**

1. **Tauri (Rust + TypeScript):**
   - `src-tauri/Cargo.toml` + root `package.json`
   - Treat as two separate projects

2. **Go + Python (microservices):**
   - `go.mod` + `requirements.txt` in different directories
   - Treat as two separate projects

3. **Full-stack (React + Java/Go backend):**
   - `frontend/package.json` + `backend/go.mod` or `backend/pom.xml`
   - Treat as separate projects

**Detection logic:**

- If both Rust and TypeScript detected in specific directories, check for Tauri
- If multiple languages at root level, treat as monorepo
- If languages in subdirectories, treat separately

---

## Edge Cases

**False positives:**

- `node_modules/` may contain `*.go`, `*.rs` files (ignore these)
- `vendor/` directories (ignore)
- `target/`, `build/`, `dist/` (build artifacts, ignore)

**Filtering rules:**

- Only scan source directories: `src/`, `lib/`, `cmd/`, `app/`, `internal/`
- Exclude: `node_modules/`, `vendor/`, `target/`, `build/`, `dist/`, `.git/`

---

## Confidence Scoring

Assign a confidence score to each detected language:

| Confidence      | Criteria                                    |
| --------------- | ------------------------------------------- |
| High (90%+)     | Build file + source files + tools available |
| Medium (60-89%) | Build file OR source files                  |
| Low (<60%)      | Only source files, no build config          |

**Threshold:** Only proceed with languages that have Medium or High confidence.
