# Language Detection Patterns

Comprehensive patterns for detecting languages and frameworks in multi-language
projects.

---

## Detection Strategy

Use a combination of:

1. **Build files** — Primary indicators (go.mod, package.json, etc.)
2. **Directory structure** — Common patterns (src/, cmd/, tests/)
3. **File extensions** — Source code files (.go, .py, .rs, .java, .ts)

Always check the **entire project tree**, not just the root directory, to detect
multi-module projects.

---

## Go

### Primary Indicators

- `go.mod` — Module definition (presence = Go project)
- `go.sum` — Dependency lock file

### Directory Patterns

- `cmd/` — Command-line applications (one binary per subdirectory)
- `pkg/` — Public library code
- `internal/` — Private application code
- `api/` — API definitions (gRPC, OpenAPI)

### Test Patterns

- `*_test.go` — Test files (same package as source)
- `testdata/` — Test fixtures
- `_test` package suffix — Black-box tests

### Detection Command

```bash
find . -name "go.mod" -type f
```

### Project Structure Extraction

For each `go.mod` found:

1. Extract module name: `go list -m`
2. List packages: `go list ./...`
3. Identify untested packages: compare source vs test files

---

## Python

### Primary Indicators

- `requirements.txt` — Pip dependencies
- `pyproject.toml` — Modern Python project config (PEP 518)
- `setup.py` — Legacy package setup
- `Pipfile` — Pipenv projects
- `poetry.lock` — Poetry projects

### Directory Patterns

- `src/` — Source code (src layout)
- Package directories with `__init__.py`
- `tests/` or `test/` — Test directory

### Test Patterns

- `test_*.py` — Pytest convention
- `*_test.py` — Alternative naming
- `tests/` directory structure mirrors `src/`

### Detection Command

```bash
find . \( -name "requirements.txt" -o -name "pyproject.toml" -o -name "setup.py" \) -type f
```

### Project Structure Extraction

1. Identify Python package root (where setup.py or pyproject.toml lives)
2. List packages: find directories with `__init__.py`
3. Map source files to test files

---

## Java

### Primary Indicators

- `pom.xml` — Maven project
- `build.gradle` or `build.gradle.kts` — Gradle project
- `settings.gradle` — Gradle multi-module project

### Directory Patterns

- `src/main/java/` — Main source code
- `src/test/java/` — Test code
- `src/main/resources/` — Resources
- Maven/Gradle standard layout

### Test Patterns

- `*Test.java` — JUnit test classes
- `*Tests.java` — Alternative naming
- Test classes in `src/test/java/` mirror `src/main/java/` package structure

### Detection Command

```bash
find . \( -name "pom.xml" -o -name "build.gradle" -o -name "build.gradle.kts" \) -type f
```

### Project Structure Extraction

- **Maven:** Parse `pom.xml` for `<modules>` (multi-module)
- **Gradle:** Parse `settings.gradle` for `include` statements
- List source packages from `src/main/java/`

---

## Rust

### Primary Indicators

- `Cargo.toml` — Package manifest
- `Cargo.lock` — Dependency lock file

### Directory Patterns

- `src/main.rs` — Binary crate
- `src/lib.rs` — Library crate
- `src/bin/` — Multiple binaries
- `tests/` — Integration tests
- `benches/` — Benchmarks

### Test Patterns

- `#[test]` — Unit tests (inline in source files)
- `#[cfg(test)]` — Test module marker
- `tests/*.rs` — Integration tests
- `tests/common/mod.rs` — Shared test utilities

### Detection Command

```bash
find . -name "Cargo.toml" -type f
```

### Project Structure Extraction

- Parse `Cargo.toml` for `[workspace]` (multi-crate workspace)
- List modules: parse `mod` declarations in `src/lib.rs` or `src/main.rs`
- Check for inline `#[test]` vs external `tests/` directory

---

## TypeScript/React

### Primary Indicators

- `package.json` — Node.js project
- Check for TypeScript: `"typescript"` in dependencies or devDependencies
- Check for React: `"react"` in dependencies

### Directory Patterns

- `src/` — Source code
- `components/` — React components
- `__tests__/` — Jest test directory
- `test/` or `tests/` — Alternative test directory

### Test Patterns

- `*.test.ts`, `*.test.tsx` — Test files (Jest convention)
- `*.spec.ts`, `*.spec.tsx` — Alternative naming (Jasmine/Mocha)
- `__tests__/*.ts` — Tests in separate directory

### Detection Command

```bash
find . -name "package.json" -type f
```

### Project Structure Extraction

1. Parse `package.json` for `"workspaces"` (monorepo)
2. Check for TypeScript: presence of `tsconfig.json`
3. Detect framework:
   - React: `"react"` in dependencies
   - Next.js: `"next"` in dependencies
   - Nest.js: `"@nestjs/core"` in dependencies

---

## Multi-Language Detection Algorithm

**Step 1: Scan for all indicators**

```bash
# Find all build files
find . \( \
  -name "go.mod" -o \
  -name "package.json" -o \
  -name "Cargo.toml" -o \
  -name "pom.xml" -o \
  -name "build.gradle" -o \
  -name "requirements.txt" -o \
  -name "pyproject.toml" \
\) -type f
```

**Step 2: Classify by language** For each file found:

- Extract the parent directory (project root for that language)
- Identify the language
- Store as: `{language: [list of project roots]}`

**Step 3: Handle nested projects** If a project is nested inside another (e.g.,
a React frontend inside a Go backend):

- Treat each as independent
- Report the hierarchy to the user

**Step 4: Report to user**

```
Detected Languages:
- Go: 2 modules
  - ./cmd/service1 (go.mod)
  - ./cmd/service2 (go.mod)
- Python: 1 package
  - ./scripts (requirements.txt)
- TypeScript/React: 1 app
  - ./frontend (package.json with react)
```

---

## Edge Cases

**Monorepos:**

- Detect workspace configuration (Go workspaces, npm workspaces, Cargo
  workspaces)
- List all subprojects individually

**Language detection conflicts:**

- If both `requirements.txt` and `package.json` exist → separate projects
- Report both languages

**No build files found:**

- Fall back to source file extensions
- Ask user to confirm language manually

**Ambiguous detection:**

- `package.json` without TypeScript → JavaScript (skip if only TypeScript is
  supported)
- Ask user if they want JavaScript test generation
