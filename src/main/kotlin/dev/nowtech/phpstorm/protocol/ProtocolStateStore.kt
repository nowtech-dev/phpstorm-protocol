package dev.nowtech.phpstorm.protocol

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

data class ProjectProtocolEntry(
  var launcher: String = "",
  var homePath: String = "",
  var build: String = "",
  var productCode: String = "",
  var registeredAt: String = "",
)

data class ProtocolState(
  var schemaVersion: Int = 1,
  var handlerScriptPath: String = "",
  var defaultLauncher: String = "",
  var defaultBuild: String = "",
  var projects: MutableMap<String, ProjectProtocolEntry> = linkedMapOf(),
)

object ProtocolStateStore {

  private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

  fun load(): ProtocolState {
    val file = ProtocolPaths.stateFile()
    if (!file.exists()) {
      return ProtocolState()
    }
    val json = Files.readString(file, StandardCharsets.UTF_8)
    if (json.isBlank()) {
      return ProtocolState()
    }
    return gson.fromJson(json, ProtocolState::class.java) ?: ProtocolState()
  }

  fun save(state: ProtocolState) {
    val dir = ProtocolPaths.configDir()
    Files.createDirectories(dir)
    val json = gson.toJson(state)
    Files.writeString(ProtocolPaths.stateFile(), json, StandardCharsets.UTF_8)
  }

  fun registerCurrentIde(projectBasePath: String?) {
    val launcher = IdeLauncherResolver.current()
    val state = load()
    state.defaultLauncher = launcher.launcherPath.toString()
    state.defaultBuild = launcher.build
    state.handlerScriptPath = ProtocolPaths.handlerScript().toString()

    val base = projectBasePath?.trim()?.takeIf { it.isNotEmpty() }
    if (base != null) {
      val normalized = normalizeProjectKey(base)
      if (normalized.isNotEmpty()) {
        state.projects[normalized] =
          ProjectProtocolEntry(
            launcher = launcher.launcherPath.toString(),
            homePath = launcher.homePath.toString(),
            build = launcher.build,
            productCode = launcher.productCode,
            registeredAt = java.time.Instant.now().toString(),
          )
        protocolLog.info("PhpStorm Protocol: registered project \"$normalized\" -> ${launcher.build}")
      }
    }
    save(state)
  }

  fun normalizeProjectKey(basePath: String): String {
    val path = Path.of(basePath.replace('/', '\\')).toAbsolutePath().normalize()
    if (!path.isDirectory()) {
      return path.parent?.toString()?.trimEnd('\\') ?: ""
    }
    return path.toString().trimEnd('\\')
  }
}
