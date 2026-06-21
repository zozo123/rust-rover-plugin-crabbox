package com.incredibuild.crabbox

import org.junit.Assert.assertEquals
import org.junit.Test

class CrabboxCommandLineTest {
    @Test
    fun `splitArgs splits on whitespace`() {
        assertEquals(listOf("cargo", "test", "--workspace"), CrabboxCommandLine.splitArgs("cargo test --workspace"))
    }

    @Test
    fun `splitArgs returns empty for blank`() {
        assertEquals(emptyList<String>(), CrabboxCommandLine.splitArgs("   "))
    }

    @Test
    fun `splitArgs honors single quotes`() {
        assertEquals(listOf("echo", "a b"), CrabboxCommandLine.splitArgs("echo 'a b'"))
    }

    @Test
    fun `splitArgs honors double quotes`() {
        assertEquals(listOf("echo", "a b"), CrabboxCommandLine.splitArgs("""echo "a b""""))
    }

    @Test
    fun `splitArgs honors backslash escape`() {
        assertEquals(listOf("a b"), CrabboxCommandLine.splitArgs("""a\ b"""))
    }

    @Test
    fun `splitArgs keeps an explicit empty quoted token`() {
        assertEquals(listOf("--flag", ""), CrabboxCommandLine.splitArgs("--flag ''"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `splitArgs rejects a trailing escape`() {
        CrabboxCommandLine.splitArgs("""cargo test\""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `splitArgs rejects an unclosed quote`() {
        CrabboxCommandLine.splitArgs("echo 'unterminated")
    }

    @Test
    fun `joinArgs round-trips through splitArgs`() {
        val args = listOf("cargo", "test", "a b", "--cfg=name with space")
        assertEquals(args, CrabboxCommandLine.splitArgs(CrabboxCommandLine.joinArgs(args)))
    }

    @Test
    fun `allowEnvArgs emits a flag per name`() {
        assertEquals(
            listOf("--allow-env", "RUST_LOG", "--allow-env", "DATABASE_URL"),
            CrabboxCommandLine.allowEnvArgs(emptyList(), listOf("RUST_LOG", "DATABASE_URL")),
        )
    }

    @Test
    fun `allowEnvArgs de-duplicates repeated names`() {
        assertEquals(
            listOf("--allow-env", "RUST_LOG"),
            CrabboxCommandLine.allowEnvArgs(emptyList(), listOf("RUST_LOG", "RUST_LOG")),
        )
    }

    @Test
    fun `allowEnvArgs skips names already declared in crabbox args`() {
        assertEquals(
            listOf("--allow-env", "BAR"),
            CrabboxCommandLine.allowEnvArgs(listOf("--allow-env", "FOO"), listOf("FOO", "BAR")),
        )
    }

    @Test
    fun `allowEnvArgs understands comma-separated and equals forms`() {
        assertEquals(
            listOf("--allow-env", "BAR"),
            CrabboxCommandLine.allowEnvArgs(listOf("--allow-env=FOO,BAZ"), listOf("FOO", "BAR", "BAZ")),
        )
    }

    @Test
    fun `allowEnvArgs ignores blank names`() {
        assertEquals(
            listOf("--allow-env", "FOO"),
            CrabboxCommandLine.allowEnvArgs(emptyList(), listOf("", "   ", "FOO")),
        )
    }

    @Test
    fun `forceHttp1ForIslo adds GODEBUG for islo runs`() {
        assertEquals(
            mapOf("GODEBUG" to "http2client=0"),
            CrabboxCommandLine.forceHttp1ForIslo(emptyMap(), listOf("--provider", "islo")),
        )
    }

    @Test
    fun `forceHttp1ForIslo detects the islo image flag`() {
        assertEquals(
            mapOf("A" to "b", "GODEBUG" to "http2client=0"),
            CrabboxCommandLine.forceHttp1ForIslo(mapOf("A" to "b"), listOf("--islo-image", "ghcr.io/x/y:1")),
        )
    }

    @Test
    fun `forceHttp1ForIslo leaves non-islo runs untouched`() {
        assertEquals(
            mapOf("A" to "b"),
            CrabboxCommandLine.forceHttp1ForIslo(mapOf("A" to "b"), listOf("--provider", "hetzner")),
        )
    }

    @Test
    fun `forceHttp1ForIslo never overrides a user-set GODEBUG`() {
        assertEquals(
            mapOf("GODEBUG" to "asyncpreemptoff=1"),
            CrabboxCommandLine.forceHttp1ForIslo(mapOf("GODEBUG" to "asyncpreemptoff=1"), listOf("--provider", "islo")),
        )
    }

    @Test
    fun `forceHttp1ForIslo does not fire on islo flags with a non-islo provider`() {
        // --islo-vcpus etc. can appear with another provider; must NOT force HTTP/1.1.
        assertEquals(
            emptyMap<String, String>(),
            CrabboxCommandLine.forceHttp1ForIslo(emptyMap(), listOf("--provider", "hetzner", "--islo-vcpus", "4", "--islo-base-url", "https://api.islo.dev")),
        )
    }

    @Test
    fun `forceHttp1ForIslo handles the provider=islo form and is case-insensitive`() {
        assertEquals(
            mapOf("GODEBUG" to "http2client=0"),
            CrabboxCommandLine.forceHttp1ForIslo(emptyMap(), listOf("--provider=islo")),
        )
        assertEquals(
            mapOf("GODEBUG" to "http2client=0"),
            CrabboxCommandLine.forceHttp1ForIslo(emptyMap(), listOf("--provider", "ISLO")),
        )
    }

    @Test
    fun `forceHttp1ForIslo fires for args built from the islo settings path`() {
        // Mirrors CrabboxSettingsState.isloRunArgs(): --provider islo + --islo-image.
        val args = listOf("--provider", "islo", "--islo-image", "ghcr.io/x/y:1")
        assertEquals(
            mapOf("GODEBUG" to "http2client=0"),
            CrabboxCommandLine.forceHttp1ForIslo(emptyMap(), args),
        )
    }
}
