# Agent Mapping

This document defines which expert agent to use for each programming language.

---

## Language → Agent Mapping

| Language          | Agent         | Subagent Type   | Model  |
| ----------------- | ------------- | --------------- | ------ |
| Go                | go-expert     | `go-expert`     | sonnet |
| Python            | python-expert | `python-expert` | sonnet |
| Java              | java-expert   | `java-expert`   | sonnet |
| Rust              | rust-expert   | `rust-expert`   | sonnet |
| TypeScript/React  | react-expert  | `react-expert`  | sonnet |
| JavaScript (Node) | react-expert  | `react-expert`  | sonnet |

**Note:** All agents use the `sonnet` model for balance between quality and performance.

---

## Agent Capabilities

### go-expert

**Domain:**

- AWS Lambda functions (API Gateway, event-driven)
- ECS tasks and long-running services
- CLI tools
- Backend services and APIs
- Microservices architecture

**Expertise:**

- Go standard library patterns
- Gin, Echo, Chi frameworks
- AWS SDK for Go (v2)
- Database access (PostgreSQL, DynamoDB)
- Concurrency patterns (goroutines, channels)
- Testing (table-driven tests, mocking)

**What to ask for:**

- Performance optimizations (goroutines, memory allocation)
- Idiomatic Go patterns
- Error handling improvements
- Testing gaps
- Security (input validation, SQL injection, etc.)

---

### python-expert

**Domain:**

- AWS Lambda functions
- Trading systems with ib_async (Interactive Brokers)
- Data pipelines and ETL
- CLI tools
- Backend services (FastAPI, Flask, Django)

**Expertise:**

- Python 3.10+ features (type hints, dataclasses, etc.)
- AWS SDK for Python (boto3)
- ib_async for trading
- pandas, numpy for data processing
- pytest for testing
- Async/await patterns

**What to ask for:**

- Performance optimizations (async, caching, vectorization)
- Pythonic patterns and idioms
- Type safety improvements (mypy)
- Testing gaps
- Security (dependency vulnerabilities, input validation)

---

### java-expert

**Domain:**

- IBC (Interactive Brokers Controller)
- IB Gateway automation
- Backend services (Spring Boot, Quarkus)
- Build systems (Maven, Gradle)

**Expertise:**

- Java 17+ features
- Spring Boot ecosystem
- Maven/Gradle build optimization
- JUnit 5 testing
- Concurrency (ExecutorService, CompletableFuture)

**What to ask for:**

- Performance optimizations (thread pools, memory management)
- Modern Java patterns (streams, optionals, records)
- Dependency management
- Testing gaps
- Security (dependency scanning, input validation)

---

### rust-expert

**Domain:**

- CLI tools
- Backend services (Actix, Rocket, Axum)
- Desktop applications (Tauri v2 backend)
- System programming
- Performance-critical code

**Expertise:**

- Rust ownership and borrowing patterns
- Async runtime (Tokio)
- Error handling (Result, anyhow, thiserror)
- Testing (unit, integration, property-based with proptest)
- Unsafe code review

**What to ask for:**

- Performance optimizations (zero-copy, SIMD, allocation reduction)
- Idiomatic Rust patterns
- Error handling improvements
- Testing gaps (especially unsafe code)
- Security (memory safety, unsafe review)

---

### react-expert

**Domain:**

- React web applications
- Desktop applications (Tauri v2 frontend)
- Component architecture
- State management (Redux, Context, Zustand)
- UI/UX implementation

**Expertise:**

- React 18+ (hooks, concurrent features)
- TypeScript for type safety
- Next.js for SSR/SSG
- State management libraries
- CSS-in-JS, Tailwind CSS
- Testing (Jest, React Testing Library, Cypress)

**What to ask for:**

- Performance optimizations (memoization, lazy loading, code splitting)
- Modern React patterns (custom hooks, composition)
- Type safety improvements (TypeScript strict mode)
- Accessibility (a11y)
- Testing gaps
- Security (XSS, CSRF, dependency vulnerabilities)

---

## Launching Agents in Parallel

When multiple languages are detected, launch agents in parallel using the Task tool:

```markdown
I'll launch expert agents to analyze the detected languages:

1. **Go analysis** - Launching go-expert for backend code
2. **TypeScript analysis** - Launching react-expert for frontend code
3. **Python analysis** - Launching python-expert for scripts
```

**Important:**

- Launch all agents in a **single message** with multiple Task tool calls
- Use `run_in_background: false` to wait for results
- Set a reasonable timeout (5 minutes per agent)
- Each agent gets the same analysis prompt template from SKILL.md Step 2

---

## Agent Prompt Customization

While the base prompt (from SKILL.md Step 2) is the same, customize for each language:

### Go-specific additions:

```
Additionally check for:
- Proper use of context.Context for cancellation
- Correct defer usage (especially in loops)
- Race conditions (run with -race flag if possible)
- Missing error wrapping (use fmt.Errorf with %w)
```

### Python-specific additions:

```
Additionally check for:
- Type hint coverage (use mypy --strict)
- Proper use of async/await (no blocking calls in async functions)
- Missing __all__ exports in public modules
- F-string usage over .format() or % formatting
```

### Java-specific additions:

```
Additionally check for:
- Proper resource management (try-with-resources)
- Stream API usage over imperative loops
- Optional usage to avoid null checks
- Record classes for immutable data
```

### Rust-specific additions:

```
Additionally check for:
- Proper error propagation (? operator vs unwrap)
- Lifetime annotations correctness
- Clippy warnings (run cargo clippy)
- Unsafe code justification and correctness
```

### TypeScript/React-specific additions:

```
Additionally check for:
- React hooks rules violations (useEffect dependencies, etc.)
- TypeScript strict mode compliance
- Prop types and component typing
- Accessibility (missing alt, aria labels, keyboard nav)
```

---

## Error Handling

**If an agent fails:**

- Log the failure with the agent name and error
- Continue with other agents
- Include the failure in the final report:
  ```
  ⚠️ Note: python-expert analysis failed (timeout after 5 minutes).
  Python code was not analyzed. You may want to run the analysis manually.
  ```

**If all agents fail:**

- Report a critical error
- Provide troubleshooting steps (check project structure, verify agent availability)
- Exit the workflow

**If an agent returns unexpected output:**

- Try to parse what you can
- Mark findings as "Partial" in the final report
- Suggest manual review of the agent output
