package dev.nowtech.phpstorm.protocol

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import dev.nowtech.phpstorm.protocol.settings.ProtocolSettings

object ProtocolService {

  const val NOTIFICATION_GROUP = "PhpStorm Protocol"

  fun installWithNotification(project: Project? = null, showSuccess: Boolean = true) {
    ApplicationManager.getApplication().executeOnPooledThread {
      val result = runCatching { install(project) }
      ApplicationManager.getApplication().invokeLater {
        val settings = ProtocolSettings.getInstance()
        result.fold(
          onSuccess = { summary ->
            settings.getState().registryRevision = WindowsProtocolRegistrar.REGISTRY_REVISION
            settings.getState().lastError = ""
            protocolLog.info("PhpStorm Protocol: install ok")
            if (showSuccess) {
              notify("Protocol handlers registered", summary, NotificationType.INFORMATION)
            }
          },
          onFailure = { err ->
            val msg = err.message ?: err.toString()
            settings.getState().lastError = msg
            protocolLog.warn("PhpStorm Protocol: install failed", err)
            notify("Protocol handler registration failed", msg, NotificationType.ERROR)
          },
        )
      }
    }
  }

  fun needsInstall(): Boolean {
    val settings = ProtocolSettings.getInstance().getState()
    if (!settings.autoRegisterOnStartup) {
      return false
    }
    if (!WindowsProtocolRegistrar.isSupported()) {
      return false
    }
    if (settings.registryRevision < WindowsProtocolRegistrar.REGISTRY_REVISION) {
      return true
    }
    return !ProtocolHandlerDeployer.isDeployed()
  }

  fun install(project: Project? = null): String {
    if (!WindowsProtocolRegistrar.isSupported()) {
      return "URL protocol registration is only implemented for Windows. On macOS/Linux, use the IDE built-in handler or a desktop entry."
    }

    ProtocolHandlerDeployer.deployIfNeeded()
    val launcher = IdeLauncherResolver.current()
    ProtocolStateStore.registerCurrentIde(project?.basePath)

    val handler = ProtocolPaths.handlerScript()
    val reg = WindowsProtocolRegistrar.register(handler)

    if (!reg.success) {
      error(reg.error.ifBlank { "Registry update failed" })
    }

    return buildString {
      appendLine("Schemes: phpstorm://, idea://")
      appendLine("Handler: $handler")
      appendLine("Default IDE: ${launcher.build}")
      appendLine("Launcher: ${launcher.launcherPath}")
      if (project?.basePath != null) {
        appendLine("Project mapping: ${ProtocolStateStore.normalizeProjectKey(project.basePath!!)}")
      }
    }.trim()
  }

  private fun notify(title: String, content: String, type: NotificationType) {
    Notifications.Bus.notify(Notification(NOTIFICATION_GROUP, title, content, type))
  }
}
