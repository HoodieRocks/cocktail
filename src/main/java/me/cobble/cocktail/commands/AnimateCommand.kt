package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import me.cobble.cocktail.commands.utils.AnimationsSuggestionProvider
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/**
 * Command to play animations on server players.
 */
object AnimateCommand {
  /**
   * Registers the 'animate' command with the server.
   *
   * @param dispatcher The command dispatcher to register with.
   */
  fun register(dispatcher: CommandDispatcher<ServerCommandSource?>) {
    dispatcher.register(
      CommandManager.literal("animate")
        .then(
          CommandManager.argument("entity", EntityArgumentType.player())
            .then(
              CommandManager.argument("animation", StringArgumentType.word())
                .suggests(AnimationsSuggestionProvider())
                .executes { context: CommandContext<ServerCommandSource> ->

                  // get entity
                  val player = EntityArgumentType.getPlayer(context, "entity")
                  val animation = StringArgumentType.getString(context, "animation")
                  val animNumber = when (animation) {
                    "swing_main" -> 0
                    "damage" -> 1
                    "leave_bed" -> 2
                    "swing_offhand" -> 3
                    "crit" -> 4
                    "magic_crit" -> 5
                    else -> {
                      context.source.sendError(Text.of("Invalid animation!"))
                      return@executes 0
                    }
                  }
                  player.networkHandler.sendPacket(
                    EntityAnimationS2CPacket(player, animNumber)
                  )
                  context
                    .source
                    .sendFeedback({ Text.of("Playing Animation!") }, false)
                  1
                })
        )
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) })
  }
}
