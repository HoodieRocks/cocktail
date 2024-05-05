package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object ComplimentCommand {
  /**
   * Registers the 'compliment' command with the server.
   *
   * @param dispatcher The command dispatcher to register with.
   */
  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(literal("compliment")
      .executes { context: CommandContext<ServerCommandSource> ->
        context.source.sendFeedback({ Text.translatable("command.compliment.compliment") }, false)
        1
      }
      .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) })
  }
}
