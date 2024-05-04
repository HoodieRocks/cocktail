package me.cobble.cocktail.commands

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.CommandFunctionArgumentType.commandFunction
import net.minecraft.command.argument.CommandFunctionArgumentType.getFunctions
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.FunctionCommand
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/*
█████ █   █  ███  █   █ █  █   █   █  ███  █   █   █████ █     █████ █████ █████ 
  █   █   █ █   █ ██  █ █ █     █ █  █   █ █   █   █     █     █     █       █   
  █   █████ █████ █ █ █ ███      █   █   █ █   █   █████ █     ████  ████    █   
  █   █   █ █   █ █  ██ █  █     █   █   █ █   █       █ █     █     █       █   
  █   █   █ █   █ █   █ █   █    █    ███  █████   █████ █████ █████ █████   █   
*/

object HTTPCommand {

  private val client: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .followRedirects(HttpClient.Redirect.ALWAYS)
    .build()

  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(
      literal("http").then(
        literal("get").then(
          argument("url", string()).then(
            argument("callback", commandFunction())
              .suggests(FunctionCommand.SUGGESTION_PROVIDER)
              .executes { apply(it) }
          )
        ).requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
      )
    )
  }

  private fun apply(context: CommandContext<ServerCommandSource>): Int {
    val url = URI.create(getString(context, "url"))
    val callback = getFunctions(context, "callback")
    val gson = Gson()

    val request = HttpRequest.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .uri(url)
      .headers("Accept", "application/json")
      .timeout(Duration.ofSeconds(10))
      .GET()
      .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

    val body = response.body()
    val json = gson.fromJson(body, JsonObject::class.java)

    // convert json to nbt
    val compound = jsonToCompound(json)
    val server = context.source.server

    server.commandManager.executeWithPrefix(
      server.commandSource.withSilent(),
      "function ${callback.first().id()} $compound"
    )

    // send feedback
    context.source.sendFeedback({ Text.of("Applied NBT to source") }, false)

    return 1
  }

  /**
   * Converts a json object to a nbt compound
   *
   * @see net.minecraft.stat.ServerStatHandler#jsonToNBT
   */
  private fun jsonToCompound(json: JsonObject): NbtCompound {
    val nbtCompound = NbtCompound()
    val jsonIterator = json.entrySet().iterator()

    while (jsonIterator.hasNext()) {
      val entry: Map.Entry<String, JsonElement> = jsonIterator.next()
      val jsonElement = entry.value
      if (jsonElement.isJsonObject) {
        nbtCompound.put(entry.key, jsonToCompound(jsonElement.asJsonObject))
      } else if (jsonElement.isJsonPrimitive) {
        val jsonPrimitive = jsonElement.asJsonPrimitive
        if (jsonPrimitive.isNumber) {
          nbtCompound.putInt(entry.key, jsonPrimitive.asInt)
        }
        if (jsonPrimitive.isString) {
          nbtCompound.putString(entry.key, jsonPrimitive.asString)
        }
        if (jsonPrimitive.isBoolean) {
          nbtCompound.putBoolean(entry.key, jsonPrimitive.asBoolean)
        }
      }
    }

    return nbtCompound
  }
}
