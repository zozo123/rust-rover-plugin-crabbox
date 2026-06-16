package com.incredibuild.crabbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrabboxWorkspaceTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `relativeManifestDir returns null for blank input`() {
        assertNull(CrabboxWorkspace.relativeManifestDir(null))
        assertNull(CrabboxWorkspace.relativeManifestDir("  "))
    }

    @Test
    fun `relativeManifestDir returns null when manifest is at the git root`() {
        val root = tmp.newFolder()
        gitRoot(root)
        File(root, "Cargo.toml").writeText("[package]\n")
        assertNull(CrabboxWorkspace.relativeManifestDir(root.toString()))
    }

    @Test
    fun `relativeManifestDir resolves a crate in a subdirectory`() {
        val root = tmp.newFolder()
        gitRoot(root)
        val crate = File(root, "crate").apply { mkdirs() }
        File(crate, "Cargo.toml").writeText("[package]\n")
        assertEquals("crate", CrabboxWorkspace.relativeManifestDir(crate.toString()))
    }

    @Test
    fun `relativeManifestDir uses forward slashes for nested crates`() {
        val root = tmp.newFolder()
        gitRoot(root)
        val crate = File(root, "a/b").apply { mkdirs() }
        File(crate, "Cargo.toml").writeText("[package]\n")
        assertEquals("a/b", CrabboxWorkspace.relativeManifestDir(crate.toString()))
    }

    @Test
    fun `relativeManifestDir returns null without a git root`() {
        val dir = tmp.newFolder()
        File(dir, "Cargo.toml").writeText("[package]\n")
        assertNull(CrabboxWorkspace.relativeManifestDir(dir.toString()))
    }

    @Test
    fun `wrapRustCommand leaves a root-level command untouched`() {
        val root = tmp.newFolder()
        gitRoot(root)
        File(root, "Cargo.toml").writeText("[package]\n")
        assertEquals("cargo test", CrabboxWorkspace.wrapRustCommand(root.toString(), "cargo test"))
    }

    @Test
    fun `wrapRustCommand cds into a subdirectory crate`() {
        val root = tmp.newFolder()
        gitRoot(root)
        val crate = File(root, "crate").apply { mkdirs() }
        File(crate, "Cargo.toml").writeText("[package]\n")
        val wrapped = CrabboxWorkspace.wrapRustCommand(crate.toString(), "cargo test")
        assertTrue("expected a bash wrapper, got: $wrapped", wrapped.startsWith("bash -lc"))
        assertTrue("expected a cd into the crate, got: $wrapped", wrapped.contains("cd 'crate' && cargo test"))
    }

    private fun gitRoot(root: File) {
        File(root, ".git").mkdirs()
    }
}
