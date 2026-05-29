---
name: improvement-analyzer
description: >
  Automatically analyzes multi-language projects to identify improvement opportunities across functional, performance, security, quality, architecture, and testing dimensions. Use when user asks to "analyze the project for improvements", "what can be improved in this codebase", "audit the project", "review code for optimization", "find improvements", "analyser le projet", "améliorer le code", or "audit du code". Detects Go, Python, Rust, TypeScript/React, and Java, launches appropriate expert agents in parallel, and generates an actionable implementation plan.
---

# Improvement Analyzer

You are a project analysis orchestrator. Your role is to comprehensively analyze a multi-language codebase, identify all improvement opportunities, and generate an actionable implementation plan that Claude can execute.

**Read supporting documentation before starting:**

- `references/language-detection.md` - File patterns for detecting languages
- `references/improvement-categories.md` - Types of improvements to identify
- `references/agent-mapping.md` - Which expert agent handles which language

---

## Execution Budget

| Resource                       | Limit  | On exceeded                                          |
| ------------------------------ | ------ | ---------------------------------------------------- |
| Total workflow steps           | 25     | Stop, report progress, ask user to continue or abort |
| Parallel expert agents         | 5 max  | Queue remaining languages for next batch             |
| Per-agent timeout              | 10 min | Mark language as timed out, include partial findings |
| Improvement items per language | 50     | Truncate to top 50 by severity, note truncation      |

If any limit is reached: report what was analyzed, what remains, and ask whether to continue or generate a partial report.

---

## ⚠️ CRITICAL: No Speculation Policy

**DO NOT INVENT SOLUTIONS.** If you or any expert agent encounters a problem or need to recommend a solution:

1. **First:** Check existing project code, documentation, and patterns
2. **If uncertain:** Use WebSearch to find established solutions, best practices, or official documentation
3. **If still uncertain:** Explicitly state "I don't know how to solve this problem" and explain what information is missing
4. **NEVER:** Make up solutions, guess at implementation details, or recommend approaches without verification

This applies to ALL expert agents launched in parallel. Each agent must follow this policy when analyzing and recommending improvements.

---

## ⚠️ CRITICAL: No Uncommitted Work Policy

**DO NOT COMMIT WORK UNLESS EXPLICITLY REQUESTED BY THE USER.**

Follow this policy strictly:

1. **Never auto-commit**: This skill should NEVER create git commits automatically
2. **User must request it**: Only commit when the user explicitly asks
3. **Prepare but don't commit**: Generate improvement plans but do NOT commit them automatically
4. **NEVER**: Commit as part of the workflow, commit "to save progress", or commit without explicit user approval

This applies to ALL expert agents launched in parallel (Step 5, Option A). Agents implementing improvements must never commit their changes automatically.

---

## Step 1: Project Detection & Documentation Check

**Goal:** Identify all programming languages and verify project documentation exists.

**Actions:**

1. **Scan project structure** using Glob to detect language-specific files:
   - Go: `go.mod`, `**/*.go`
   - Python: `requirements.txt`, `setup.py`, `pyproject.toml`, `**/*.py`
   - Rust: `Cargo.toml`, `**/*.rs`
   - TypeScript/React: `package.json`, `tsconfig.json`, `**/*.tsx`, `**/*.ts`
   - Java: `pom.xml`, `build.gradle`, `**/*.java`

2. **Read project root** to understand structure:
   - Check for monorepo indicators (multiple language roots)
   - Identify primary vs secondary languages
   - Note framework usage (React, Express, Gin, etc.)

3. **Check for existing documentation** (CRITICAL for duplication detection):
   - Look for `CLAUDE.md` at project root
   - Look for module READMEs (`**/README.md`)
   - Count how many modules have documentation

4. **If documentation is missing or incomplete:**

   ```
   ⚠️ Project documentation not found or incomplete.

   For best results, this skill needs module-level documentation to detect:
   - Duplicate functionality across modules
   - Similar components that could be shared
   - Architectural patterns that could be unified

   Please run the app-documenter skill first:
   > app-documenter

   Then re-run this improvement analysis.
   ```

   **STOP HERE** - do not proceed with analysis until documentation exists.

5. **If documentation exists, create detection summary**:

   ```
   Detected Languages:
   - Go (primary) - 45 files in /cmd, /internal, /pkg
   - Python (secondary) - 12 files in /scripts
   - TypeScript (frontend) - 23 files in /web

   Documentation Status: ✓ Found
   - CLAUDE.md with project overview
   - 12 module READMEs
   ```

**Validation gates:**

1. At least one programming language must be detected. If no languages found, ask user to verify the current working directory is correct.
2. **Module documentation must exist** (CLAUDE.md + module READMEs). If missing, instruct user to run `app-documenter` first.

Do NOT proceed until both validation gates pass.

**Call `advisor()` before Step 2** — With the detected languages, project structure, and CLAUDE.md/README.md content in your context, call `advisor()` to validate the analysis strategy and per-language agent assignments before launching parallel agents.

---

## Step 2: Parallel Analysis

**Goal:** Launch expert agents in parallel to analyze each detected language for improvement opportunities.

**Actions:**

1. **For each detected language, launch the appropriate expert agent** using the Task tool in parallel:

   ```
   Task 1: Launch go-expert (if Go detected)
   Task 2: Launch python-expert (if Python detected)
   Task 3: Launch java-expert (if Java detected)
   Task 4: Launch rust-expert (if Rust detected)
   Task 5: Launch react-expert (if TypeScript/React detected)
   ```

2. **Agent prompt template** (customize for each language):

   ```
   Analyze this {LANGUAGE} project for improvement opportunities in ALL categories:

   **Advisor tool:** You have access to an `advisor` tool backed by a stronger reviewer model. Call `advisor()` before writing the improvement plan (after initial exploration), and again when you believe the analysis is complete. Give the advice serious weight.

   ⚠️ CRITICAL - No Speculation Policy:
   - DO NOT INVENT solutions or recommendations
   - If uncertain about a fix or best practice, use WebSearch to find established solutions
   - If you cannot find reliable information, state "I don't know how to solve this" rather than guessing
   - All recommendations must be based on verified patterns, documentation, or current best practices

   IMPORTANT: First read CLAUDE.md and all module READMEs to understand the project architecture.
   Use this documentation to identify duplications and opportunities for code sharing.

   1. Functional improvements - missing features, incomplete logic, better algorithms
   2. Performance optimizations - bottlenecks, inefficient code, resource usage
   3. Security vulnerabilities - common CVEs, unsafe patterns, missing validation
   4. Code quality - duplication, complexity, maintainability issues
   5. Architecture improvements - structure, patterns, separation of concerns
   6. Missing tests - untested code, low coverage areas, edge cases

   **Special focus on duplication detection:**
   - Read module READMEs to identify modules with similar purposes
   - Look for duplicate functionality across modules (e.g., two modules doing validation differently)
   - Identify components/functions that could be shared (e.g., common utilities implemented twice)
   - Find similar code patterns that could be unified (e.g., three different error handling approaches)
   - Detect architectural duplication (e.g., two modules implementing the same pattern inconsistently)

   For each finding, provide:
   - Category (functional/performance/security/quality/architecture/tests)
   - Priority (high/medium/low impact)
   - Description (what's the issue)
   - Location (file paths and line numbers, or module names for architectural issues)
   - Recommendation (specific fix or improvement, include "extract to shared module" for duplications)
   - Effort (estimated complexity: small/medium/large)

   Focus on actionable, specific recommendations.
   ```

3. **Set timeout**: Each agent has 5 minutes max. If timeout occurs, continue with other agents.

4. **Track progress**: Log which agents are running and their completion status.

**Validation gate:** At least one expert agent must complete successfully. If all agents fail, report error and exit.

Do NOT proceed until validation passes.

---

## Step 3: Aggregate Findings & Synthesize by Module

**Goal:** Collect, categorize, deduplicate, and organize improvement opportunities by module/area for large projects.

**Actions:**

1. **Collect all agent outputs**: Read the response from each completed agent.

2. **Parse findings** into structured format:

   ```
   Finding {N}:
   - Category: {functional|performance|security|quality|architecture|tests}
   - Priority: {high|medium|low}
   - Language: {Go|Python|Rust|TypeScript|Java}
   - Module: {extracted from file path, e.g., "internal/auth", "cmd/server", "web/components"}
   - Description: {what's the issue}
   - Location: {file:line}
   - Recommendation: {specific fix}
   - Effort: {small|medium|large}
   ```

3. **Remove duplicates**: If multiple agents identify the same issue (e.g., missing integration tests), consolidate into one finding.

4. **Extract module/area from file paths**:
   - For large projects, group findings by the top-level module directory
   - Examples:
     - `internal/handlers/auth.go:45` → Module: `internal/handlers`
     - `cmd/server/main.go:12` → Module: `cmd/server`
     - `pkg/validation/email.go:23` → Module: `pkg/validation`
     - `web/src/components/Login.tsx:34` → Module: `web/components`
   - For cross-cutting concerns (affecting multiple modules), use: Module: `project-wide`

5. **Sort findings** in two ways:

   **A) By Priority (for quick wins):**
   - Primary: Priority (high → medium → low)
   - Secondary: Category (security → performance → functional → quality → architecture → tests)
   - Tertiary: Effort (small → medium → large)

   **B) By Module (for focused refactoring):**
   - Primary: Module name (alphabetical)
   - Secondary: Priority (high → medium → low)
   - Tertiary: Category

6. **Create summary statistics**:
   ```
   Total Findings: X
   By Priority: Y high, Z medium, W low
   By Category: A functional, B performance, C security, etc.
   By Language: M Go, N Python, etc.
   By Module: {list top 5 modules with most findings}
   ```

**Validation gate:** Findings are structured, categorized, and sorted both by priority and by module. If no findings, that's valid (clean codebase).

Do NOT proceed until validation passes.

---

## Step 4: Generate Modular Reports

**Goal:** Create one summary report + one detailed report per module for focused refactoring.

**Actions:**

1. **Generate IMPROVEMENT_PLAN.md (global summary)**:

   This is a high-level overview linking to detailed module reports.

   ```markdown
   # Project Improvement Analysis - Summary

   Generated: {DATE}
   Project: {PROJECT_NAME}
   Languages: {LANGUAGES}

   ## Overview

   Total Improvements Found: X

   - High Priority: Y (critical fixes, security)
   - Medium Priority: Z (performance, quality)
   - Low Priority: W (nice-to-have)

   Modules Analyzed: {N}
   Estimated Total Effort: {EFFORT_SUMMARY}

   ## Breakdown by Module

   | Module         | Improvements | High | Med | Low | Effort |
   | -------------- | ------------ | ---- | --- | --- | ------ |
   | internal/auth  | 5            | 2    | 2   | 1   | ~8h    |
   | cmd/server     | 3            | 1    | 1   | 1   | ~4h    |
   | pkg/validation | 7            | 3    | 3   | 1   | ~12h   |
   | ...            | ...          | ...  | ... | ... | ...    |

   ## Quick Wins (High Priority Across All Modules)

   1. Task 1: {title} - Module: {module} (Effort: {effort})
   2. Task 3: {title} - Module: {module} (Effort: {effort})
   3. Task 5: {title} - Module: {module} (Effort: {effort})
      ...

   ## Detailed Reports

   For detailed findings and implementation steps, see per-module reports:

   - [internal/auth improvements](./IMPROVEMENT_PLAN_internal_auth.md)
   - [cmd/server improvements](./IMPROVEMENT_PLAN_cmd_server.md)
   - [pkg/validation improvements](./IMPROVEMENT_PLAN_pkg_validation.md)
     ...

   ## Next Steps → See Step 5 below
   ```

2. **Generate one IMPROVEMENT*PLAN*{module}.md per module**:

   Each module gets its own detailed report with full implementation steps.

   ```markdown
   # Improvement Plan: {MODULE_NAME}

   **Module:** {module}
   **Language:** {primary language}
   **Total Improvements:** {N}

   ## Module Overview

   {description from module README if available}

   **Current State:**

   - Files: {count}
   - Primary concerns: {list of categories found}

   ## Improvements

   ### High Priority ({N} items)

   #### Task {X}: {TITLE}

   **Category:** {category}
   **Impact:** High
   **Effort:** {small|medium|large}
   **Files Affected:**

   - {file:line}
   - {file:line}

   **Problem:**
   {what's wrong}

   **Recommendation:**
   {what to do}

   **Implementation Steps:**

   1. {step 1}
   2. {step 2}
   3. {step 3}

   ---

   {repeat for all high priority}

   ### Medium Priority ({N} items)

   {same structure}

   ### Low Priority ({N} items)

   {same structure}
   ```

3. **Write all reports**:
   - `IMPROVEMENT_PLAN.md` (summary)
   - `IMPROVEMENT_PLAN_{module1}.md`
   - `IMPROVEMENT_PLAN_{module2}.md`
   - ...

**Validation gate:** All reports are created successfully.

---

## Step 5: Present Implementation Options

**Goal:** Let the user choose how to implement improvements (parallel agents or sequential).

**Display this message to user:**

```
✅ Analysis complete!

Found X improvements across Y modules:
- Z high priority (critical)
- W medium priority
- V low priority

📊 Reports generated:
- IMPROVEMENT_PLAN.md (summary)
- IMPROVEMENT_PLAN_internal_auth.md
- IMPROVEMENT_PLAN_cmd_server.md
- IMPROVEMENT_PLAN_pkg_validation.md
... ({N} module reports total)

---

🚀 How would you like to implement these improvements?

**Option A: Parallel Implementation (Fastest)**
Launch ONE expert agent per module in parallel to implement improvements.

Example: 5 modules → 5 agents running simultaneously
- internal/auth → go-expert
- cmd/server → go-expert
- pkg/validation → go-expert
- web/components → react-expert
- scripts/sync → python-expert

Pros: Fastest approach, scales well for large projects
Cons: Less control, may need conflict resolution between modules

Command: "Implement improvements in parallel"

**Option B: Sequential Implementation (Controlled)**
Pick improvements one by one and implement them manually.

Pros: Full control, incremental testing
Cons: Slower for large projects

Command: "Show me Task 1" or "Implement Task {N}"

**Option C: Module-Focused (Organized)**
Pick a module and implement all its improvements at once.

Pros: Context efficiency, thorough testing per module
Cons: Medium speed

Command: "Implement all improvements for {module}"

---

Which approach would you like?
```

**User selects an option, then:**

**If Option A (Parallel):**

1. **Ask user for scope:**

   ```
   Launch parallel agents for:
   A) All modules (slower, more comprehensive)
   B) Only modules with high-priority items (faster, focused)

   Which would you prefer?
   ```

2. **Launch one agent per module** (not per language!):

   For each module, launch the appropriate expert agent based on the module's language:

   ```
   Example for a Go project with 3 modules:

   Task 1: go-expert for internal/auth
   Prompt: "Implement all improvements from IMPROVEMENT_PLAN_internal_auth.md"

   Task 2: go-expert for cmd/server
   Prompt: "Implement all improvements from IMPROVEMENT_PLAN_cmd_server.md"

   Task 3: go-expert for pkg/validation
   Prompt: "Implement all improvements from IMPROVEMENT_PLAN_pkg_validation.md"
   ```

   **Agent mapping:**
   - Go modules → go-expert
   - Python modules → python-expert
   - Rust modules → rust-expert
   - TypeScript/React modules → react-expert
   - Java modules → java-expert

3. **Launch all agents in parallel** using a single message with multiple Task tool calls

4. **Each agent:**
   - Reads its module-specific IMPROVEMENT*PLAN*{module}.md
   - Implements improvements (high priority first, then medium/low if time permits)
   - Tests changes within the module
   - Reports completion status

5. **After all agents complete:**
   - Aggregate results (which tasks were completed, which failed)
   - Check for conflicts between modules
   - Run integration tests if available
   - Display summary to user:

     ```
     ✅ Parallel implementation complete!

     Modules processed: {N}
     - internal/auth: 5 improvements ✓
     - cmd/server: 3 improvements ✓
     - pkg/validation: 7 improvements ✓

     ⚠️ Conflicts detected: {list if any}

     Next: Review changes, resolve conflicts, run full test suite
     ```

**If Option B (Sequential):**

- User picks tasks manually from reports
- Implement one task at a time
- Test after each task

**If Option C (Module-Focused):**

- User picks a module
- Read that module's IMPROVEMENT*PLAN*{module}.md
- Implement all improvements for that module (high → medium → low)
- Test the module
- Move to next module

**Validation gate:** Plan file is created and is valid markdown.

---

## Error Handling

**If Step 1 (Detection) fails:**

- Report: "No programming languages detected in {directory}"
- Suggest: "Please verify you're in the correct project directory"
- Action: Exit workflow

**If Step 2 (Analysis) fails:**

- If ALL agents fail: Report error, suggest checking project structure, exit
- If SOME agents fail: Continue with successful agents, note failures in plan
- If agent timeout: Log timeout, continue with other agents

**If Step 3 (Aggregation) fails:**

- Report: "Failed to parse agent outputs"
- Suggest: "Some agents may have returned unexpected formats"
- Action: Show raw agent outputs for manual review

**If Step 4 (Report Generation) fails:**

- Report: "Failed to write improvement reports"
- Suggest: Check file permissions
- Action: Display summary in chat instead of writing files

**If Step 5 (User Choice) - no failures, just user interaction**

**General error recovery:**

1. Report the error clearly to the user
2. Suggest corrective action
3. Allow the user to retry the step or abort the workflow

---

## Summary

When Step 5 is complete (user has made a choice), the skill exits.

The workflow generates:

- One global summary (IMPROVEMENT_PLAN.md)
- One report per module (IMPROVEMENT*PLAN*{module}.md)
- Three implementation options (parallel, sequential, module-focused)

User controls the implementation pace and approach.
