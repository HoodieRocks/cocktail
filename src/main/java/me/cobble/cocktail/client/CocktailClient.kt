package me.cobble.cocktail.client

import me.cobble.cocktail.server.CocktailServer
import net.fabricmc.api.ClientModInitializer

class CocktailClient : ClientModInitializer {
  override fun onInitializeClient() {
    CocktailServer.sideAgnosticInitialize()
  }
}
