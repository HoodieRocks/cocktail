package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/**
 * Handles target related commands.
 */
object TargetCommand {
  /**
   * Registers the "target" command with the dispatcher.
   *
   * @param dispatcher the command dispatcher
   */
  fun register(dispatcher: CommandDispatcher<ServerCommandSource?>) {
    dispatcher.register(
      CommandManager.literal("target")
        .then(
          CommandManager.argument("entity", EntityArgumentType.entity())
            .then(
              CommandManager.literal("set")
                .then(
                  CommandManager.argument("target", EntityArgumentType.entity())
                    .executes { context: CommandContext<ServerCommandSource> ->

                      // get entity
                      val entity = EntityArgumentType.getEntity(context, "entity")
                      val target = EntityArgumentType.getEntity(context, "target")

                      if (target !is LivingEntity) {
                        context
                          .source
                          .sendError(
                            Text.of("Target must be a living entity!")
                          )
                        return@executes 0
                      }
                      if (entity is MobEntity) {
                        // set target
                        entity.target = target
                        context
                          .source
                          .sendFeedback(
                            { Text.of("Set target to " + target.getDisplayName() + "!") },
                            false
                          )
                        return@executes 1
                      } else {
                        context
                          .source
                          .sendError(Text.of("Entity must be a mob entity!"))
                        return@executes 0
                      }
                    })
            )
            .then(
              CommandManager.literal("clear")
                .executes { context: CommandContext<ServerCommandSource> ->

                  // get entity
                  val entity = EntityArgumentType.getEntity(context, "entity")
                  if (entity is MobEntity) {
                    // clear target
                    entity.target = null
                    context
                      .source
                      .sendFeedback({ Text.of("Cleared target!") }, false)
                    return@executes 1
                  } else {
                    context
                      .source
                      .sendError(Text.of("Entity must be a mob entity!"))
                    return@executes 0
                  }
                })
        )
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) })
  }
}
