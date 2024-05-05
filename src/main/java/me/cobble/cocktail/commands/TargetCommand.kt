package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EntityArgumentType.entity
import net.minecraft.command.argument.EntityArgumentType.getEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/** Handles target related commands. */
object TargetCommand {
  /**
   * Registers the "target" command with the dispatcher.
   *
   * @param dispatcher the command dispatcher
   */
  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(
      literal("target")
        .then(
          argument("entity", entity())
            .then(
              literal("set")
                .then(
                  argument("target", entity()).executes {
                    context: CommandContext<ServerCommandSource> ->

                    // get entity
                    val entity = getEntity(context, "entity")
                    val target = getEntity(context, "target")

                    if (target !is LivingEntity) {
                      context.source.sendError(Text.of("Must be a living entity!"))
                      return@executes 0
                    }
                    if (entity is MobEntity) {
                      // set target
                      entity.target = target
                      context.source.sendFeedback(
                        { Text.of("Set target to ${target.displayName}!") },
                        false,
                      )
                      return@executes 1
                    } else {
                      context.source.sendError(Text.of("Must be a hostile mob!"))
                      return@executes 0
                    }
                  }
                )
            )
            .then(
              literal("clear").executes { context: CommandContext<ServerCommandSource> ->

                // get entity
                val entity = getEntity(context, "entity")
                if (entity is MobEntity) {
                  // clear target
                  entity.target = null
                  context.source.sendFeedback({ Text.of("Cleared target!") }, false)
                  return@executes 1
                } else {
                  context.source.sendError(Text.of("Must be a hostile mob!"))
                  return@executes 0
                }
              }
            )
        )
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
    )
  }
}
