# Docker release checks

Two containerised checks on `IBCLinux-<version>.zip`, so the artifact is tested
as a consumer receives it rather than as `target/classes`:

| Stage           | What it proves                                                                                                                                                         |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `verify`        | The ZIP is intact, carries its launchers as executable, stamps the version matching its file name, and packages a real compile instead of an IDE-generated stub class. |
| `gateway-smoke` | The freshly built IBC starts the Gateway installed in the runtime image, resolves java, and submits a login.                                                           |

The Gateway jars IBC compiles against are copied out of
`quay.io/hartza-capital/ib-gateway:stable`, so no IB installer download and no
license click-through is involved. That image is `linux/amd64` only: the build
stage stays on the host platform, and only `gateway-smoke` runs under emulation.

## Usage

```sh
docker compose run --rm verify         # ~1 min after the first build
docker compose run --rm --build smoke  # end-to-end, needs the Gateway image
docker compose run --rm dist           # write the ZIP to ./dist
```

`make docker-verify` and `make docker-smoke` wrap the first two. The smoke test
uses deliberately invalid credentials: it proves the wiring, not an IB login,
and exits non-zero when IBC never reaches `Login attempt`.

## What the smoke test expects

From `/tmp/smoke/ibc-run.log` inside the container:

- `version: <version from the ZIP>`
- `Location of java executable=<path>`
- `Login attempt: 1`
- no `Unresolved compilation problem`, no `An exception has occurred`

`SMOKE_TIMEOUT` (default 240 s) bounds the login wait.
