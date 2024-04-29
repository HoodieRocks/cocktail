package me.cobble.cocktail.config

object Config {
  private val configIO = ConfigIO()
  private val configData = configIO.loadConfig()

  fun get() = configData
}
