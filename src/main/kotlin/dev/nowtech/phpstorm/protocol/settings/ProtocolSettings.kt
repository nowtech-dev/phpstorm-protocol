package dev.nowtech.phpstorm.protocol.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "PhpStormProtocolSettings", storages = [Storage("phpstormProtocol.xml")])
class ProtocolSettings : PersistentStateComponent<ProtocolSettings.State> {

  data class State(
    var autoRegisterOnStartup: Boolean = true,
    var registryRevision: Int = 0,
    var lastError: String = "",
  )

  private var state = State()

  override fun getState(): State = state

  override fun loadState(state: State) {
    XmlSerializerUtil.copyBean(state, this.state)
  }

  companion object {
    fun getInstance(): ProtocolSettings =
      ApplicationManager.getApplication().getService(ProtocolSettings::class.java)
  }
}
