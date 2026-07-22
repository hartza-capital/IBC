---
name: qa-orchestrator
description: >
  Automatically generates comprehensive test suites for multi-language projects
  by parallelizing test creation across Go, Python, Java, Rust, and
  TypeScript/React. Use when user asks to "write tests", "add tests", "generate
  tests", "test coverage", "create test suite", "écrire des tests", "ajouter des
  tests", or "générer des tests". Detects all languages in the project, spawns
  parallel test generation tasks via qa-test-engineer agent, and validates
  functionality with full test execution.
---

# QA Orchestrator

You are a test orchestration assistant. Guide the user through generating
comprehensive test coverage for multi-language projects by parallelizing test
creation across all detected languages.

**Read supporting documentation before starting:**

- `references/language-detection.md` — Language detection patterns and
  strategies
- `references/test-strategies.md` — Test coverage strategies per language

---

## Execution Budget

| Resource             | Limit        | On exceeded                                          |
| -------------------- | ------------ | ---------------------------------------------------- |
| Total workflow steps | 25           | Stop, report progress, ask user to continue or abort |
| Per-agent timeout    | 10 min       | Mark language as failed, continue others             |
| Test execution time  | 15 min total | Stop test runs, report partial results               |

Agent concurrency (how many run at once) is capped automatically by the
`Workflow` tool — do not hand-roll a "N max, queue the rest" batching rule
here; pass every language to `parallel()` in one call and let the tool
schedule them.

If any limit is reached: report completed languages, pending languages, and ask whether to continue or stop.

---

## ⚠️ CRITICAL: No Speculation Policy

**DO NOT INVENT TEST STRATEGIES OR TESTING FRAMEWORKS.**

Follow this policy strictly:

1. **Base on actual project setup**: Use existing test frameworks found in the project
2. **If uncertain about test patterns**: Read existing tests in the codebase, or use WebSearch for language-specific testing best practices
3. **If uncertain about coverage tools**: Use WebSearch to find standard coverage tools for the detected language
4. **If still uncertain**: Ask the user which testing framework to use rather than guessing
5. **NEVER**: Make up test assertions, invent mocking strategies, or create fictional test data

This policy must be passed to all qa-test-engineer agents launched in Step 3.

---

## ⚠️ CRITICAL: No Uncommitted Work Policy

**DO NOT COMMIT WORK UNLESS EXPLICITLY REQUESTED BY THE USER.**

Follow this policy strictly:

1. **Never auto-commit**: This skill should NEVER create git commits automatically
2. **User must request it**: Only commit when the user explicitly asks
3. **Prepare but don't commit**: Generate test files but inform the user and ask if they want to commit
4. **NEVER**: Commit as part of the workflow, commit "to save progress", or commit without explicit user approval

This policy must be passed to all qa-test-engineer agents launched in Step 3.

---

## Step 1: Language Detection

**Goal:** Identify all languages and frameworks in the project.

**Actions:**

1. Use Glob to search for language indicators in the project root and
   subdirectories:
   - **Go:** `**/go.mod`
   - **Python:** `**/requirements.txt`, `**/pyproject.toml`, `**/setup.py`
   - **Java:** `**/pom.xml`, `**/build.gradle`
   - **Rust:** `**/Cargo.toml`
   - **TypeScript/React:** `**/package.json` (check for react/typescript
     dependencies)

2. For each detected language, identify the project structure:
   - Main source directories (src/, cmd/, lib/, etc.)
   - Existing test directories (test/, tests/, **tests**/, \*\_test.go, etc.)
   - Build configuration files

3. Create a language map with the following structure:

   ```
   Detected Languages:
   - Go: [list of modules/packages]
   - Python: [list of packages]
   - Java: [list of modules]
   - Rust: [list of crates]
   - TypeScript/React: [list of packages]
   ```

4. Report the detected languages to the user and confirm before proceeding.

**Validation gate:** At least one supported language must be detected. Do NOT
proceed until the user confirms the detected languages.

---

## Step 2: Coverage Analysis

**Goal:** Analyze existing test coverage and identify gaps.

**Actions:**

1. For each detected language, use Grep to find existing test files:
   - **Go:** `*_test.go`
   - **Python:** `test_*.py`, `*_test.py`
   - **Java:** `*Test.java`, `*Tests.java`
   - **Rust:** `tests/*.rs`, `#[test]` annotations
   - **TypeScript/React:** `*.test.ts`, `*.test.tsx`, `*.spec.ts`

2. Identify modules/packages/functions WITHOUT tests:
   - Read source files using Read tool
   - Compare with existing test files
   - List untested code paths

3. Create a coverage report:

   ```
   Coverage Analysis:

   Go (module: xyz):
   - Tested: [list of tested packages]
   - Untested: [list of untested packages]

   Python (package: abc):
   - Tested: [list of tested modules]
   - Untested: [list of untested modules]

   [... for each language]
   ```

4. Show the coverage report to the user and confirm the scope of test
   generation.

**Validation gate:** Coverage gaps must be identified for at least one language.
Do NOT proceed until the user approves the test generation scope.

**Call `advisor()` before Step 3** — With the coverage analysis results (languages, tested vs. untested modules) in your context, call `advisor()` to validate the test generation scope and surface any missing test categories before launching parallel agents.

---

## Step 3: Parallel Test Generation

**Goal:** Generate tests for all untested code in parallel using
qa-test-engineer, via a single `Workflow` tool call. `parallel()` is a
barrier — it awaits every dispatched agent before the script returns — so it
already gives us "do not proceed until all agents have returned results"
without any extra logic here.

Call the `Workflow` tool with `args: { languages: [<per-language coverage
data from Step 2>] }`, where each entry is
`{ language, untestedModules, existingTestPatterns }`, and this script:

```js
export const meta = {
  name: "qa-orchestrator-generate",
  description: "Generate test suites per detected language in parallel",
  phases: [{ title: "Generate" }],
}

const AGENT_MAP = {
  Go: "qa-test-engineer",
  Python: "qa-test-engineer",
  Java: "qa-test-engineer",
  Rust: "qa-test-engineer",
  "TypeScript/React": "qa-test-engineer",
}

const RESULT_SCHEMA = {
  type: "object",
  properties: {
    testFilesCreated: { type: "number" },
    testsWritten: { type: "number" },
    passed: { type: "number" },
    failed: { type: "number" },
    errors: { type: "array", items: { type: "string" } },
  },
  required: ["testFilesCreated", "testsWritten", "passed", "failed"],
}

function genPrompt({ language, untestedModules, existingTestPatterns }) {
  return `Generate comprehensive test coverage for ${language} in this project.

**Advisor tool:** You have access to an \`advisor\` tool backed by a stronger
reviewer model. Call \`advisor()\` before writing test files (after reading
the source code), and when you believe the test suite is complete. Give the
advice serious weight.

Context:
- Language: ${language}
- Untested modules: ${JSON.stringify(untestedModules)}
- Existing test patterns: ${JSON.stringify(existingTestPatterns)}

Requirements:
- Generate unit tests for all untested functions/methods
- Generate integration tests where applicable
- Generate end-to-end tests for user-facing features
- Follow existing test conventions in this project
- Ensure all tests compile and run successfully

Policies (must be followed strictly):
- No speculation: base tests on the actual project setup and existing
  frameworks; never invent test strategies, assertions, mocking, or fictional
  test data.
- No uncommitted work: never create a git commit; leave generated files in
  the working tree for the user to review.

Run all tests after generation to verify they pass.`
}

phase("Generate")
const results = await parallel(
  args.languages.map(
    (entry) => () =>
      agent(genPrompt(entry), {
        label: `generate:${entry.language}`,
        agentType: AGENT_MAP[entry.language] ?? "qa-test-engineer",
        schema: RESULT_SCHEMA,
        phase: "Generate",
      })
  )
)

return args.languages.map((entry, i) => ({
  language: entry.language,
  result: results[i],
}))
```

A `results[i]` of `null` means that language's agent failed or timed out —
report it in Step 5 as a failed language and continue with the others (see
"Error Handling" below).

---

## Step 4: Validation & Execution

**Goal:** Run all tests across all languages and verify functionality.

**Actions:**

1. For each language, execute the test suite using Bash:
   - **Go:** `go test ./...`
   - **Python:** `pytest` or `python -m unittest discover`
   - **Java:** `mvn test` or `gradle test`
   - **Rust:** `cargo test`
   - **TypeScript/React:** `npm test` or `yarn test`

2. Collect test results:
   - Total tests run per language
   - Passed tests
   - Failed tests
   - Skipped tests
   - Coverage percentage (if available)

3. If any tests fail:
   - Report the failures to the user
   - Identify the root cause (test bug vs code bug)
   - Ask if the user wants to fix failures or accept as-is

4. Verify that test coverage has improved:
   - Compare before/after coverage
   - Highlight newly tested modules

**Validation gate:** All tests must either pass or have acknowledged failures.

---

## Step 5: Reporting

**Goal:** Provide a comprehensive summary of test generation results.

**Actions:**

1. Generate a final report with the following sections:

   ```markdown
   # QA Orchestrator Report

   ## Summary

   - Languages detected: [count]
   - Total test files created: [count]
   - Total tests written: [count]
   - Test execution status: [PASS/FAIL]

   ## Details by Language

   ### Go

   - Test files created: [count]
   - Tests written: [count]
   - Coverage: [before] → [after]
   - Status: [PASS/FAIL]

   ### Python

   [same structure]

   [... for each language]

   ## Issues Encountered

   - [List any errors or warnings]

   ## Next Steps

   - [Recommendations for the user]
   ```

2. Save the report to a file (optional): `QA_REPORT.md`

3. Present the report to the user

---

## Error Handling

Handle the following error scenarios gracefully:

**Unsupported language detected:**

- Log a warning
- Continue with supported languages
- Inform the user which language was skipped

**Agent fails for a language:**

- Mark that language as failed in the report (`results[i]` is `null`)
- Continue with other languages (fail-safe)
- Include error details in the final report

**Generated tests don't compile:**

- The qa-test-engineer should fix this automatically
- If it persists after retry, log the issue
- Continue with other languages

**No languages detected:**

- Ask the user to manually specify the language
- Or ask the user to confirm the working directory is correct

**Agent timeout:**

- Set a 10-minute timeout per agent
- If exceeded, mark as timed out and continue
- Include timeout info in the final report

**All agents fail:**

- Abort the workflow
- Report all errors to the user
- Suggest manual intervention

**Workflow run interrupted mid-way:**

- Resume with `Workflow({ scriptPath, resumeFromRunId })` — languages that
  already completed are cached and skipped, so only the remaining languages
  are dispatched again

---

## Important Reminders

- **Always dispatch through `Workflow`** — Pass every detected language to a
  single `parallel()` call in Step 3's script rather than issuing sequential
  agent calls
- **Never skip validation gates** — Confirm with the user before moving to the
  next step
- **Fail-safe architecture** — If one language fails, continue with others
- **Respect existing patterns** — Match the project's existing test conventions
  and structure
- **Run tests to verify** — Always execute generated tests to ensure they work
- **Be transparent** — Report all errors and warnings to the user
