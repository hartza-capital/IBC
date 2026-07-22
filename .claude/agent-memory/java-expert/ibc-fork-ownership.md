---
name: ibc-fork-ownership
description: This repo is a fork of IbcAlpha/IBC with a live upstream remote — all 83 src files look modified, but only ~12 carry real logic divergence; scope edits to those.
metadata:
  type: project
---

`hartza-capital/IBC` is a **fork** of `IbcAlpha/IBC` with a live `upstream`
remote that is still merged from (upstream 3.24.1 landed in commit `9b7f649`).
Refactors, dead-code removal, and style sweeps must be scoped by **file
ownership**, not by "is this symbol used".

`git diff --name-status <merge-base> HEAD -- 'src/**/*.java'` is **misleading**:
it marks all 83 src files `M`, because this fork ran repo-wide Javadoc and
formatting commits (`5d7ffc4`, `71e491d`) across every file. The logic is still
upstream's.

To get the real divergence, ignore whitespace and comment-only lines:

```bash
MB=$(git merge-base HEAD upstream/master)
for f in src/ibcalpha/ibc/*.java; do
  n=$(git diff -w $MB HEAD -- "$f" | grep -E '^[+-]' | grep -vE '^[+-]{3}' \
      | grep -vE '^[+-]\s*(\*|/\*|//)' | grep -vE '^[+-]\s*$' | wc -l)
  [ "$n" -gt 0 ] && echo "$n $f"
done | sort -rn
```

As of `b35dd5a` that yields only ~12 files — `RestartTask`, `CommandDispatcher`,
`Totp` (new), `Base32` (new), `SecondFactorAuthenticationDialogHandler`, `Utils`,
`DefaultSettings`, then 1–2 line edits in `TwsSettingsSaver`,
`GatewayLoginFrameHandler`, `ConfigurationAction`, `IbcTws`,
`AbstractLoginHandler` — plus everything under `test/` (all hartza-authored).

**Why:** Editing the ~71 upstream-verbatim files creates a merge conflict on
every future upstream pull, for no behavioral gain. Upstream's apparently-unused
public methods (e.g. `SwingUtils.findComboBox`, `isButtonEnabled`,
`isCheckBoxSelected`, `isRadioButtonSelected`, `setRadioButtonSelected`,
`Utils.getErrStream`) are its published API surface, exercised by upstream code
this fork does not contain.

**How to apply:**

- Remove/refactor only inside the hartza-owned surface above; **flag** dead code
  found in upstream-verbatim files and report it for a future upstream PR.
- Unused-import removal is the safe exception — trivial and low-conflict even in
  upstream files.
- The ~2600 checkstyle violations are almost entirely upstream style debt; do not
  "fix" them repo-wide.
- Static call-graph tools are near-useless here: IBC dispatches Swing window
  events polymorphically, so `WindowHandler.recogniseWindow`/`filterEvent`/
  `handleWindow`, `Runnable.run`, `Callable.call`, constructors, and Swing
  overrides all show zero callers while being very much alive. See
  [[ibc-testability]]; build setup in [[ibc-build-requirements]].
