package dev.nowtech.phpstorm.protocol

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

object ProtocolHandlerDeployer {

  private const val HANDLER_RESOURCE = "/protocol/open-url.js"
  private const val JSON_RESOURCE = "/protocol/json2.js"

  fun deployIfNeeded(): PathResult {
    val dir = ProtocolPaths.configDir()
    Files.createDirectories(dir)

    copyResource(HANDLER_RESOURCE, ProtocolPaths.handlerScript())
    copyResource(JSON_RESOURCE, ProtocolPaths.handlerJsonPolyfill())

    return PathResult(ProtocolPaths.handlerScript(), updated = true)
  }

  private fun copyResource(resourcePath: String, target: java.nio.file.Path) {
    val resource =
      ProtocolHandlerDeployer::class.java.getResourceAsStream(resourcePath)
        ?: error("Missing bundled handler resource: $resourcePath")
    resource.use { input ->
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  data class PathResult(val path: java.nio.file.Path, val updated: Boolean)

  fun readBundledRevision(): Int {
    val resource =
      ProtocolHandlerDeployer::class.java.getResourceAsStream(HANDLER_RESOURCE)
        ?: return 0
    val text = resource.bufferedReader(StandardCharsets.UTF_8).readText()
    val marker = "// revision:"
    val line = text.lineSequence().firstOrNull { it.trimStart().startsWith(marker) }
    return line?.substringAfter(marker)?.trim()?.toIntOrNull() ?: 3
  }

  fun isDeployed(): Boolean =
    ProtocolPaths.handlerScript().exists() && ProtocolPaths.handlerJsonPolyfill().exists()
}
