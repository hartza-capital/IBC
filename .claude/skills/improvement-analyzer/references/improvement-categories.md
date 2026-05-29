# Improvement Categories

This document defines the types of improvements to identify during project analysis.

---

## 1. Functional Improvements

**Definition:** Missing features, incomplete logic, better algorithms, or enhanced functionality.

**What to look for:**

- **Missing features:**
  - Incomplete CRUD operations (e.g., Create and Read exist, but no Update or Delete)
  - Partial implementation of a workflow (e.g., user registration but no password reset)
  - Missing edge case handling (e.g., no pagination for large datasets)

- **Better algorithms:**
  - Inefficient sorting/searching (e.g., O(n²) when O(n log n) is possible)
  - Suboptimal data structures (e.g., array when a map would be better)
  - Missed opportunities for caching or memoization

- **Enhanced logic:**
  - Hardcoded values that should be configurable
  - Repeated logic that could be abstracted
  - Business rules that could be more flexible

**Examples:**

- "Add DELETE endpoint for `/users/:id` to complete CRUD operations"
- "Replace linear search in `findUserById` with map lookup"
- "Add rate limiting to API endpoints to prevent abuse"

**Priority guidelines:**

- High: Missing critical functionality that blocks workflows
- Medium: Enhancements that improve usability or flexibility
- Low: Nice-to-have features or minor algorithmic improvements

---

## 2. Performance Optimizations

**Definition:** Bottlenecks, inefficient code, excessive resource usage, or slow operations.

**What to look for:**

- **CPU bottlenecks:**
  - Nested loops with high iteration counts
  - Expensive operations inside loops (e.g., regex compilation, JSON parsing)
  - Inefficient string concatenation or manipulation

- **Memory issues:**
  - Memory leaks (e.g., unreleased resources, unbounded caches)
  - Large object allocations that could be avoided
  - Unnecessary deep copies of data structures

- **I/O inefficiencies:**
  - N+1 query problems in database access
  - Synchronous I/O when async would be better
  - Missing connection pooling or keep-alive

- **Network overhead:**
  - Missing gzip compression for API responses
  - Large payload sizes (e.g., fetching entire objects when only a few fields are needed)
  - Excessive API calls that could be batched

**Examples:**

- "Use connection pooling in `database.go` to reduce connection overhead"
- "Add caching for `getProductById` API to avoid repeated database queries"
- "Compile regex patterns once at startup instead of on each request"

**Priority guidelines:**

- High: Severe bottlenecks that cause timeouts or crashes
- Medium: Noticeable slowness or resource waste
- Low: Minor inefficiencies with negligible impact

---

## 3. Security Vulnerabilities

**Definition:** Common CVEs, unsafe patterns, missing validation, or security weaknesses.

**What to look for:**

- **Input validation:**
  - Missing validation on user inputs (XSS, SQL injection risks)
  - Insufficient sanitization of file uploads
  - No rate limiting on sensitive endpoints (brute force risk)

- **Authentication & Authorization:**
  - Missing authentication checks on protected endpoints
  - Weak password requirements
  - Insecure session management (e.g., no expiry, predictable tokens)
  - Missing CSRF protection

- **Data exposure:**
  - Sensitive data logged or returned in errors
  - Missing encryption for sensitive data at rest or in transit
  - Excessive permissions granted (e.g., database user has DROP privileges)

- **Dependencies:**
  - Outdated dependencies with known vulnerabilities
  - Unused dependencies that expand attack surface

**Examples:**

- "Add input validation for email parameter in `/register` endpoint (XSS risk)"
- "Upgrade `axios` to v1.6.5 to fix CVE-2023-45857"
- "Remove database credentials from error messages in `database.go:45`"

**Priority guidelines:**

- High: Critical vulnerabilities (RCE, auth bypass, data exposure)
- Medium: Medium-severity risks (XSS, CSRF, weak validation)
- Low: Low-severity issues (dependency updates, hardening)

---

## 4. Code Quality

**Definition:** Duplication, complexity, maintainability issues, or poor readability.

**What to look for:**

- **Code duplication (use documentation for detection):**

  **IMPORTANT:** Read CLAUDE.md and module READMEs first to identify duplications at the architectural level.

  **Module-level duplication (high-value findings):**
  - Multiple modules with similar purposes (e.g., "user validation" in both `auth/` and `api/`)
  - Duplicate functionality across modules (e.g., two different logging implementations)
  - Similar components that could be extracted to a shared module (e.g., common utilities)
  - Inconsistent implementations of the same pattern (e.g., three different error handling approaches)

  **File-level duplication (code-level findings):**
  - Identical or near-identical code blocks within files
  - Similar logic that could be abstracted into a helper function
  - Copy-pasted boilerplate that could use a template or generator

  **How to detect using documentation:**
  1. Read CLAUDE.md to understand the project structure and module purposes
  2. Read each module's README to understand what it does
  3. Look for modules with overlapping responsibilities
  4. Identify modules that could share code but don't
  5. Check for inconsistent patterns across modules

- **Complexity:**
  - Functions exceeding 50 lines (should be broken down)
  - Cyclomatic complexity > 10 (too many branches)
  - Deep nesting (> 4 levels of indentation)

- **Maintainability:**
  - Magic numbers or strings without constants
  - Unclear variable/function names
  - Missing or outdated comments/documentation
  - Inconsistent code style or formatting

- **Anti-patterns:**
  - God objects (classes/modules doing too much)
  - Tight coupling between modules
  - Global state or singletons misused

**Examples:**

**Module-level duplications:**

- "Modules `auth/` and `api/validators/` both implement email validation. Extract to shared `pkg/validation/` module"
- "Error handling differs between `internal/handlers/` and `internal/services/`. Standardize using a shared error package"
- "Both `cmd/server/` and `cmd/worker/` have duplicate configuration loading. Extract to `pkg/config/`"

**File-level duplications:**

- "Extract duplicate error handling in `api/*.go` into a shared middleware"
- "Refactor `processOrder` function (120 lines) into smaller, focused functions"
- "Replace magic number `86400` with constant `SECONDS_IN_DAY`"

**Priority guidelines:**

- High: Module-level duplications causing inconsistency or bugs across the codebase
- Medium: File-level duplications affecting maintainability
- Low: Minor style or readability improvements

---

## 5. Architecture Improvements

**Definition:** Structure, patterns, separation of concerns, or design decisions.

**What to look for:**

- **Layering:**
  - Missing separation between presentation, business logic, and data layers
  - Business logic leaked into controllers or handlers
  - Data access logic mixed with business logic

- **Modularity:**
  - Monolithic files/modules that should be split
  - Circular dependencies between packages
  - Tight coupling that prevents independent testing

- **Patterns:**
  - Missing dependency injection (hard to test or swap implementations)
  - Lack of interfaces for abstractions
  - Missing or misused design patterns (Factory, Strategy, Repository, etc.)

- **Scalability:**
  - Single-instance assumptions (no horizontal scaling support)
  - Missing graceful shutdown handling
  - Hardcoded configuration that should be environment-based

**Examples:**

- "Introduce repository pattern for data access to decouple business logic from database"
- "Split `handlers/api.go` (500 lines) into separate handler files per resource"
- "Add dependency injection for services to improve testability"

**Priority guidelines:**

- High: Architectural issues blocking critical work (testing, scaling, changes)
- Medium: Design improvements that would simplify future work
- Low: Theoretical or aesthetic architecture enhancements

---

## 6. Missing Tests

**Definition:** Untested code, low coverage areas, or missing edge case tests.

**What to look for:**

- **Coverage gaps:**
  - No unit tests for critical business logic
  - Missing integration tests for API endpoints
  - No end-to-end tests for key user workflows

- **Edge cases:**
  - Happy path tested, but error cases ignored
  - Boundary conditions not covered (empty input, max values, etc.)
  - Concurrency issues not tested

- **Test quality:**
  - Tests too brittle (break on minor changes)
  - Tests not isolated (depend on external state or other tests)
  - Missing assertions (tests that don't actually verify behavior)

**Examples:**

- "Add unit tests for `calculateDiscount` function (currently 0% coverage)"
- "Add integration tests for POST `/orders` endpoint (error cases missing)"
- "Add edge case tests for empty cart in checkout flow"

**Priority guidelines:**

- High: Critical code with no tests (business logic, security, data integrity)
- Medium: Important code paths with partial coverage
- Low: Non-critical or well-tested code with minor coverage gaps

---

## Categorization Guidelines

When analyzing findings:

1. **One category per finding:** If an issue spans multiple categories, choose the most relevant one.
2. **Prioritize security:** If a finding has both quality and security implications, categorize as security.
3. **Be specific:** Instead of "Code could be better", say "Duplicate validation logic in 3 files (quality)".
4. **Include context:** Always mention file paths, line numbers, and specific code examples.

---

## Example Structured Finding

```
Category: Performance
Priority: High
Language: Go
Description: N+1 query problem in order list endpoint
Location: internal/handlers/orders.go:45-60
Recommendation: Use a JOIN query or preload associations to fetch orders and their items in a single query
Effort: Small (1-2 hours)

Details:
The `ListOrders` function fetches orders in a loop, then for each order fetches its items in a separate query. For 100 orders with 5 items each, this results in 501 queries instead of 1-2.

Impact:
- Current: ~2s response time for 100 orders
- Expected after fix: ~200ms response time

Implementation steps:
1. Modify the SQL query to use LEFT JOIN on order_items
2. Update the row scanning logic to group items by order
3. Add a test to verify the query count is reduced
```
