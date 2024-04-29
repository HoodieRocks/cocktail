package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object SetSlotCommand {
  fun register(dispatcher: CommandDispatcher<ServerCommandSource?>) {
    dispatcher.register(
      CommandManager.literal("setslot")
        .then(
          CommandManager.argument("entity", EntityArgumentType.player())
            .then(
              CommandManager.argument("slot", IntegerArgumentType.integer(0, 8))
                .executes { context: CommandContext<ServerCommandSource> ->

                  // get entity
                  val entity =
                    EntityArgumentType.getPlayer(context, "entity")
                  entity.networkHandler.sendPacket(
                    UpdateSelectedSlotS2CPacket(IntegerArgumentType.getInteger(context, "slot"))
                  )
                  // send feedback
                  context
                    .source
                    .sendFeedback(
                      {
                        Text.of(
                          "Set slot to " + IntegerArgumentType.getInteger(
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
