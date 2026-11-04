package dev.nowtech.phpstorm.protocol

import java.nio.file.Path
import java.nio.file.Paths

object ProtocolPaths {

  const val HANDLER_SCRIPT_NAME = "open-url.js"
  const val HANDLER_JSON_NAME = "json2.js"
  const val STATE_FILE_NAME = "state.json"

  fun configDir(): Path =
    Paths.get(System.getProperty("user.home")).resolve(".jetbrains").resolve("phpstorm-protocol")

  fun handlerScript(): Path = configDir().resolve(HANDLER_SCRIPT_NAME)

  fun handlerJsonPolyfill(): Path = configDir().resolve(HANDLER_JSON_NAME)

  fun stateFile(): Path = configDir().resolve(STATE_FILE_NAME)
}
