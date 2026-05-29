# Mandated Comment Formats

For each supported language there is **exactly one** comment format — the
language's official idiomatic standard. It is mandatory. Do not adapt to the
project's existing style, do not invent variants, do not mix formats. If the
project currently uses something else, this skill converges it onto the
standard below.

Comments must sit **immediately above** the function declaration with no
blank line between the comment block and the `func`/`def`/`fn`/etc.

---

## Go — godoc (mandatory)

- Use `//` line comments only. Never `/* */`.
- The first sentence **must start with the function name** and read as a
  complete sentence (godoc convention, enforced by `go doc`).
- Parameters are documented in a bulleted list using `//   - name: description`.
- Return values are documented in prose after the parameter list.
- Apply to exported AND unexported functions.

```go
// FetchUser retrieves a user by ID from the primary database.
//
// It returns ErrNotFound when no row matches the given id, and wraps any
// underlying driver error.
//
//   - ctx: cancellation and deadline propagation
//   - id:  primary key of the user to load
//
// Returns the hydrated User or an error.
func FetchUser(ctx context.Context, id int64) (*User, error) { ... }
```

---

## Python — Google-style docstrings (mandatory)

- Triple double-quoted docstrings `"""..."""`, never single quotes.
- **Google style only** — no NumPy, no reST, no Sphinx-style `:param:`.
- Sections: `Args:`, `Returns:`, `Raises:` — in that order, omit a section
  only when it has no content.
- One line per parameter under `Args:`.
- Apply to public AND private (`_name`) functions.

```python
def fetch_user(session: Session, user_id: int) -> User:
    """Retrieve a user by primary key.

    Raises NotFoundError when no row matches.

    Args:
        session: Active SQLAlchemy session used for the query.
        user_id: Primary key of the user to load.

    Returns:
        The hydrated User instance.

    Raises:
        NotFoundError: If no user has this id.
    """
```

`self` and `cls` are omitted from `Args:` unless their role is unusual
(e.g. mixin behavior).

---

## Rust — rustdoc (mandatory)

- Use `///` outer doc comments above items. `//!` is reserved for module-
  level docs at the top of a file and is not used by this skill.
- Sections use Markdown headers: `# Arguments`, `# Returns`, `# Errors`,
  `# Panics`, `# Examples`. Include the sections that apply, omit the rest.
- Reference parameter names with backticks.
- Apply to `pub` AND non-`pub` items.

```rust
/// Retrieve a user by primary key.
///
/// # Arguments
/// * `pool` - Connection pool used to acquire a database handle.
/// * `id`   - Primary key of the user to load.
///
/// # Errors
/// Returns [`Error::NotFound`] if no row matches `id`, or a driver error
/// when the query itself fails.
pub async fn fetch_user(pool: &PgPool, id: i64) -> Result<User, Error> { ... }
```

Generic type parameters worth documenting go under `# Type Parameters`.

---

## TypeScript / JavaScript — TSDoc (mandatory)

- `/** ... */` block comments only. Never `//`.
- Tags: `@param`, `@returns`, `@throws`. In TypeScript do **not** include
  the type in `@param` (TSDoc reads it from the signature).
- One line per `@param`, in declaration order.
- Apply to exported AND non-exported functions, components, and hooks.

```ts
/**
 * Retrieve a user by primary key.
 *
 * Throws NotFoundError when no row matches the given id.
 *
 * @param db - Database client used for the query.
 * @param id - Primary key of the user to load.
 * @returns The hydrated user.
 * @throws NotFoundError when the user does not exist.
 */
export async function fetchUser(db: Db, id: number): Promise<User> { ... }
```

For React components, document the component above the function and each prop
inline in the `Props` type/interface using a single-line `/** ... */` per
field.

---

## Java — Javadoc (mandatory)

- `/** ... */` Javadoc blocks only.
- Tags in this order: `@param`, `@return`, `@throws`. One line per `@param`,
  in declaration order.
- The first sentence is the summary used in the Javadoc index — it must read
  as a complete standalone sentence.
- Apply to public, protected, package-private, AND private methods.

```java
/**
 * Retrieves a user by primary key.
 *
 * <p>Throws {@link NotFoundException} when no row matches.</p>
 *
 * @param session active Hibernate session used for the query
 * @param id      primary key of the user to load
 * @return the hydrated user
 * @throws NotFoundException if no user has this id
 */
public User fetchUser(Session session, long id) { ... }
```

---

## Universal content rules (apply to every language)

- **Functional, not implementational.** Describe what the function does and
  why it exists. The code already shows how.
- **One line per parameter.** Even obvious ones — uniformity is the point.
- **Mention non-obvious behavior**: side effects, mutation, I/O, blocking,
  concurrency, error/exception conditions.
- **Do not restate the signature.** "Takes a string and returns a string"
  adds nothing.
- **Keep summaries short.** 1–3 lines for the main description; expand only
  when the function genuinely warrants it.
- **No project-tone matching.** The standard above is the standard.
