# Crabbox Rust Runner Image

Purpose-built image for running Rust proof commands from RustRover through
Crabbox on Islo.

It is intentionally more than `rust:1-bookworm`:

- Rust stable with `cargo`, `rustc`, `clippy`, and `rustfmt`.
- `cargo-nextest` for fast test runs.
- Native build dependencies common in Rust crates: `clang`, `lld`, `mold`,
  `cmake`, `pkg-config`, `libssl-dev`, and `protobuf-compiler`.
- Crabbox sync/runtime tools: `git`, `ssh`, `rsync`, `tar`, `curl`, `jq`.
- Non-root `crabbox` user and `/workspace/crabbox` workdir.
- Rust tools are also symlinked into `/usr/local/bin` so delegated exec
  transports with a minimal `PATH` can still find `cargo`.

The plugin defaults Islo Rust actions to:

```text
ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.2
```

Manual smoke:

```bash
docker build -t crabbox-rust-runner:local docker/rust-runner
docker run --rm crabbox-rust-runner:local cargo --version
docker run --rm crabbox-rust-runner:local cargo nextest --version
docker run --rm crabbox-rust-runner:local env -i PATH=/usr/local/bin:/usr/bin:/bin cargo --version
```
