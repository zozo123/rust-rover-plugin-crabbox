plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.incredibuild"
version = "0.3.3"

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
            Crabbox runs the command from the git repository root. Cargo actions
            now cd into the crate's manifest directory when it lives in a
            subdirectory, so Run Cargo Test works for monorepo crates and the
            bundled examples/hello-crabbox demo. Also fixes a Settings dialog
            that reported unsaved changes for the Islo image field on open.
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
