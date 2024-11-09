package me.cobble.cocktail.config

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import me.cobble.cocktail.Cocktail
import net.fabricmc.loader.api.FabricLoader

class ConfigIO {
  private val internalConfig = javaClass.classLoader.getResourceAsStream("config.jsonc")
  private val configDir = FabricLoader.getInstance().configDir.resolve("cocktail")
  private val configPath = configDir.resolve("config.jsonc")
  private val logger = Cocktail.logger

  fun createConfig() {
    if (configPath.exists()) return
    configPath.createParentDirectories().createFile()

    if (internalConfig == null) {
      logger.error("Config could not be found in JAR, contact Cbble_ immediately!")
      return
    }

    configPath.writeBytes(internalConfig.readAllBytes())
    logger.info("Wrote config to ${configPath.toAbsolutePath()}")
  }

  fun loadConfig(): ConfigData {
    val gson = Gson()

    if (!configPath.exists()) {
      logger.warn("Config not found, creating...")
      createConfig()
    }

    val configContent = configPath.readText()
    val configData: ConfigData

    try {
      configData = gson.fromJson(configContent, ConfigData::class.java)
    } catch (e: JsonSyntaxException) {
      logger.warn(e.stackTraceToString())
      logger.warn("Config is malformed, attempting to recover...")
      configPath.moveTo(configDir.resolve("config.bak.jsonc"))
      createConfig()
      return loadConfig()
    }

    return configData
  }
}
