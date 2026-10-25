# Maven dependency security audit — IBC 3.25.1

Audit date: 2026-10-20. Scope: commit `19fb567` (`fix/auto-restart-dialog-and-jvm-options`),
the only Maven build in the repo (`pom.xml`, single module, packaging `jar`).
Tooling: Maven 3.10.0 (JDK 27, `release=25` per the pom), OSV.dev advisory database
(`api.osv.dev/v1/querybatch`), artifact inventory from `dependency:list` and
`dependency:resolve-plugins`.

## Result

| Surface                        | Artifacts resolved | Advisories                                    | Shipped to users |
| ------------------------------ | ------------------ | --------------------------------------------- | ---------------- |
| Runtime / compile scope        | 0                  | 0                                             | no code at all   |
| Test scope                     | 14                 | 0                                             | no               |
| Build plugin classpaths        | 236 (15 plugins)   | 13 advisories / 16 (artifact, advisory) pairs | no               |
| After the pins in §Remediation | 229                | 4 pairs                                       | no               |

`IBC.jar` (159 KB) contains only `ibcalpha/ibc/*.class` and `META-INF/`: the shipped
artifact carries **no third-party library**. The whole vulnerability surface of this
pom is build-time — CI runners and release builds, not the product.

Counting convention: the artifact counts above are rows of the `dependency:list` /
`dependency:resolve-plugins` output (250 = 236 plugin-classpath rows + 14 test
artifacts; 229 after the pins = 215 + 14). Read off the plugin class realms instead
(`mvn -X <plugin>:help`, the `Included:` lines — what a plugin actually loads), the
unsatisfied pom gives 300 rows over 15 realms for 220 distinct
`group:artifact:version`, and the §1 pins give 213 distinct — the same finding set,
counted without the resolve-plugins parse.

The 13 advisories are **not** stale pins of ours: every plugin and every project
dependency is at its latest published release (`versions:display-plugin-updates`
reports no update outside the Maven 4 betas; `display-dependency-updates` reports
none). The vulnerable libraries are pinned by the upstream plugin POMs.

## Inventory

- Project dependencies (all `test`): `junit-jupiter` 6.1.3 (with `junit-platform-*`
  6.1.3, `opentest4j` 1.3.0, `apiguardian-api` 1.1.2, `jspecify` 1.0.0),
  `junit-platform-launcher` 6.1.3, `mockito-core` 5.24.0 (`byte-buddy` 1.17.7,
  `byte-buddy-agent` 1.17.7, `objenesis` 3.3).
- Plugins: clean 3.5.0, enforcer 3.6.3, antrun 3.2.0, build-helper 3.6.2,
  compiler 3.16.0, jar 3.5.1, assembly 3.8.0, surefire 3.6.0, jacoco 0.8.15,
  checkstyle 3.6.0, spotbugs 4.10.5.0, resources 3.5.0, site 3.22.0 (resources and
  site are injected by the super POM, not declared in the pom).
- No `<repositories>` block: every artifact comes from Maven Central. No third-party
  repository, no snapshot repository.

## Findings

Severity is the GHSA severity carried by the advisory. "Pulled by" names the plugin
whose transitive tree contains the artifact.

| Advisory / CVE                       | Artifact                                 | Pulled by                                              | Severity | Fixed in         |
| ------------------------------------ | ---------------------------------------- | ------------------------------------------------------ | -------- | ---------------- |
| GHSA-6fmv-xxpf-w3cw / CVE-2025-67030 | plexus-utils 3.0.24, 3.6.0, 4.0.1, 4.0.2 | jacoco, resources, checkstyle, antrun, assembly, clean | HIGH     | 3.6.1 / 4.0.3    |
| GHSA-wxr5-93ph-8wr9 / CVE-2025-48734 | commons-beanutils 1.9.4                  | checkstyle, site                                       | HIGH     | 1.11.0           |
| GHSA-vx9q-rhv9-3jvg / CVE-2025-67721 | aircompressor 0.27                       | assembly                                               | HIGH     | 2.0.3            |
| GHSA-2fvj-hgj9-j2gr / CVE-2026-10050 | jetty-security 9.4.58                    | site                                                   | HIGH     | 12.0.36 / 9.4.63 |
| GHSA-355h-qmc2-wpwf / CVE-2026-2332  | jetty-http 9.4.58                        | site                                                   | HIGH     | 12.0.33 / 9.4.60 |
| GHSA-65r4-943x-97jj / CVE-2026-75140 | jsoup 1.22.1                             | site                                                   | HIGH     | 1.23.2           |
| GHSA-j288-q9x7-2f5v / CVE-2025-48924 | commons-lang3 3.17.0                     | checkstyle                                             | MODERATE | 3.18.0           |
| GHSA-7g45-4rm6-3mm3 / CVE-2023-2976  | guava 31.0.1-jre                         | checkstyle                                             | MODERATE | 32.0.0           |
| GHSA-8wh2-6qhj-h7j9 / CVE-2024-36124 | snappy 0.4                               | spotbugs, checkstyle                                   | MODERATE | 0.5              |
| GHSA-7p3p-8qv8-m2vh / CVE-2026-6790  | jetty-server 9.4.58                      | site                                                   | MODERATE | 12.0.35 / 12.1.9 |
| GHSA-qh8g-58pp-2wxh / CVE-2024-6763  | jetty-http 9.4.58                        | site                                                   | MODERATE | 12.0.12          |
| GHSA-pmhh-3w7g-xqp8 / CVE-2026-71497 | jsoup 1.22.1                             | site                                                   | MODERATE | 1.23.1           |
| GHSA-5mg8-w23w-74h3 / CVE-2020-8908  | guava 31.0.1-jre                         | checkstyle                                             | LOW      | 32.0.0           |

## Reachability

A finding only matters where the code actually runs. What executes:

| Plugin       | Executed by                                                                                                |
| ------------ | ---------------------------------------------------------------------------------------------------------- |
| checkstyle   | CI `mvn -B checkstyle:check` (both jobs) and `checkstyle:checkstyle`; `make checkstyle`                    |
| resources    | every Maven run (super-POM lifecycle)                                                                      |
| antrun       | compile — every build (`make dev`, `make dist`, `mvn test`)                                                |
| jacoco       | `initialize` + `test`                                                                                      |
| surefire     | `test`                                                                                                     |
| clean        | `make clean`                                                                                               |
| assembly     | `package` — release builds only                                                                            |
| spotbugs     | bound to `verify` — no make target reaches it; manual `mvn spotbugs:check`, advisory (`failOnError=false`) |
| site         | **nothing** — no target invokes `mvn site`                                                                 |
| enforcer     | `validate` — every build (requires `IBC_BIN`)                                                              |
| build-helper | `generate-sources` — every build                                                                           |
| jar          | `package` — release builds only                                                                            |
| compiler     | disabled (`default-compile` bound to `phase: none`); antrun compiles                                       |

So: plexus-utils, commons-beanutils, commons-lang3, guava and snappy run on every
CI run and every release; aircompressor runs on release packaging; the Jetty and
jsoup trees never run anywhere in this repo.

## Remediation

### 1. Pin the plugin transitives (clears 12 of 16 pairs — verified)

Maven lets a plugin's `<dependencies>` override its own transitive versions. The
pins below are the minimal releases that clear the advisory, tested against the
current plugins:

| Plugin                         | Override                                                                                         |
| ------------------------------ | ------------------------------------------------------------------------------------------------ |
| maven-checkstyle-plugin 3.6.0  | guava 33.7.2-jre, commons-beanutils 1.11.0, commons-lang3 3.21.0, snappy 0.5, plexus-utils 4.0.3 |
| jacoco-maven-plugin 0.8.15     | plexus-utils 4.0.3                                                                               |
| maven-antrun-plugin 3.2.0      | plexus-utils 4.0.3                                                                               |
| maven-assembly-plugin 3.8.0    | plexus-utils 4.0.3, aircompressor 2.0.3                                                          |
| spotbugs-maven-plugin 4.10.5.0 | snappy 0.5, plexus-utils 4.0.3                                                                   |
| maven-clean-plugin 3.5.0       | plexus-utils 4.0.3                                                                               |
| maven-resources-plugin 3.5.0   | plexus-utils 4.0.3                                                                               |
| maven-site-plugin 3.22.0       | commons-beanutils 1.11.0, jsoup 1.23.2, plexus-utils 4.0.3                                       |

One `<pluginManagement>` block covers all eight: Maven merges those `<dependencies>`
into every plugin declaration of the effective model, and the two plugins injected by
the super POM (`resources`, `site`) are no exception — measured, their realms load
the pinned plexus-utils without the plugin being declared in `<build><plugins>`.

Measured effect on `dependency:resolve-plugins` + OSV: **16 → 4 pairs**, 250 → 229
artifacts. Only the Jetty set remains.

### 2. Residual — Jetty 9.4 inside maven-site-plugin (accept, documented)

`maven-site-plugin` 3.22.0 (the latest) embeds Jetty 9.4.58. Four advisories need
Jetty 9.4.60/9.4.63 or 12.x; **neither 9.4.60 nor 9.4.63 exists on Maven Central**
(the 9.4 line ends at 9.4.58.v20250814) and 12.x is not API-compatible with the
plugin. The tree is unreachable in this repo (`mvn site` is invoked by no target),
so the exposure is: a developer running `mvn site` locally against untrusted
content. Accept it in writing, or drop the site plugin from scope. Do not add the
jetty pins — they only hide the finding behind a version that does not exist.

### 3. Gate in CI (currently absent)

Nothing in `.github/workflows/` or the pom scans dependencies: no OWASP
dependency-check, no trivy/grype, no OSV. Proposed gate, cheap and key-free:

- a `make deps-audit` target running the OSV batch query over
  `dependency:list` + `dependency:resolve-plugins` (~2 s, no NVD API key, no
  first-run DB download), failing on HIGH above an allowlist that names the four
  Jetty entries with this rationale. The script it runs is not in the repo yet
  (`ibc_maven_osv.py` lives outside the checkout): it has to land under
  `scripts/` before the gate can be reviewed or run in CI;
- wired as a CI step next to `checkstyle:check`, so a new plugin or a plugin
  upgrade that drags a vulnerable transitive fails the PR instead of being
  discovered at the next audit.

OWASP dependency-check was rejected for the gate: it needs an NVD API key and a
multi-hundred-MB first-run sync in CI.

Prerequisite before that gate can mean anything: the build currently stops at
`validate` without `IBC_BIN`. Verified in this checkout —

```
$ unset IBC_BIN && mvn -B validate
[ERROR] Failed to execute goal ...maven-enforcer-plugin:3.6.3:enforce (require-ibc-bin):
        Environment variable 'IBC_BIN' not set ...
```

`.github/workflows/test-java.yml` defines no `IBC_BIN`, so unless it is set as a
repository/org Actions variable, the `test` job never reaches `checkstyle:check`
or `mvn test`. The dependency gate belongs after that is settled.

### 4. Repo hygiene the scan cannot see

Three things outside the artifact set make a dependency gate lie or block it, none of
them found by an OSV query:

- `dependency-reduced-pom.xml` at the repo root is a shade-plugin by-product frozen at
  `3.23.0`, still naming `maven-clean-plugin` 3.3.2 and `maven-enforcer-plugin` 3.4.1.
  No build reads it — the pom declares no shade plugin and nothing references the file
  — so it is a second Maven manifest at the root that states a dependency set this
  project does not have. Delete it rather than leave a reader or a future scanner to
  choose between the two.
- `make deps-update`, `plugins-update`, `update` and `deps-upgrade` invoke
  `./mvnw versions:...` with no version, so each run fetches whatever
  `versions-maven-plugin` is current at that moment — and `deps-upgrade` rewrites
  `pom.xml` from it. Pin it in `<pluginManagement>` like the rest, or the one target
  that mutates the pom is the only unpinned tool in the build.
- The uncommitted edit to `test-java.yml` moves `JAVA_VERSION` from 25 to 21 and
  `./mvnw` to `mvn`. With `<release>25</release>` and the Ant `<javac release="25">`
  in the pom, that job cannot compile, and the wrapper's pinned Maven 3.10.0 is
  dropped. Any gate added next to `checkstyle:check` inherits the problem.

## Verification performed

On a detached worktree of `19fb567` with the §1 pins applied:

```
mvn -B dependency:resolve-plugins   → 15 plugins, 229 artifacts
OSV querybatch over that inventory  → 4 findings, all jetty-* via maven-site-plugin
mvn -B checkstyle:check             → 0 violations (guava 33.7.2, beanutils 1.11.0,
                                      lang3 3.21.0, snappy 0.5, plexus-utils 4.0.3 loaded)
mvn -B resources:resources          → BUILD SUCCESS (plexus-utils 4.0.3)
mvn -B clean                        → BUILD SUCCESS
mvn -B test                         → 112 tests, 0 failures, 0 errors (jacoco agent active)
mvn -B package -DskipTests          → dist/IBCLinux-3.25.1.zip built (assembly 3.8.0
                                      with aircompressor 2.0.3)
mvn -B spotbugs:check               → 52 pre-existing findings, advisory (snappy 0.5)
```

The Zip assembly path exercises plexus-archiver with the overridden aircompressor,
but not its snappy/lz4 codecs (the descriptor uses deflate) — that specific code
path is untested by this run.

Unpatched baseline for comparison: same inventory → 16 pairs over 13 artifacts.

Independent reproduction, second instrument (each plugin's class realm via
`mvn -X <plugin>:help`, the OSV Maven database mirrored locally, and every verdict
cross-checked with a unit `POST /v1/query`): same 16 → 4 pairs, the four remaining
ones all `jetty-*` inside `maven-site-plugin`. Three further measurements:

- the §1 pins leave checkstyle 9.3 in place with guava 33.7.2-jre, beanutils 1.11.0,
  commons-lang3 3.21.0 and snappy 0.5, and `checkstyle:check` still reports 0
  violations over the same 84 files — the library pins do not break the old parser;
- pinning the tool instead (`com.puppycrawl.tools:checkstyle:14.3.0` in the plugin's
  `<dependencies>`) also yields a valid run — realm becomes checkstyle 14.3.0, guava
  33.7.1-jre, beanutils 1.11.0, antlr4-runtime 4.13.2, 84 files, 0 violations. Either
  fixes the advisories; the tool pin additionally retires a 2022 parser;
- `aircompressor` 2.0.3 keeps the `SnappyFramedInputStream(InputStream)` and
  `(InputStream, boolean)` constructors that plexus-archiver 4.10.4 calls (`javap` on
  both jars), so the aircompressor pin is not a latent `NoSuchMethodError`; only the
  snappy code path itself remains unexercised, as noted above.

A `trivy fs --list-all-pkgs` on this repo reports one package — the project artifact —
and no transitive Maven dependency, because Trivy does not resolve a pom's tree. A
gate built on `trivy fs` would be green for ever here; the OSV inventory has to come
from Maven itself.

## Out of scope

- The TWS/Gateway jars on `IBC_BIN` (vendor-provided, not Maven artifacts) and the
  JRE inside the published image. Neither is resolved by this pom.
- Container base images (`docker/`), covered by image scanning, not by this audit.
- Static analysis of IBC's own code: `spotbugs:check` runs in the build, advisory
  only, and reports 52 pre-existing findings.
