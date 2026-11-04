package dev.nowtech.phpstorm.protocol

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class RegisterProtocolAction : AnAction(), DumbAware {
  override fun actionPerformed(e: AnActionEvent) {
    ProtocolService.installWithNotification(e.project, showSuccess = true)
  }
}
