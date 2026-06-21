plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.incredibuild"
version = "0.3.5"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.2.6.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = "Crabbox Runner"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }

        description = """
            Turn RustRover into a remote proof button for Rust builds and tests.
            The plugin delegates to the local crabbox CLI, so Crabbox keeps
            ownership of auth, lease lifecycle, sync, delegated providers such
            as Islo, evidence, and cleanup.
        """.trimIndent()

        changeNotes = """
            Islo runs now work from the IDE: the plugin sets GODEBUG=http2client=0
            for Islo commands so crabbox's HTTP/2 client no longer hangs on the
            sandbox-create call ("http2: timeout awaiting response headers"). It is
            scoped to Islo runs and never overrides a GODEBUG you set yourself.
            Verified end to end against a real Islo sandbox (lease + cargo test +
            env forwarding). The default Islo Rust image tracks the version (0.3.5).
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}
