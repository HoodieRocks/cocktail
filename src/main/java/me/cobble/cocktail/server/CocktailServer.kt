package me.cobble.cocktail.server

import com.mojang.brigadier.CommandDispatcher
import me.cobble.cocktail.Cocktail
import me.cobble.cocktail.commands.AnimateCommand
import me.cobble.cocktail.commands.ComplimentCommand
import me.cobble.cocktail.commands.HTTPCommand
import me.cobble.cocktail.commands.SetSlotCommand
import me.cobble.cocktail.commands.TargetCommand
import me.cobble.cocktail.commands.TimezoneCommand
import me.cobble.cocktail.commands.VelocityCommand
import me.cobble.cocktail.utils.DatapackUpdater
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import org.slf4j.Logger

class CocktailServer : DedicatedServerModInitializer {

  override fun onInitializeServer() {
    sideAgnosticInitialize()
  }

  companion object {
    private val log: Logger = Cocktail.logger

    fun sideAgnosticInitialize() {
      log.info("Loading commands...")

      CommandRegistrationCallback.EVENT.register {
        dispatcher: CommandDispatcher<ServerCommandSource>,
        _,
        _ ->
        VelocityCommand.register(dispatcher)
        TargetCommand.register(dispatcher)
        SetSlotCommand.register(dispatcher)
        AnimateCommand.register(dispatcher)
        ComplimentCommand.register(dispatcher)
        HTTPCommand.register(dispatcher)
        TimezoneCommand.register(dispatcher)
      }

      ServerLifecycleEvents.SERVER_STARTING.register(
        ServerStarting { server: MinecraftServer ->
          val updater = DatapackUpdater(server)
          updater.run()
        }
      )

      ServerLifecycleEvents.SERVER_STARTED.register(
        ServerStarted { server: MinecraftServer ->
          // Ask server to reload data
          log.info("Reloading datapacks...")
          server.commandManager.executeWithPrefix(server.commandSource, "reload")
        }
      )

      log.info("Done!")
    }
  }
}
