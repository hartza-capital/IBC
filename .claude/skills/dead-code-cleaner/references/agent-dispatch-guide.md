# Agent Dispatch Guide

Instructions to provide to each expert agent for dead code cleanup.

---

## Go (go-expert)

**Task prompt:**

```
Analyze and remove dead code in this Go project.

Steps:
1. Run dead code analysis:
   - go vet ./...
   - staticcheck ./... (if available)
   - golangci-lint run --enable unused,deadcode (if available)

   If the repo is indexed (auto-index if not) AND a graph candidate list was
   provided by the orchestrator, prioritize those candidates — verify each with trace_path (direction=inbound)
   before removal. Use trace_path (direction=inbound, risk_labels=true) to confirm no transitive dependents.

2. Identify dead code:
   - Unused functions (not called anywhere, not exported)
   - Unused variables and constants
   - Unused imports
   - Unused struct fields
   - Unused type definitions

   Graph enhancement: for any function flagged by go vet or staticcheck,
   confirm with trace_path (direction=inbound) that it has 0 callers across all packages.
   If callers exist in the graph, do NOT remove — the static tool may have
   missed a reflection call or build-tag variation.

3. Remove dead code safely:
   - For each unused item, verify it's not used via reflection or build tags
   - If the repo is indexed: call trace_path (direction=inbound, risk_labels=true) before each removal to
     see the blast radius; skip items with unexpected dependents
   - Remove unused imports first (safest)
   - Remove unused internal functions
   - Remove unused types and constants
   - DO NOT remove exported items without explicit confirmation

4. Validate after each batch:
   - go build ./...
   - go test ./...

5. Report what was removed (function names, file paths)
```

**Common patterns:**

- Unused handler functions registered in init() may appear unused (keep these)
- Exported functions in internal/ packages can be removed if truly unused
- Check for `//go:generate` directives that might use "unused" code

---

## Python (python-expert)

**Task prompt:**

```
Analyze and remove dead code in this Python project.

Steps:
1. Run dead code analysis:
   - ruff check --select F401,F841 (unused imports, variables)
   - vulture . (if available, for unused code)
   - mypy --warn-unused-ignores (if using mypy)

   If the repo is indexed (auto-index if not) AND a graph candidate list was
   provided by the orchestrator, prioritize those candidates. Use trace_path (direction=inbound) to verify
   each candidate has truly 0 callers — Python dynamic dispatch (getattr,
   importlib) often fools static tools.

2. Identify dead code:
   - Unused imports
   - Unused functions (not called, not in __all__)
   - Unused variables
   - Unused class methods
   - Unused modules/files

   Graph enhancement: for functions flagged by vulture or ruff, confirm
   with trace_path (direction=inbound) that there are no callers. Vulture has a high false
   positive rate for dynamic patterns — the call graph provides a
   second opinion that's grounded in actual call sites.

3. Remove dead code safely:
   - Remove unused imports first
   - Remove unused private functions (start with _)
   - Remove unused variables
   - Check if "unused" functions are in __all__ (may be API)
   - Verify files are not imported dynamically
   - If the repo is indexed: call trace_path (direction=inbound, risk_labels=true) before removing any
     class or module to confirm no unexpected dependents

4. Validate after each batch:
   - python -m pytest (if tests exist)
   - python -m unittest discover (fallback)
   - Run type checking if configured

5. Report what was removed (function names, file paths)
```

**Common patterns:**

- Lambda functions in AWS may have unused handler names (keep these)
- `__all__` exports should be preserved even if not internally used
- Dynamic imports via `importlib` won't be detected

---

## Rust (rust-expert)

**Task prompt:**

```
Analyze and remove dead code in this Rust project.

Steps:
1. Run dead code analysis:
   - cargo clippy -- -W dead_code -W unused
   - cargo +nightly udeps (for unused dependencies, if available)

   If the repo is indexed (auto-index if not) AND a graph candidate list was
   provided by the orchestrator, prioritize those candidates. Use trace_path (direction=inbound) to
   confirm each has 0 callers — especially important for pub items that
   clippy won't flag.

2. Identify dead code:
   - Unused functions (dead_code warnings)
   - Unused imports (unused_imports warnings)
   - Unused variables (unused_variables warnings)
   - Unused dependencies (udeps output)
   - Unused struct fields
   - Unused type aliases

   Graph enhancement: cargo clippy only flags private dead code.
   Use trace_path (direction=inbound) on pub functions to find exported items that are
   also dead (0 callers in the whole repo) — clippy cannot catch these.

3. Remove dead code safely:
   - Remove unused imports first
   - Remove unused private functions
   - Remove unused internal types
   - For pub items: use trace_path (direction=inbound) to verify 0 callers before
     flagging for removal; call trace_path (direction=inbound, risk_labels=true) to confirm blast radius
   - Remove unused dependencies from Cargo.toml

4. Validate after each batch:
   - cargo check
   - cargo test
   - cargo clippy (verify warnings decreased)

5. Report what was removed (function names, dependency names)
```

**Common patterns:**

- Tauri commands may appear unused but are called from frontend
  (keep #[tauri::command])
- Items used only in #[cfg(test)] may appear unused in non-test builds
- proc_macro items may use "unused" code
- pub items in lib.rs are part of the crate API (verify before removing)

---

## TypeScript/React (react-expert)

**Task prompt:**

```
Analyze and remove dead code in this TypeScript/React project.

Steps:
1. Run dead code analysis:
   - bunx knip (or npx knip) for full analysis
   - bunx knip --include files (unused files)
   - bunx knip --include exports (unused exports)
   - bunx knip --include types (unused types)
   - bunx knip --include dependencies (unused dependencies)
   - bunx i18n-check (if locales/ exists, for unused translation keys)

   If the repo is indexed (auto-index if not) AND a graph candidate list was
   provided by the orchestrator, cross-reference it with knip's output. Candidates appearing
   in both lists are high-confidence dead code. Use trace_path (direction=inbound) to
   verify zero callers for any item knip may have missed.

2. Identify dead code:
   - Unused files (not imported)
   - Unused exports (exported but never imported)
   - Duplicate exports (both named and default)
   - Unused types and interfaces
   - Unused dependencies in package.json
   - Unused translation keys (i18n)

   Graph enhancement: knip struggles with re-exports and barrel files.
   Use trace_path (direction=inbound) on exported symbols from index.ts barrel files to
   find exports that are re-exported but never actually consumed.

3. Remove dead code safely:
   - Remove unused types first (safest)
   - Convert duplicate exports to single export (React: default, Services: named)
   - Remove unused files (check for dynamic imports first)
   - If the repo is indexed: call trace_path (direction=inbound, risk_labels=true) before removing a file
     to catch dynamic imports (React.lazy, import()) that knip may miss
   - Remove unused dependencies from package.json
   - Remove unused i18n keys from en/ and fr/ (if applicable)

4. Validation after each batch:
   - bun run tsc --noEmit (or npm run tsc)
   - Re-run knip to verify progress
   - Run tests if available

5. Report what was removed (file names, export names, dependency names)
```

**Common patterns:**

- React components lazy-loaded (React.lazy) may appear unused (verify)
- Barrel exports (index.ts) re-exporting from subdirectories
- Services: prefer named exports (export const myService = ...)
- Components: prefer default exports (export default MyComponent)
- Build artifacts in src-tauri/target/ will show as unused (ignore)

---

## Java (java-expert)

**Task prompt:**

```
Analyze and remove dead code in this Java project.

Steps:
1. Run dead code analysis:
   - Maven: mvn pmd:check (if PMD configured)
   - Gradle: gradle check (if configured)
   - Use IDE analysis or manual inspection

   If the repo is indexed (auto-index if not) AND a graph candidate list was
   provided by the orchestrator, prioritize those candidates. Java reflection and Spring DI
   make static analysis unreliable — use trace_path (direction=inbound) to cross-validate
   each candidate before treating it as safe to remove.

2. Identify dead code:
   - Unused private methods
   - Unused private fields
   - Unused imports
   - Unused local variables
   - Unused classes (not referenced)

   Graph enhancement: Java's reflection-heavy frameworks (Spring, JPA)
   create many phantom "unused" items. Use trace_path (direction=inbound) to distinguish
   genuinely uncalled code from Spring-wired beans. If trace_path (direction=inbound)
   returns 0 AND there are no Spring/JPA annotations on the item, it's safe
   to remove.

3. Remove dead code safely:
   - Remove unused imports
   - Remove unused private methods
   - Remove unused private fields
   - Remove unused local variables
   - For public/protected: use trace_path (direction=inbound) to verify 0 callers across
     all modules; use trace_path (direction=inbound, risk_labels=true) to confirm no dependents

4. Validate after each batch:
   - mvn clean compile (Maven)
   - gradle build (Gradle)
   - mvn test / gradle test

5. Report what was removed (class names, method names)
```

**Common patterns:**

- Spring beans may use "unused" fields via reflection (keep @Autowired)
- Serialization may use "unused" fields (keep serialVersionUID)
- Public API methods should not be removed without explicit confirmation

---

## Multi-Language Projects

**If multiple languages detected, dispatch agents in parallel:**

**Example 1: Tauri project (Rust + TypeScript)**

```
Launch two agents simultaneously:
1. rust-expert with Rust instructions (analyze src-tauri/)
2. react-expert with TypeScript instructions (analyze src/)

Wait for both to complete before validation step.
```

**Example 2: Microservices monorepo (Go + Python)**

```
Launch two agents simultaneously:
1. go-expert with Go instructions (analyze services/api-gateway/)
2. python-expert with Python instructions (analyze services/data-processor/)

Wait for both to complete before validation step.
```

**Example 3: Full-stack project (Java backend + React frontend)**

```
Launch two agents simultaneously:
1. java-expert with Java instructions (analyze backend/)
2. react-expert with TypeScript instructions (analyze frontend/)

Wait for both to complete before validation step.
```

**Validation for multi-language:**

- Run validation for each language separately
- If any language fails, report which one and stop

---

## Error Recovery

If an agent reports failure:

1. Capture the error message
2. Continue with other agents (if multi-language)
3. Report all errors in final summary
4. Suggest manual intervention for failed language

If validation fails:

1. Show the error output
2. Suggest: `git diff` to review changes
3. Suggest: `git restore .` to revert
4. DO NOT auto-revert without user confirmation
