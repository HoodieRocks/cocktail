package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.EntityArgumentType.entities
import net.minecraft.command.argument.EntityArgumentType.entity
import net.minecraft.command.argument.EntityArgumentType.getEntities
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
        .then(argument("entity", entities()).then(setTarget()).then(clearTarget()))
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
    )
  }

  private fun setTarget(): LiteralArgumentBuilder<ServerCommandSource> {
    return literal("set")
      .then(
        argument("target", entity()).executes { context: CommandContext<ServerCommandSource> ->

          // get entity
          val entities = getEntities(context, "entity")
          val target = getEntity(context, "target")

          if (target !is LivingEntity) {
            context.source.sendError(Text.of("Must be a living entity!"))
            return@executes 0
          }
          if (entities.all { it is MobEntity }) {
            // set target
            entities.forEach { (it as MobEntity).target = target }
            context.source.sendFeedback(
              { Text.literal("Targeting ").append(target.displayName).append("!") },
              false,
            )
            return@executes 1
          } else {
            context.source.sendError(Text.of("Must be a hostile mob!"))
            return@executes 0
          }
        }
      )
  }

  private fun clearTarget(): LiteralArgumentBuilder<ServerCommandSource> {
    return literal("clear").executes { context: CommandContext<ServerCommandSource> ->

      // get entity
      val entities = getEntities(context, "entity")
      if (entities.all { it is MobEntity }) {
        // clear target
        entities.forEach { (it as MobEntity).target = null }
        context.source.sendFeedback({ Text.of("Cleared target!") }, false)
        return@executes 1
      } else {
        context.source.sendError(Text.of("Must be a hostile mob!"))
        return@executes 0
      }
    }
  }
}
