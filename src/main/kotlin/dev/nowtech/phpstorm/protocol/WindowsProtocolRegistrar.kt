package dev.nowtech.phpstorm.protocol

import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object WindowsProtocolRegistrar {

  const val REGISTRY_REVISION = 8
  private val PROTOCOLS = listOf("phpstorm", "idea")

  fun isSupported(): Boolean = SystemInfo.isWindows

  fun register(handlerScript: Path): RegistrationResult {
    require(isSupported()) { "URL protocol registration is only supported on Windows" }
    require(handlerScript.toFile().isFile) { "Handler script missing: $handlerScript" }

    val command = buildCommand(handlerScript)
    val errors = mutableListOf<String>()

    for (scheme in PROTOCOLS) {
      runCatching {
        writeProtocolKey(scheme, command)
      }.onFailure { errors += "$scheme: ${it.message ?: it.toString()}" }
    }

    return if (errors.isEmpty()) {
      RegistrationResult(success = true, command = command)
    } else {
      RegistrationResult(success = false, command = command, error = errors.joinToString("\n"))
    }
  }

  fun buildCommand(handlerScript: Path): String {
    val script = handlerScript.toAbsolutePath().normalize().toString()
    return "wscript.exe //E:JScript //B \"${script}\" \"%1\""
  }

  private fun writeProtocolKey(scheme: String, command: String) {
    val base = "HKCU\\Software\\Classes\\$scheme"
    regAdd(base, default = true, name = null, data = "URL:$scheme Protocol")
    regAdd(base, default = false, name = "URL Protocol", data = "")
    regAdd("$base\\shell\\open\\command", default = true, name = null, data = command)
  }

  private fun regAdd(key: String, default: Boolean, name: String?, data: String) {
    val args = mutableListOf("reg", "add", key)
    if (default) {
      args += listOf("/ve", "/d", data)
    } else {
      args += listOf("/v", name!!, "/d", data)
    }
    args += "/f"
    runProcess(args)
  }

  private fun runProcess(args: List<String>) {
    val process =
      ProcessBuilder(args)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    val finished = process.waitFor(30, TimeUnit.SECONDS)
    if (!finished) {
      process.destroyForcibly()
      error("reg.exe timed out: ${args.joinToString(" ")}")
    }
    if (process.exitValue() != 0) {
      error(
        buildString {
          append("reg.exe failed (${process.exitValue()}): ")
          append(args.joinToString(" "))
          if (output.isNotBlank()) {
            append("\n")
            append(output.trim())
          }
        },
      )
    }
  }

  data class RegistrationResult(
    val success: Boolean,
    val command: String,
    val error: String = "",
  )
}
