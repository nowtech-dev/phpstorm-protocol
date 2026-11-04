package dev.nowtech.phpstorm.protocol

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ProtocolStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (!ProtocolService.needsInstall()) {
      return
    }
    synchronized(InstallGate) {
      if (InstallGate.done) {
        return
      }
      InstallGate.done = true
    }
    protocolLog.info("PhpStorm Protocol: scheduling handler registration")
    ProtocolService.installWithNotification(project, showSuccess = false)
  }

  private object InstallGate {
    @Volatile var done = false
  }
}
