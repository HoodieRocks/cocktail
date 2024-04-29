package me.cobble.cocktail

import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Cocktail : ModInitializer {
  override fun onInitialize() {
    logger.info("Starting Cocktail...")
    logger.info("Loading config...")
  }

  companion object {
    private const val MOD_ID: String = "cocktail"
    val logger: Logger = LoggerFactory.getLogger(MOD_ID)
  }
}
