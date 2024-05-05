package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.context.CommandContext
import me.cobble.cocktail.commands.utils.AnimationsSuggestionProvider
import net.minecraft.command.argument.EntityArgumentType.getPlayer
import net.minecraft.command.argument.EntityArgumentType.player
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/** Command to play animations on server players. */
object AnimateCommand {
  /**
   * Registers the 'animate' command with the server.
   *
   * @param dispatcher The command dispatcher to register with.
   */
  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(
      literal("animate")
        .then(
          argument("entity", player())
            .then(
              argument("animation", word()).suggests(AnimationsSuggestionProvider()).executes {
                context: CommandContext<ServerCommandSource> ->

                // get entity
                val player = getPlayer(context, "entity")
                val animation = getString(context, "animation")
                val animNumber =
                  when (animation) {
                    "swing_main" -> 0
                    "damage" -> 1
                    "leave_bed" -> 2
                    "swing_offhand" -> 3
                    "crit" -> 4
                    "magic_crit" -> 5
                    else -> {
                      context.source.sendError(Text.translatable("command.animate.invalid"))
                      return@executes 0
                    }
                  }
                player.networkHandler.sendPacket(EntityAnimationS2CPacket(player, animNumber))
                context.source.sendFeedback({ Text.translatable("command.animate.success") }, false)
                1
              }
            )
        )
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
    )
  }
}
