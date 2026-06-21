# Crabbox Runner for RustRover

Crabbox Runner turns RustRover into a remote proof button for Rust. The plugin
keeps the project, inspections, and normal edit loop local, then delegates
expensive or evidence-sensitive commands to the local `crabbox` CLI:

```bash
crabbox run -- cargo test
crabbox run -- cargo test --workspace
crabbox run -- cargo clippy --all-targets
```

That is the product promise: click once in RustRover to prove the code you are
editing in a real Crabbox or Islo sandbox, with streamed output and durable
evidence coming back to the IDE.

## Website

https://zozo123.github.io/rust-rover-plugin-crabbox/

## Why This Shape

OpenClaw Crabbox is a remote testbox and sandbox control plane, not a generic
shell script. A RustRover plugin should therefore wrap the official local CLI
instead of reimplementing lease, broker, SSH, rsync, delegated provider, auth, or
artifact behavior.

The resulting architecture is intentionally thin:

```text
RustRover action / run configuration
  -> Crabbox Runner plugin
  -> local crabbox CLI
  -> Crabbox broker + SSH/rsync or delegated provider
  -> remote runner
```

This fits Rust teams that have bigger CI-like test workloads than their laptops
can comfortably handle, need remote Linux or provider-specific environments, or
want reproducible evidence from local development and agent-assisted work.

## MVP Features

- `Tools > Crabbox > Doctor`
- `Tools > Crabbox > Doctor Islo`
- `Tools > Crabbox > Login...`
- `Tools > Crabbox > Init Repo`
- `Tools > Crabbox > Sync Plan`
- `Tools > Crabbox > Warmup Box`
- `Tools > Crabbox > Run Cargo Test`
- `Tools > Crabbox > Run Cargo Test Workspace`
- `Tools > Crabbox > Run Cargo Clippy`
- `Tools > Crabbox > Run Cargo Nextest`
- `Tools > Crabbox > Run Cargo Test on Islo`
- `Tools > Crabbox > Run Islo Rust Smoke`
- `Tools > Crabbox > Stop Lease...`
- Persistent `Crabbox` run configurations for commands like
  `cargo test --workspace`
- Console links for Crabbox URLs, `run_...` ids, and `cbx_...` lease ids

## Install From GitHub

1. Open the latest GitHub release.
2. Download `crabbox-rustrover-0.3.5.zip`.
3. In RustRover, open `Settings > Plugins`.
4. Choose the gear menu, then `Install Plugin from Disk...`.
5. Select the downloaded zip and restart RustRover.

After restart, open a Rust project and run `Tools > Crabbox > Doctor`.

## Settings

Open `Settings > Crabbox`:

- `Crabbox executable`: defaults to `crabbox`; set `/path/to/crabbox.sh` only if
  your team really uses a wrapper.
- `Broker URL`: used by the login action when present.
- `Default provider`: optional, for example `hetzner`, `aws`, or `islo`.
- `Default class`: optional, for example `standard`, `fast`, `large`, or `beast`.
- `Default Crabbox args`: extra flags appended before the Cargo command.
- `Islo Rust image`: defaults to
  `ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5`, so
  Cargo, clippy, rustfmt, nextest, OpenSSL, protobuf, and native build tools are
  available in the remote sandbox.
- `Islo API key`: stored in IntelliJ Password Safe and injected into Crabbox
  runs as `ISLO_API_KEY`.

The plugin does not store broker tokens. Use Crabbox's own login flow.

## Islo Smoke Test

After installing Crabbox and authenticating:

```bash
brew install openclaw/tap/crabbox
crabbox doctor
```

In RustRover:

1. Open `Settings > Crabbox`.
2. Paste your Islo API key into `Islo API key`.
3. Keep `Islo Rust image` as the default Crabbox Rust Runner image, or replace
   it with your own baked image.
4. Run `Tools > Crabbox > Doctor Islo`.
5. Run `Tools > Crabbox > Run Islo Rust Smoke` to confirm the provider path.
6. In a Rust project, run `Tools > Crabbox > Run Cargo Test on Islo`.

The expected CLI shape is:

```bash
ISLO_API_KEY=... crabbox doctor --provider islo
ISLO_API_KEY=... crabbox run --provider islo --islo-image ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5 -- cargo --version
crabbox run --provider islo --islo-image ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5 -- cargo test
```

If the Islo provider name or account setup differs in your Crabbox deployment,
keep the plugin unchanged and put the correct provider flags in `Default Crabbox
args` or a saved run configuration.

## How Crabbox Resolves Your Project

Crabbox syncs and runs from the **enclosing git repository root**, not the
folder you launched from. If your `Cargo.toml` is at the repo root (the usual
case), `cargo test` just works. If the crate lives in a subdirectory, a bare
`cargo test` runs at the repo root and fails with
`could not find Cargo.toml ... or any parent directory`.

The plugin handles this: the Cargo actions detect the manifest directory and
`cd` into it relative to the git root before running Cargo, so monorepo crates
and the bundled demo work without extra configuration.

## Demo Crate

`examples/hello-crabbox` is a tiny crate that exists to prove the path end to
end. Verified with the published runner image via Crabbox's local container
provider (the same sync + image + `cargo test` path Islo uses):

```bash
crabbox run \
  --provider local-container \
  --local-container-image ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5 \
  -- bash -lc 'cd examples/hello-crabbox && cargo test'
# Compiling hello-crabbox v0.1.0
# test result: ok. 2 passed; 0 failed
# run summary ... exit=0
```

To run it on Islo, swap the provider and image flags:

```bash
crabbox run \
  --provider islo \
  --islo-image ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5 \
  -- bash -lc 'cd examples/hello-crabbox && cargo test'
```

## Rust Runner Image

The recommended use case is a dependency-heavy Rust project where local output is
not enough proof: native crates, OpenSSL/protobuf bindings, generated code,
agent-written tests, or `cargo nextest` suites that should run in a clean remote
sandbox.

The runner image lives in `docker/rust-runner` and is published to GHCR:

```text
ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.5
```

It includes Rust stable, `clippy`, `rustfmt`, `cargo-nextest`, `clang`, `lld`,
`mold`, `cmake`, `pkg-config`, `libssl-dev`, `protobuf-compiler`, `git`, `ssh`,
and `rsync`.

## Build

```bash
./gradlew buildPlugin
./gradlew runIde
```

The installable zip is written to `build/distributions/`.

The plugin targets IntelliJ Platform build `252+` and avoids RustRover-specific
APIs in this MVP, so it should run in RustRover and other compatible IntelliJ
Platform IDEs that can execute external tools.

## Roadmap

The next valuable layer is Rust-aware context detection:

- run the test under the caret;
- infer `cargo test -p <package> <test_name>`;
- surface `crabbox events`, `results`, `artifacts`, `ssh`, and `webvnc` actions
  from parsed console metadata;
- warn when `.crabbox.yaml` is missing or `sync-plan` looks too large;
- offer cleanup notifications for kept leases.
