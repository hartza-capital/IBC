---
name: ibc-checkstyle-setup
description: Checkstyle runs on the plugin's default sun_checks.xml (no configLocation), skips test/, and its local cache silently hides already-clean files from the report.
metadata:
  type: project
---

`pom.xml` configures `maven-checkstyle-plugin` with **only**
`<suppressionsLocation>`. There is no `<configLocation>` and no ruleset file in
the repo (`checkstyle-suppressions.xml` is the only checkstyle file), so the
**effective ruleset is the plugin default `sun_checks.xml`**, run by the
Checkstyle **9.3** engine bundled with plugin 3.6.0.

**Why:** the CI comment in `.github/workflows/test-java.yml` claims the run uses
"the project's own pom-configured ruleset (not a standalone google/sun config)",
which is the opposite of what happens — Checkstyle itself prints
`... with sun_checks.xml ruleset`. Reading that comment instead of the tool
output sends you looking for a ruleset that does not exist.

**How to apply:**

- **Clear the cache before trusting a local report.** `checkstyle:checkstyle`
  caches clean files in `target/checkstyle-cachefile` and omits them from
  `checkstyle-result.xml` entirely. A stale cache showed 26 of 83 files scanned;
  `rm -f target/checkstyle-cachefile` first. Violation counts stay correct (only
  clean files are cached) — the **file list** is what gets truncated.
- **`test/` is never analysed.** `includeTestSourceDirectory` is unset (default
  `false`), so only the 83 files in `src/` are scanned. The
  `files="^test[/\\].*\.java$"` suppression is therefore a no-op — and would
  still not match if enabled, since Checkstyle feeds it absolute paths.
- **Nothing fails the build.** CI runs `checkstyle:checkstyle` (never
  `checkstyle:check`) and pipes it to reviewdog with `-level=warning
  -filter-mode=added`: new violations on touched lines get inline PR comments,
  existing ones and untouched code are never reported.
- **Baseline:** 144 violations on 2026-07-22, reduced to 122 the same day. ~90
  are sun_checks artefacts (LineLength=80, FinalParameters) that argue for
  choosing a modern ruleset, not for mass-editing — most of the rest is upstream
  style debt, see [[ibc-fork-ownership]].
