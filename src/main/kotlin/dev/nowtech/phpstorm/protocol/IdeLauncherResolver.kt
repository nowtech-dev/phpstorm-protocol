package dev.nowtech.phpstorm.protocol

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.PathManager
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

data class IdeLauncherInfo(
  val launcherPath: Path,
  val homePath: Path,
  val build: String,
  val productCode: String,
)

object IdeLauncherResolver {

  private val CANDIDATE_BINARIES =
    listOf(
      "phpstorm64.exe",
      "phpstorm.exe",
      "idea64.exe",
      "idea.exe",
      "webstorm64.exe",
      "webstorm.exe",
    )

  fun currentOrNull(): IdeLauncherInfo? =
    runCatching { current() }.getOrNull()

  fun current(): IdeLauncherInfo {
    val home = PathManager.getHomePath().let { Path.of(it) }
    val binDir = home.resolve("bin")
    val launcher =
      CANDIDATE_BINARIES
        .map { binDir.resolve(it) }
        .firstOrNull { it.isRegularFile() }
        ?: error("No IDE launcher executable found under $binDir")

    val info = ApplicationInfo.getInstance()
    return IdeLauncherInfo(
      launcherPath = launcher.toAbsolutePath().normalize(),
      homePath = home.toAbsolutePath().normalize(),
      build = info.build.asString(),
      productCode = info.build.productCode,
    )
  }

  fun validateLauncher(path: Path) {
    require(Files.isRegularFile(path)) { "Launcher not found: $path" }
  }
}
