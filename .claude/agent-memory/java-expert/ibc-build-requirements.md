---
name: ibc-build-requirements
description: IBC_BIN must point at a real TWS/Gateway jars directory — the IBKR dependency is compile-time via fully-qualified calls, invisible to an import grep.
metadata:
  type: project
---

`IBC_BIN` must point at a **real** TWS/Gateway `jars/` directory (e.g. `~/Applications/IB Gateway 10.45/jars` on macOS). An empty directory satisfies the `maven-enforcer-plugin` check and then fails `javac` with `package jclient does not exist`.

**Why:** No file in `src/ibcalpha/ibc/` has an `import` of an IBKR package, so grepping imports wrongly suggests the IBKR dependency is runtime-only. It is compile-time: `IbcTws` reaches TWS through fully-qualified names — `ibgateway.GWClient.main(...)` and `jclient.LoginFrame.main(...)` (two call sites, both in `IbcTws`). Verified empirically: build with an empty `IBC_BIN` → enforcer passes, compile fails.

**How to apply:**

- Before building on an unfamiliar machine, locate a real install rather than stubbing `IBC_BIN`; on macOS the jars live under `~/Applications/IB Gateway <version>/jars`, not `~/Jts/<version>/jars`.
- Build under JDK 17 (`JAVA_HOME=/opt/homebrew/opt/openjdk@17` with Homebrew) — the pom targets release 17.
- When checking whether new code introduces an IBKR coupling, grep for `jclient.`/`ibgateway.` as well as `import`.
- Testability is a separate question — see [[ibc-testability]].
