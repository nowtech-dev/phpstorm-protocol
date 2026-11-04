package dev.nowtech.phpstorm.protocol

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ProtocolProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isDefault) {
      return
    }
    val basePath = project.basePath ?: return
    ApplicationManager.getApplication().executeOnPooledThread {
      runCatching {
        ProtocolHandlerDeployer.deployIfNeeded()
        ProtocolStateStore.registerCurrentIde(basePath)
      }.onFailure { e ->
        protocolLog.warn("PhpStorm Protocol: project registration failed for \"$basePath\"", e)
      }
    }
  }
}
