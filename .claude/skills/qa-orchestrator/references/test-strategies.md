# Test Coverage Strategies

Language-specific strategies for generating comprehensive test suites.

---

## General Principles

For all languages, follow the **test pyramid**:

1. **Unit tests (70%)** — Test individual functions/methods in isolation
2. **Integration tests (20%)** — Test interactions between components
3. **End-to-end tests (10%)** — Test complete user workflows

**Coverage goals:**

- **Minimum:** 80% line coverage
- **Target:** 90% line coverage
- **Edge cases:** 100% coverage for critical paths (auth, payment, security)

---

## Go

### Test Structure

- **Unit tests:** `*_test.go` files in the same package as source
- **Integration tests:** `*_integration_test.go` or separate `integration/`
  directory
- **End-to-end tests:** `*_e2e_test.go` or `e2e/` directory

### What to Test

**Functions:**

```go
// Source: internal/calculator/add.go
func Add(a, b int) int { return a + b }

// Test: internal/calculator/add_test.go
func TestAdd(t *testing.T) {
    tests := []struct {
        name string
        a, b int
        want int
    }{
        {"positive numbers", 1, 2, 3},
        {"negative numbers", -1, -2, -3},
        {"zero", 0, 0, 0},
    }
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            got := Add(tt.a, tt.b)
            if got != tt.want {
                t.Errorf("Add() = %v, want %v", got, tt.want)
            }
        })
    }
}
```

**Structs and methods:**

- Test constructor/factory functions
- Test each public method
- Test error handling

**HTTP handlers:**

- Use `httptest` package
- Test status codes, headers, response bodies
- Test middleware

**Database operations:**

- Use test database or mocks
- Test CRUD operations
- Test transaction handling

### Test Patterns

- **Table-driven tests:** Use slices of test cases
- **Subtests:** Use `t.Run()` for each case
- **Test helpers:** Extract common setup/teardown
- **Mocks:** Use interfaces for dependency injection

### Running Tests

```bash
go test ./...                  # Run all tests
go test -v ./...               # Verbose output
go test -cover ./...           # With coverage
go test -race ./...            # Race detector
```

---

## Python

### Test Structure

- **Unit tests:** `tests/test_*.py` or `tests/*_test.py`
- **Integration tests:** `tests/integration/test_*.py`
- **End-to-end tests:** `tests/e2e/test_*.py`

### What to Test

**Functions:**

```python
# Source: src/calculator.py
def add(a: int, b: int) -> int:
    return a + b

# Test: tests/test_calculator.py
import pytest
from src.calculator import add

class TestAdd:
    def test_positive_numbers(self):
        assert add(1, 2) == 3

    def test_negative_numbers(self):
        assert add(-1, -2) == -3

    def test_zero(self):
        assert add(0, 0) == 0

    @pytest.mark.parametrize("a,b,expected", [
        (1, 2, 3),
        (-1, -2, -3),
        (0, 0, 0),
    ])
    def test_add_parametrized(self, a, b, expected):
        assert add(a, b) == expected
```

**Classes:**

- Test `__init__` (constructor)
- Test each public method
- Test properties and setters
- Test dunder methods (`__str__`, `__eq__`, etc.)

**Async functions:**

- Use `pytest-asyncio`
- Test with `@pytest.mark.asyncio`

**Exceptions:**

```python
def test_raises_error():
    with pytest.raises(ValueError, match="invalid input"):
        function_that_raises()
```

### Test Patterns

- **Fixtures:** Use `@pytest.fixture` for setup/teardown
- **Parametrize:** Use `@pytest.mark.parametrize` for multiple cases
- **Mocks:** Use `unittest.mock` or `pytest-mock`
- **Coverage:** Use `pytest-cov`

### Running Tests

```bash
pytest                         # Run all tests
pytest -v                      # Verbose output
pytest --cov=src               # With coverage report
pytest -k "test_add"           # Run specific tests
```

---

## Java

### Test Structure

- **Unit tests:** `src/test/java/` mirroring `src/main/java/` structure
- **Integration tests:** Separate test class or `@Tag("integration")`
- **End-to-end tests:** Separate module or `@Tag("e2e")`

### What to Test

**Classes and methods:**

```java
// Source: src/main/java/com/example/Calculator.java
public class Calculator {
    public int add(int a, int b) { return a + b; }
}

// Test: src/test/java/com/example/CalculatorTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    private final Calculator calculator = new Calculator();

    @Test
    void testAddPositiveNumbers() {
        assertEquals(3, calculator.add(1, 2));
    }

    @Test
    void testAddNegativeNumbers() {
        assertEquals(-3, calculator.add(-1, -2));
    }

    @ParameterizedTest
    @CsvSource({"1,2,3", "-1,-2,-3", "0,0,0"})
    void testAddParameterized(int a, int b, int expected) {
        assertEquals(expected, calculator.add(a, b));
    }
}
```

**Spring Boot applications:**

- Use `@SpringBootTest` for integration tests
- Use `@WebMvcTest` for controller tests
- Use `@DataJpaTest` for repository tests

**REST APIs:**

- Use `MockMvc` for endpoint testing
- Test request/response serialization
- Test error handling

### Test Patterns

- **JUnit 5:** Use `@Test`, `@BeforeEach`, `@AfterEach`
- **Assertions:** Use `assertEquals`, `assertThrows`, etc.
- **Mocks:** Use Mockito (`@Mock`, `@InjectMocks`)
- **Parameterized tests:** Use `@ParameterizedTest`

### Running Tests

```bash
mvn test                       # Maven
gradle test                    # Gradle
mvn test -Dtest=CalculatorTest # Run specific test
```

---

## Rust

### Test Structure

- **Unit tests:** Inline in source files under `#[cfg(test)]` module
- **Integration tests:** `tests/*.rs` files
- **Doc tests:** Examples in documentation comments

### What to Test

**Functions (unit tests):**

```rust
// Source: src/calculator.rs
pub fn add(a: i32, b: i32) -> i32 { a + b }

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_add_positive() {
        assert_eq!(add(1, 2), 3);
    }

    #[test]
    fn test_add_negative() {
        assert_eq!(add(-1, -2), -3);
    }

    #[test]
    fn test_add_zero() {
        assert_eq!(add(0, 0), 0);
    }
}
```

**Structs and traits:**

- Test public methods
- Test trait implementations
- Test error handling with `Result<T, E>`

**Error handling:**

```rust
#[test]
fn test_error_case() {
    let result = function_that_fails();
    assert!(result.is_err());
}

#[test]
#[should_panic(expected = "panic message")]
fn test_panic() {
    function_that_panics();
}
```

**Integration tests:**

```rust
// tests/integration_test.rs
use my_crate::Calculator;

#[test]
fn test_calculator_integration() {
    let calc = Calculator::new();
    assert_eq!(calc.add(1, 2), 3);
}
```

### Test Patterns

- **Unit tests:** Inline with `#[cfg(test)]`
- **Assertions:** Use `assert!`, `assert_eq!`, `assert_ne!`
- **Result testing:** Use `?` operator or `is_ok()` / `is_err()`
- **Doc tests:** Add examples in `///` comments (they run as tests)

### Running Tests

```bash
cargo test                     # Run all tests
cargo test --verbose           # Verbose output
cargo test test_add            # Run specific test
cargo test -- --nocapture      # Show println! output
```

---

## TypeScript/React

### Test Structure

- **Unit tests:** `*.test.ts`, `*.test.tsx` (Jest convention)
- **Component tests:** `*.test.tsx` using React Testing Library
- **Integration tests:** `integration/*.test.ts`
- **End-to-end tests:** `e2e/*.spec.ts` (Playwright/Cypress)

### What to Test

**Functions:**

```typescript
// Source: src/utils/add.ts
export function add(a: number, b: number): number {
  return a + b
}

// Test: src/utils/add.test.ts
import { add } from "./add"

describe("add", () => {
  it("should add positive numbers", () => {
    expect(add(1, 2)).toBe(3)
  })

  it("should add negative numbers", () => {
    expect(add(-1, -2)).toBe(-3)
  })

  it.each([
    [1, 2, 3],
    [-1, -2, -3],
    [0, 0, 0],
  ])("should add %i and %i to get %i", (a, b, expected) => {
    expect(add(a, b)).toBe(expected)
  })
})
```

**React components:**

```tsx
// Source: src/components/Button.tsx
export function Button({ label, onClick }) {
  return <button onClick={onClick}>{label}</button>
}

// Test: src/components/Button.test.tsx
import { render, screen, fireEvent } from "@testing-library/react"
import { Button } from "./Button"

describe("Button", () => {
  it("should render with label", () => {
    render(<Button label="Click me" onClick={() => {}} />)
    expect(screen.getByText("Click me")).toBeInTheDocument()
  })

  it("should call onClick when clicked", () => {
    const handleClick = jest.fn()
    render(<Button label="Click me" onClick={handleClick} />)
    fireEvent.click(screen.getByText("Click me"))
    expect(handleClick).toHaveBeenCalledTimes(1)
  })
})
```

**Hooks:**

- Use `@testing-library/react-hooks` (deprecated, now use `renderHook`)
- Test state updates
- Test side effects

**API calls:**

- Mock `fetch` or axios with `jest.mock()`
- Test loading, success, and error states

### Test Patterns

- **Jest:** Use `describe`, `it`, `expect`
- **React Testing Library:** Use `render`, `screen`, `fireEvent`
- **Mocks:** Use `jest.fn()`, `jest.mock()`
- **Snapshots:** Use `toMatchSnapshot()` (sparingly)

### Running Tests

```bash
npm test                       # Run tests in watch mode
npm test -- --coverage         # With coverage
npm test -- Button.test.tsx    # Run specific test
```

---

## Dispatching to qa-test-engineer

When calling the qa-test-engineer agent, provide:

```
Task(
  subagent_type: "qa-test-engineer",
  description: "Generate tests for [language]",
  prompt: "Generate comprehensive test coverage for [language] in this project.

  Context:
  - Language: [Go/Python/Java/Rust/TypeScript]
  - Project root: [path]
  - Untested modules: [list from coverage analysis]
  - Existing test patterns: [patterns found in detection]
  - Test structure: [unit/integration/e2e locations]

  Requirements:
  - Generate unit tests for all untested functions/methods
  - Generate integration tests where applicable
  - Generate end-to-end tests for user-facing features
  - Follow the test strategies in references/test-strategies.md
  - Match existing test conventions (naming, structure, assertions)
  - Ensure all tests compile and run successfully

  After generation, run all tests and report results."
)
```

The qa-test-engineer will:

1. Analyze the codebase for the specified language
2. Generate tests according to the strategies above
3. Run tests to verify they pass
4. Report results back to qa-orchestrator
