package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg
import com.mojang.brigadier.arguments.DoubleArgumentType.getDouble
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.LookingPosArgument
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

object VelocityCommand {
  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(
      literal("velocity").then(worldOriented()).then(localOriented()).requires {
        source: ServerCommandSource ->
        source.hasPermissionLevel(2)
      }
    )
  }

  private fun worldOriented(): LiteralArgumentBuilder<ServerCommandSource> {
    return literal("world").then(xyzArgs()).executes { context: CommandContext<ServerCommandSource>
      ->
      // Get the entity from the command source
      val entity = context.source.entity

      // Check if the entity is null, if so send an
      // error message
      if (entity == null) {
        context.source.sendError(Text.of("No entity!"))
        return@executes 0
      }

      // Add velocity to the entity based on the
      // provided x, y, z coordinates
      entity.addVelocity(
        getDouble(context, "x") / 100.0,
        getDouble(context, "y") / 100.0,
        getDouble(context, "z") / 100.0,
      )
      entity.velocityModified = true

      // Send feedback that the velocity has been
      // applied
      context.source.sendFeedback({ Text.of("Applied velocity!") }, false)
      1
    }
  }

  private fun localOriented(): LiteralArgumentBuilder<ServerCommandSource> {
    return literal("local")
      .then(
        argument("x", doubleArg())
          .then(
            argument("y", doubleArg())
              .then(
                argument("z", doubleArg()).executes { context: CommandContext<ServerCommandSource>
                  ->
                  // Get the entity from the command source
                  val entity = context.source.entity

                  // Check if the entity is null, if so send an
                  // error message
                  if (entity == null) {
                    context.source.sendError(Text.of("No entity!"))
                    return@executes 0
                  }

                  // Calculate the absolute velocity based on the
                  // provided x, y, z coordinates
                  val absoluteVel =
                    Vec3d(
                      getDouble(context, "x") / 100.0,
                      getDouble(context, "y") / 100.0,
                      getDouble(context, "z") / 100.0,
                    )
                  val lookingPos = LookingPosArgument(absoluteVel.x, absoluteVel.y, absoluteVel.z)

                  // Calculate the local coordinates for the entity
                  val localCoordinates =
                    lookingPos.getPos(context.source).relativize(entity.pos).negate()

                  // Apply the calculated velocity to the entity
                  entity.addVelocity(localCoordinates.x, localCoordinates.y, localCoordinates.z)
                  entity.velocityModified = true

                  // Send feedback that the velocity has been
                  // applied
                  context.source.sendFeedback({ Text.of("Applied velocity!") }, false)
                  1
                }
              )
          )
      )
  }

  private fun xyzArgs(): RequiredArgumentBuilder<ServerCommandSource, Double> {
    return argument("x", doubleArg())
      .then(argument("y", doubleArg()).then(argument("z", doubleArg())))
  }
}
