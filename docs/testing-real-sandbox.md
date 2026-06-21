# Testing against a real sandbox

The plugin shells out to the [`crabbox`](https://github.com/openclaw/crabbox) CLI,
which leases a box, syncs the dirty checkout, runs your command, streams output,
and releases the box on exit. You can exercise this end to end two ways: a
**free local sandbox** and a **billable cloud (Islo) sandbox**.

> Install the CLI first: `brew install openclaw/tap/crabbox` (crabbox 0.26.0+).

## 1. Free local sandbox — `scripts/e2e-local.sh`

`scripts/e2e-local.sh` proves the v0.3.4 env-forwarding fix against a local box,
with a negative control. The local providers are `coordinator: never`, so this
path never touches a broker, the network, or Islo — **it is free**.

```bash
scripts/e2e-local.sh
```

It auto-detects one container runtime:

| Provider | Platform | Prerequisite |
| --- | --- | --- |
| `apple-container` | macOS (Apple silicon) | `container system start` (the script runs it) |
| `local-container` | any | a running Docker daemon |

Expected output:

```
==> PASS (negative control): E2E_PROOF correctly NOT forwarded
==> PASS (positive case): --allow-env forwarded E2E_PROOF=[forwarded_ok]
==> RESULT: PASS — --allow-env forwarding verified on provider=apple-container
```

To run the Rust example crate (not just the env probe) in a local box, point it
at the runner image that ships `cargo`/`clippy`/`nextest`:

```bash
crabbox run --provider apple-container \
  --apple-container-image ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5 \
  -- bash -lc 'cd examples/hello-crabbox && cargo test'
```

(Use `--local-container-image` and a running Docker daemon for the
`local-container` provider instead.)

## 2. Real Islo sandbox (costs money)

Islo is a delegated cloud provider. Running against it leases a **billable**
box and hits the network — there is no free tier on this path.

```bash
ISLO_API_KEY=… crabbox run --provider islo \
  --islo-image ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5 \
  --allow-env E2E_PROOF \
  -- bash -lc 'cd examples/hello-crabbox && cargo test'
```

`ISLO_API_KEY` is read by the crabbox CLI's own process for auth (it is **not**
forwarded into the sandbox). Relevant flags: `--islo-image`, `--islo-vcpus`,
`--islo-memory-mb`, `--islo-disk-gb`, `--islo-base-url` (default
`https://api.islo.dev`).

To run this in CI, add a **manual-dispatch-only** workflow (never on push/PR, so
it cannot bill unexpectedly) and store the key as a secret:

```bash
gh secret set ISLO_API_KEY
```

## 3. What "proof" means here

crabbox forwards only a **narrow** env allowlist (`NODE_OPTIONS`, `CI`) to the
remote command. Any other variable must be passed explicitly with
`--allow-env NAME` (repeatable, or comma-separated). Since v0.3.4 the plugin
emits an `--allow-env` flag for each run-configuration environment variable, so
they reach the remote command instead of being silently dropped.

The tests prove this by setting a sentinel var and asserting the remote command
sees it: **without** `--allow-env` the remote echo is empty
(`NEG=[]`); **with** it, the value comes through (`POS=[forwarded_ok]`). Do not
name the sentinel with a `CRABBOX_` prefix — that namespace is reserved by the
CLI.
