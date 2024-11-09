package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.string
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object TimezoneCommand {
  /**
   * Registers the "time" command with the dispatcher.
   *
   * @param dispatcher the command dispatcher
   */
  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(
      literal("timezone")
        .then(
          argument("timezone", string()).executes {
            val timezone = TimeZone.of(getString(it, "timezone"))
            val localTime = Clock.System.now()
            val dateTime = localTime.toLocalDateTime(timezone)

            it.source
              .withReturnValueConsumer { successful, returnValue ->
                if (successful) dateTime.toInstant(timezone).epochSeconds else returnValue
              }
              .sendFeedback({ Text.literal("The time is ${dateTime.time}") }, false)
            1
          }
        )
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
    )
  }
}
