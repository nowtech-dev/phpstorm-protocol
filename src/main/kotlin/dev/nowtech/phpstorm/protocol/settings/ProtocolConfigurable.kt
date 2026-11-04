package dev.nowtech.phpstorm.protocol.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import dev.nowtech.phpstorm.protocol.ProtocolService
import dev.nowtech.phpstorm.protocol.WindowsProtocolRegistrar
import javax.swing.JButton
import javax.swing.JComponent

class ProtocolConfigurable : Configurable {

  private var autoRegisterBox: JBCheckBox? = null
  private var statusLabel: JBLabel? = null

  override fun getDisplayName(): String = "PhpStorm Protocol"

  override fun createComponent(): JComponent {
    val settings = ProtocolSettings.getInstance()
    autoRegisterBox =
      JBCheckBox("Register phpstorm:// and idea:// on IDE startup (Windows)", settings.getState().autoRegisterOnStartup)
    statusLabel = JBLabel(statusText(settings))
    val registerButton =
      JButton("Register handlers now").apply {
        addActionListener {
          ProtocolService.installWithNotification(null, showSuccess = true)
          statusLabel?.text = statusText(ProtocolSettings.getInstance())
        }
      }
    val builder =
      FormBuilder.createFormBuilder()
        .addComponent(autoRegisterBox!!)
        .addComponent(registerButton)
    if (!WindowsProtocolRegistrar.isSupported()) {
      builder.addComponent(JBLabel("Automatic registration is available on Windows only."))
    }
    builder.addComponent(statusLabel!!)
    return builder.panel
  }

  private fun statusText(settings: ProtocolSettings): String {
    val err = settings.getState().lastError
    if (err.isNotBlank()) {
      return "Last error: ${err.take(400)}"
    }
    return "Maps each project (nearest .idea) to this IDE install: ${settings.getState().registryRevision} registry revision."
  }

  override fun isModified(): Boolean {
    val s = ProtocolSettings.getInstance().getState()
    return autoRegisterBox?.isSelected != s.autoRegisterOnStartup
  }

  override fun apply() {
    val s = ProtocolSettings.getInstance().getState()
    s.autoRegisterOnStartup = autoRegisterBox?.isSelected == true
    statusLabel?.text = statusText(ProtocolSettings.getInstance())
  }

  override fun reset() {
    val s = ProtocolSettings.getInstance().getState()
    autoRegisterBox?.isSelected = s.autoRegisterOnStartup
    statusLabel?.text = statusText(ProtocolSettings.getInstance())
  }
}
