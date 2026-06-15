package com.incredibuild.crabbox

import java.io.File

/**
 * Crabbox syncs and runs from the enclosing **git repository root**, not from
 * the directory the command was invoked in. When the Cargo project lives in a
 * subdirectory of that root (a monorepo crate, or the bundled
 * `examples/hello-crabbox` demo), a bare `cargo test` runs at the repo root and
 * fails with `could not find Cargo.toml in <dir> or any parent directory`.
 *
 * This resolver finds the Cargo manifest directory relative to the git root so
 * the plugin can `cd` into it before running Cargo. For the common case where
 * `Cargo.toml` is at the repo root, no wrapping happens and the command is left
 * untouched.
 */
object CrabboxWorkspace {
    /**
     * Returns the manifest directory relative to the git repository root using
     * forward slashes, or `null` when no wrapping is needed (manifest at the
     * git root, no git root, or no manifest found).
     */
    fun relativeManifestDir(baseDir: String?): String? {
        if (baseDir.isNullOrBlank()) return null
        val base = File(baseDir).absoluteFile
        val gitRoot = findUp(base, stopAt = null) { File(it, ".git").exists() } ?: return null
        val manifestDir = findUp(base, stopAt = gitRoot) { File(it, "Cargo.toml").isFile } ?: return null
        if (manifestDir.absoluteFile == gitRoot.absoluteFile) return null

        val rel = gitRoot.toPath().relativize(manifestDir.toPath()).toString()
        return rel.ifBlank { null }?.replace(File.separatorChar, '/')
    }

    /**
     * Wraps a Cargo command so it runs from the manifest directory when that
     * directory is a subdirectory of the git root. Otherwise returns the command
     * unchanged. The wrapped form is `bash -lc "cd '<rel>' && <command>"`.
     */
    fun wrapRustCommand(baseDir: String?, rustCommand: String): String {
        val rel = relativeManifestDir(baseDir) ?: return rustCommand
        val inner = "cd ${shellQuote(rel)} && $rustCommand"
        return CrabboxCommandLine.joinArgs(listOf("bash", "-lc", inner))
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private inline fun findUp(start: File, stopAt: File?, predicate: (File) -> Boolean): File? {
        var dir: File? = start
        while (dir != null) {
            if (predicate(dir)) return dir
            if (stopAt != null && dir.absoluteFile == stopAt.absoluteFile) return null
            dir = dir.parentFile
        }
        return null
    }
}
