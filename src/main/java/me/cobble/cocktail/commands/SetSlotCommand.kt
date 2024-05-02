package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EntityArgumentType.getPlayer
import net.minecraft.command.argument.EntityArgumentType.player
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object SetSlotCommand {
  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(
      literal("setslot")
        .then(
          argument("entity", player())
            .then(
              argument("slot", integer(0, 8))
                .executes { context: CommandContext<ServerCommandSource> ->

                  // get entity
                  val entity = getPlayer(context, "entity")
                  entity.networkHandler.sendPacket(
                    UpdateSelectedSlotS2CPacket(getInteger(context, "slot"))
                  )
                  // send feedback
                  context
                    .source
                    .sendFeedback(
                      {
                        Text.of(
                          "Set slot to " + getInteger(
                            context,
                            "slot"
                          )
                        )
                      },
                      false
                    )
                  1
                })
        )
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) })
  }
}
