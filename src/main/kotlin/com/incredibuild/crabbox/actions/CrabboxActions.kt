package com.incredibuild.crabbox.actions

import com.incredibuild.crabbox.CrabboxSettingsState
import com.incredibuild.crabbox.CrabboxSecrets
import com.incredibuild.crabbox.CrabboxTaskRunner
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.options.ShowSettingsUtil

abstract class CrabboxProjectAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }
}

class CrabboxConfigureAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Crabbox")
    }
}

class CrabboxDoctorAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        CrabboxTaskRunner.runSimple(project, "Crabbox doctor", listOf("doctor"))
    }
}

class CrabboxDoctorIsloAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!ensureIsloApiKey(project)) return
        CrabboxTaskRunner.runSimple(project, "Crabbox doctor Islo", listOf("doctor", "--provider", "islo"))
    }
}

class CrabboxLoginAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val configuredBroker = CrabboxSettingsState.getInstance().state.brokerUrl
        val brokerUrl = Messages.showInputDialog(
            project,
            "Broker URL. Leave empty to use Crabbox's configured/default broker.",
            "Crabbox Login",
            Messages.getQuestionIcon(),
            configuredBroker,
            null,
        ) ?: return

        val args = if (brokerUrl.isBlank()) {
            listOf("login")
        } else {
            listOf("login", "--url", brokerUrl.trim())
        }
        CrabboxTaskRunner.runSimple(project, "Crabbox login", args)
    }
}

class CrabboxInitAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        CrabboxTaskRunner.runSimple(project, "Crabbox init", listOf("init"))
    }
}

class CrabboxSyncPlanAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        CrabboxTaskRunner.runSimple(project, "Crabbox sync-plan", listOf("sync-plan"))
    }
}

class CrabboxWarmupAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val args = listOf("warmup") + CrabboxSettingsState.getInstance().defaultRunArgs()
        CrabboxTaskRunner.runSimple(project, "Crabbox warmup", args)
    }
}

class CrabboxRunCargoTestAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        CrabboxTaskRunner.runRust(project, "Crabbox cargo test", "cargo test")
    }
}

class CrabboxRunCargoTestWorkspaceAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        CrabboxTaskRunner.runRust(project, "Crabbox cargo test --workspace", "cargo test --workspace")
    }
}

class CrabboxRunCargoClippyAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        CrabboxTaskRunner.runRust(project, "Crabbox cargo clippy", "cargo clippy --all-targets")
    }
}

class CrabboxRunCargoNextestAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        CrabboxTaskRunner.runRust(project, "Crabbox cargo nextest", "cargo nextest run")
    }
}

class CrabboxRunCargoTestIsloAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!ensureIsloApiKey(project)) return
        val args = CrabboxSettingsState.getInstance().isloRunArgs()
        CrabboxTaskRunner.runRust(project, "Crabbox cargo test on Islo", "cargo test", args)
    }
}

class CrabboxRunIsloRustSmokeAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!ensureIsloApiKey(project)) return
        val args = CrabboxSettingsState.getInstance().isloRunArgs()
        CrabboxTaskRunner.runRust(project, "Crabbox Islo Rust smoke", "cargo --version", args)
    }
}

class CrabboxStopLeaseAction : CrabboxProjectAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val leaseId = Messages.showInputDialog(
            project,
            "Lease id or slug to stop:",
            "Stop Crabbox Lease",
            Messages.getQuestionIcon(),
        ) ?: return

        if (leaseId.isNotBlank()) {
            CrabboxTaskRunner.runSimple(project, "Crabbox stop ${leaseId.trim()}", listOf("stop", leaseId.trim()))
        }
    }
}

private fun ensureIsloApiKey(project: com.intellij.openapi.project.Project): Boolean {
    if (CrabboxSecrets.hasIsloApiKey()) {
        return true
    }

    val choice = Messages.showYesNoDialog(
        project,
        "Set ISLO_API_KEY in Settings > Crabbox before running Islo sandboxes.",
        "Islo API Key Required",
        "Open Settings",
        "Cancel",
        Messages.getWarningIcon(),
    )
    if (choice == Messages.YES) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Crabbox")
    }
    return false
}
