package me.cobble.cocktail.cmds

import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.doubleArgument
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentManyEntities
import dev.jorel.commandapi.kotlindsl.objectiveArgument
import dev.jorel.commandapi.kotlindsl.proxyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.scoreboard.Objective
import org.bukkit.util.Vector

class VelocityCommand {

  init {
    val precisionExtender = 1000
    val scoreboard = Bukkit.getServer().scoreboardManager?.mainScoreboard!!
    val suggestions =
      ArgumentSuggestions.strings<CommandSender> { scoreboard.objectives.map { it.name }.toTypedArray() }

    // entity.location.direction.multiply(z).add(Vector(0.0, y, 0.0)).add(Vector(0.0, 0.0, x))
    // just saving it for later :)
    commandAPICommand("velocity") {
      subcommand("relative") {
        entitySelectorArgumentManyEntities("entities")
        doubleArgument("x")
        doubleArgument("y")
        doubleArgument("z")
        anyExecutor { sender, args ->
          if (sender.isOp) {
            val entities = args[0] as Collection<*>
            val x = args[1] as Double
            val y = args[2] as Double
            val z = args[3] as Double
            entities.forEach { entity ->
              if (entity is Entity) {
                val axisBase = Vector(0, 0, 1)

                val axisLeft =
                  axisBase.clone()
                    .rotateAroundY(Math.toRadians((-entity.location.yaw + 90.0f).toDouble()))

                val axisUp: Vector = entity.location.direction.clone()
                  .rotateAroundNonUnitAxis(axisLeft, Math.toRadians(-90.0))

                val rotatedX = axisLeft.clone().normalize().multiply(x)
                val rotatedY = axisUp.clone().normalize().multiply(y)
                val rotatedZ = entity.location.direction.clone().multiply(z)
                entity.velocity = rotatedZ.add(rotatedX).add(rotatedY) // Y is STILL broken
              }
            }
          }
        }
        proxyExecutor { sender, args ->
          if (sender.caller.isOp) {
            val entities = args[0] as Collection<*>
            val x = args[1] as Double
            val y = args[2] as Double
            val z = args[3] as Double
            entities.forEach { entity ->
              if (entity is Entity) {
                val axisBase = Vector(0, 0, 1)

                val axisLeft =
                  axisBase.clone()
                    .rotateAroundY(Math.toRadians((-entity.location.yaw + 90.0f).toDouble()))

                val axisUp: Vector = entity.location.direction.clone()
                  .rotateAroundNonUnitAxis(axisLeft, Math.toRadians(-90.0))

                val rotatedX = axisLeft.clone().normalize().multiply(x)
                val rotatedY = axisUp.clone().normalize().multiply(y)
                val rotatedZ = entity.location.direction.clone().multiply(z)
                entity.velocity = Vector(0, 0, 0).add(rotatedZ).add(rotatedX).add(rotatedY)
              }
            }
          }
        }
      }
      subcommand("aligned") {
        entitySelectorArgumentManyEntities("entities")
        doubleArgument("x")
        doubleArgument("y")
        doubleArgument("z")
        anyExecutor { sender, args ->
          if (sender.isOp) {
            val entities = args[0] as ArrayList<*>
            val x = args[1] as Double
            val y = args[2] as Double
            val z = args[3] as Double
            entities.forEach { entity ->
              if (entity is Entity) {
                entity.velocity = Vector(x, y, z)
              }
            }
          }
        }
        proxyExecutor { sender, args ->
          if (sender.caller.isOp) {
            val entities = args[0] as ArrayList<*>
            val x = args[1] as Double
            val y = args[2] as Double
            val z = args[3] as Double
            entities.forEach { entity ->
              if (entity is Entity) {
                entity.velocity = Vector(x, y, z)
              }
            }
          }
        }
      }
      subcommand("scoreboard") {
        entitySelectorArgumentManyEntities("entities")
        objectiveArgument("board")
        anyExecutor { sender, args ->
          if (sender.isOp) {
            val entities = args[0] as ArrayList<*>
            val board = args[1] as Objective
            val x = board.getScore("x").score.toDouble() / precisionExtender
            val y = board.getScore("y").score.toDouble() / precisionExtender
            val z = board.getScore("z").score.toDouble() / precisionExtender
            entities.forEach { entity ->
              if (entity is Entity) {
                entity.velocity = Vector(x, y, z)
              }
            }
          }
        }
        proxyExecutor { sender, args ->
          if (sender.caller.isOp) {
            val entities = args[0] as ArrayList<*>
            val board = scoreboard.getObjective(args[1] as String)!!
            val x = board.getScore("x").score.toDouble() / precisionExtender
            val y = board.getScore("y").score.toDouble() / precisionExtender
            val z = board.getScore("z").score.toDouble() / precisionExtender
            entities.forEach { entity ->
              if (entity is Entity) {
                entity.velocity = Vector(x, y, z)
              }
            }
          }
        }
      }
    }
  }
}
