package me.cobble.cocktail.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.LookingPosArgument
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

object VelocityCommand {
  fun register(dispatcher: CommandDispatcher<ServerCommandSource?>) {
    dispatcher.register(
      CommandManager.literal("velocity")
        .then(
          CommandManager.literal("world")
            .then(
              CommandManager.argument("x", DoubleArgumentType.doubleArg())
                .then(
                  CommandManager.argument("y", DoubleArgumentType.doubleArg())
                    .then(
                      CommandManager.argument("z", DoubleArgumentType.doubleArg())
                        .executes { context: CommandContext<ServerCommandSource> ->
                          // Get the entity from the command source
                          val entity = context.source.entity

                          // Check if the entity is null, if so send an
                          // error message
                          if (entity == null) {
                            context
                              .source
                              .sendError(Text.of("No entity found!"))
                            return@executes 0
                          }

                          // Add velocity to the entity based on the
                          // provided x, y, z coordinates
                          entity.addVelocity(
                            DoubleArgumentType.getDouble(context, "x") / 100.0,
                            DoubleArgumentType.getDouble(context, "y") / 100.0,
                            DoubleArgumentType.getDouble(context, "z") / 100.0
                          )
                          entity.velocityModified = true

                          // Send feedback that the velocity has been
                          // applied
                          context
                            .source
                            .sendFeedback(
                              { Text.of("Applied velocity!") },
                              false
                            )
                          1
                        })
                )
            )
        )
        .then(
          CommandManager.literal("local")
            .then(
              CommandManager.argument("x", DoubleArgumentType.doubleArg())
                .then(
                  CommandManager.argument("y", DoubleArgumentType.doubleArg())
                    .then(
                      CommandManager.argument("z", DoubleArgumentType.doubleArg())
                        .executes { context: CommandContext<ServerCommandSource> ->
                          // Get the entity from the command source
                          val entity = context.source.entity

                          // Check if the entity is null, if so send an
                          // error message
                          if (entity == null) {
                            context
                              .source
                              .sendError(Text.of("No entity found!"))
                            return@executes 0
                          }

                          // Calculate the absolute velocity based on the
                          // provided x, y, z coordinates
                          val absoluteVel =
                            Vec3d(
                              DoubleArgumentType.getDouble(context, "x") / 100.0,
                              DoubleArgumentType.getDouble(context, "y") / 100.0,
                              DoubleArgumentType.getDouble(context, "z") / 100.0
                            )
                          val lookingPos =
                            LookingPosArgument(
                              absoluteVel.x,
                              absoluteVel.y,
                              absoluteVel.z
                            )

                          // Calculate the local coordinates for the entity
                          val localCoordinates =
                            lookingPos
                              .toAbsolutePos(context.source)
                              .relativize(entity.pos)
                              .negate()

                          // Apply the calculated velocity to the entity
                          entity.addVelocity(
                            localCoordinates.x,
                            localCoordinates.y,
                            localCoordinates.z
                          )
                          entity.velocityModified = true

                          // Send feedback that the velocity has been
                          // applied
                          context
                            .source
                            .sendFeedback(
                              { Text.of("Applied velocity!") },
                              false
                            )
                          1
                        })
                )
            )
        )
        .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) })
  }
}
