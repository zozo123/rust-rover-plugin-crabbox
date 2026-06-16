#!/usr/bin/env bash
#
# e2e-local.sh — Prove the crabbox --allow-env env-forwarding fix against a FREE
# local provider, with a negative control.
#
# What it proves:
#   crabbox forwards only a NARROW env (NODE_OPTIONS, CI) to the remote command.
#   A normal var like E2E_PROOF must NOT leak through (negative control),
#   and MUST leak through once --allow-env E2E_PROOF is passed (positive).
#   This is exactly the behavior the RustRover plugin emits since v0.3.4.
#
# Cost/safety: uses only a FREE local provider (apple-container or
# local-container) — coordinator:never, so it never touches Islo, the network,
# or a broker. The sync source is a tiny throwaway git repo in a mktemp dir, not
# the plugin checkout.
#
set -eu

PROBE_VAL="forwarded_ok"

say()  { printf '%s\n' "==> $*"; }
warn() { printf '%s\n' "!!  $*" >&2; }

# crabbox auto-releases its ephemeral lease, but the Apple container runtime can
# fail the post-run delete; leftover crabbox-* boxes degrade the runtime (runs
# then emit literal/garbled output). Sweep them both BEFORE and AFTER the test
# so we start and end from a clean runtime. Best-effort and quiet.
sweep_containers() {
  if command -v container >/dev/null 2>&1; then
    for c in $(container ls -a 2>/dev/null | awk 'NR>1 && /crabbox-/ {print $1}'); do
      container stop "$c" >/dev/null 2>&1 || true
      container delete "$c" >/dev/null 2>&1 || true
    done
  fi
}

TMPDIR_E2E=""
cleanup() {
  if [ -n "${TMPDIR_E2E}" ] && [ -d "${TMPDIR_E2E}" ]; then
    rm -rf "${TMPDIR_E2E}"
  fi
  sweep_containers
}
trap cleanup EXIT INT TERM

# --- preflight: crabbox present --------------------------------------------
if ! command -v crabbox >/dev/null 2>&1; then
  say "SKIP: crabbox CLI not found on PATH. Install with: brew install openclaw/tap/crabbox"
  exit 0
fi
say "crabbox: $(crabbox --version 2>/dev/null || echo unknown)"

# --- provider auto-detection (FREE, coordinator:never) ----------------------
# apple-container: macOS Apple silicon; arch is inferred from the host, so do
#   NOT pass --arch (the CLI only accepts --arch for azure/aws).
# local-container: any host with a running Docker daemon; uses the default arch.
PROVIDER=""
if command -v container >/dev/null 2>&1; then
  say "Detected Apple 'container' CLI; ensuring the system service is running"
  if container system start >/dev/null 2>&1; then
    PROVIDER="apple-container"
  else
    warn "container system start failed; falling back to docker if available"
  fi
fi
if [ -z "${PROVIDER}" ] && command -v docker >/dev/null 2>&1; then
  say "Checking Docker daemon"
  if docker info >/dev/null 2>&1; then
    PROVIDER="local-container"
  else
    warn "docker CLI present but daemon not reachable"
  fi
fi
if [ -z "${PROVIDER}" ]; then
  say "SKIP: no FREE local provider available."
  say "      Need Apple 'container' (macOS Apple silicon) or a running Docker daemon."
  exit 0
fi
say "Using provider=${PROVIDER}"

# Start from a clean runtime so a leftover box from a prior run can't degrade it.
if [ "${PROVIDER}" = "apple-container" ]; then
  sweep_containers
fi

# --- tiny throwaway sync source --------------------------------------------
TMPDIR_E2E="$(mktemp -d 2>/dev/null || mktemp -d -t crabbox-e2e)"
say "Throwaway sync repo: ${TMPDIR_E2E}"
(
  cd "${TMPDIR_E2E}"
  git init -q
  git config user.email "e2e@crabbox.local"
  git config user.name  "crabbox e2e"
  printf 'crabbox env-forwarding e2e\n' > README.md
  git add README.md
  git commit -q -m "e2e fixture"
)

# Run crabbox in the throwaway repo. $1 = tag (NEG/POS); rest = extra flags.
# No --arch: apple-container infers it from the host and rejects an explicit
# --arch; local-container uses its default.
run_case() {
  tag="$1"; shift
  # The remote payload keeps inner double quotes around the value so the remote
  # shell expands ${E2E_PROOF}; the local \$ keeps the var unexpanded until it
  # reaches the box. NOTE: the probe var must NOT be in crabbox's reserved
  # CRABBOX_* namespace, or crabbox escapes the reference and it arrives literal.
  E2E_PROOF="${PROBE_VAL}" crabbox run \
    --provider "${PROVIDER}" \
    "$@" \
    -- bash -lc "echo \"${tag}=[\${E2E_PROOF}]\"" \
    2>&1
}

# Extract the value between [ ] from the real output line (anchored to line
# start so we do not match crabbox's "running on ... echo" command-echo, which
# carries the un-expanded ${VAR} text).
extract() {
  printf '%s\n' "$2" \
    | grep -oE "^$1=\[[^]]*\]" \
    | head -n1 \
    | sed -E "s/^$1=\[(.*)\]$/\1/"
}

# crabbox's overall exit code can be non-zero purely because the local runtime
# failed to delete the box AFTER a successful command, so we judge by the
# remote command's own output (the TAG=[...] marker) rather than the exit code.
# A missing marker means the command itself never ran -> real failure.
require_marker() {
  tag="$1"; out="$2"
  # Anchor to line start: crabbox echoes the command it runs in a
  # "running on ... bash -lc 'echo \"TAG=[...]\"'" log line that ALSO contains
  # TAG=[...] (with the variable un-expanded). The real command output is the
  # only line that BEGINS with TAG=[. Match that, not the echo.
  if ! printf '%s\n' "${out}" | grep -qE "^${tag}=\["; then
    warn "${tag}: crabbox produced no '^${tag}=[...]' output line; the run itself failed:"
    printf '%s\n' "${out}" >&2
    exit 1
  fi
}

# --- negative control ------------------------------------------------------
say "Negative control (no --allow-env): E2E_PROOF must NOT be forwarded"
NEG_OUT="$(cd "${TMPDIR_E2E}" && run_case NEG || true)"
require_marker NEG "${NEG_OUT}"
NEG_VAL="$(extract NEG "${NEG_OUT}")"
say "negative observed: NEG=[${NEG_VAL}]"

# --- positive case ---------------------------------------------------------
say "Positive case (--allow-env E2E_PROOF): value MUST be forwarded"
POS_OUT="$(cd "${TMPDIR_E2E}" && run_case POS --allow-env E2E_PROOF || true)"
require_marker POS "${POS_OUT}"
POS_VAL="$(extract POS "${POS_OUT}")"
say "positive observed: POS=[${POS_VAL}]"

# --- verdict ---------------------------------------------------------------
FAIL=0
if [ -z "${NEG_VAL}" ]; then
  say "PASS (negative control): E2E_PROOF correctly NOT forwarded"
else
  warn "FAIL (negative control): E2E_PROOF unexpectedly forwarded as [${NEG_VAL}]"; FAIL=1
fi
if [ "${POS_VAL}" = "${PROBE_VAL}" ]; then
  say "PASS (positive case): --allow-env forwarded E2E_PROOF=[${POS_VAL}]"
else
  warn "FAIL (positive case): expected [${PROBE_VAL}], got [${POS_VAL}]"; FAIL=1
fi

echo
if [ "${FAIL}" -eq 0 ]; then
  say "RESULT: PASS — --allow-env forwarding verified on provider=${PROVIDER}"
  exit 0
else
  warn "RESULT: FAIL — env-forwarding behavior did not match expectations"
  exit 1
fi
