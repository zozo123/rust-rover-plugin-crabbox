plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.incredibuild"
version = "0.3.1"

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
            Updates the plugin and website logo to match the Crabbox crab mark
            from crabbox.sh. Keeps the Crabbox Rust Runner image defaults and
            Islo run flow.
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
