# Language Detection Patterns

This document defines how to detect programming languages in a project.

## Detection Strategy

Use the Glob tool to search for language-specific marker files and source files. A language is considered "present" if either:

1. A language-specific configuration/manifest file exists, OR
2. Source files with the language's extension exist

## Language Patterns

### Go

**Primary indicators:**

- `go.mod` (Go modules file)
- `go.sum` (dependency lock file)

**Secondary indicators:**

- `**/*.go` (Go source files)
- `Makefile` with Go commands (e.g., `go build`, `go test`)

**Common locations:**

- `/cmd/` - application entry points
- `/internal/` - private application code
- `/pkg/` - public library code
- `/api/` - API definitions

**Framework detection:**

- Gin: check for `github.com/gin-gonic/gin` in go.mod
- Echo: check for `github.com/labstack/echo` in go.mod
- Chi: check for `github.com/go-chi/chi` in go.mod

### Python

**Primary indicators:**

- `requirements.txt` (pip dependencies)
- `setup.py` (package setup)
- `pyproject.toml` (modern Python packaging)
- `Pipfile` (Pipenv)
- `poetry.lock` (Poetry)

**Secondary indicators:**

- `**/*.py` (Python source files)
- `tox.ini` (testing)
- `pytest.ini` (pytest config)

**Common locations:**

- `/src/` - source code
- `/tests/` - test files
- `/scripts/` - utility scripts

**Framework detection:**

- Django: check for `manage.py` or `django` in requirements
- Flask: check for `flask` in requirements
- FastAPI: check for `fastapi` in requirements

### Rust

**Primary indicators:**

- `Cargo.toml` (Rust manifest)
- `Cargo.lock` (dependency lock)

**Secondary indicators:**

- `**/*.rs` (Rust source files)

**Common locations:**

- `/src/` - source code (with `main.rs` or `lib.rs`)
- `/benches/` - benchmarks
- `/examples/` - example programs

**Framework detection:**

- Actix: check for `actix-web` in Cargo.toml
- Rocket: check for `rocket` in Cargo.toml
- Axum: check for `axum` in Cargo.toml

### TypeScript/React

**Primary indicators:**

- `package.json` (npm/yarn package file)
- `tsconfig.json` (TypeScript config)

**Secondary indicators:**

- `**/*.ts` (TypeScript files)
- `**/*.tsx` (TypeScript + JSX files)
- `**/*.jsx` (JavaScript + JSX files)
- `yarn.lock` or `package-lock.json`

**Common locations:**

- `/src/` - source code
- `/components/` - React components
- `/pages/` - Next.js pages
- `/app/` - Next.js app router

**Framework detection:**

- React: check for `react` in package.json dependencies
- Next.js: check for `next` in package.json
- Vue: check for `vue` in package.json
- Angular: check for `@angular/core` in package.json

### Java

**Primary indicators:**

- `pom.xml` (Maven)
- `build.gradle` or `build.gradle.kts` (Gradle)

**Secondary indicators:**

- `**/*.java` (Java source files)

**Common locations:**

- `/src/main/java/` - main source code
- `/src/test/java/` - test code
- `/src/main/resources/` - resources

**Framework detection:**

- Spring Boot: check for `spring-boot` in pom.xml or build.gradle
- Quarkus: check for `quarkus` in dependencies
- Micronaut: check for `micronaut` in dependencies

## Multi-Language Projects

If multiple languages are detected:

1. **Determine primary vs secondary:**
   - Count source files for each language
   - Check directory structure (e.g., `/backend` in Go, `/frontend` in TypeScript)
   - Read README.md for project description

2. **Common patterns:**
   - Go backend + TypeScript frontend (full-stack web app)
   - Python scripts + Go services (data processing + API)
   - Rust core + Python bindings (performance-critical library)

3. **Monorepo indicators:**
   - Multiple root-level language configs (e.g., `go.mod` and `package.json` at root)
   - Workspace files: `yarn workspaces`, `go.work`, `Cargo workspace`
   - Directory structure with clear separation (e.g., `/services/`, `/packages/`)

## Example Detection Output

```
Detected Languages:
✓ Go (primary)
  - go.mod found at root
  - 45 .go files in /cmd, /internal, /pkg
  - Framework: Gin (detected in go.mod)

✓ Python (secondary)
  - requirements.txt found at root
  - 12 .py files in /scripts
  - Framework: None (utility scripts)

✓ TypeScript (frontend)
  - package.json + tsconfig.json in /web
  - 23 .tsx files in /web/src/components
  - Framework: React (detected in package.json)

✗ Rust - not detected
✗ Java - not detected
```

## Edge Cases

**No language detected:**

- May be a non-code project (documentation, config files only)
- May use an unsupported language
- Ask user to confirm project directory

**Language files but no source:**

- Empty project or template
- Config files left over from previous setup
- Consider the language "detected" but note the absence of source

**Conflicting indicators:**

- Multiple versions of the same language (e.g., Python 2 and 3)
- Use the most recent/modern indicator (e.g., pyproject.toml over setup.py)
