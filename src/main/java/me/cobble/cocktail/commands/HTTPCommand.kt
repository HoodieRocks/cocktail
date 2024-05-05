package me.cobble.cocktail.commands

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.context.CommandContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import net.minecraft.command.argument.CommandFunctionArgumentType.commandFunction
import net.minecraft.command.argument.CommandFunctionArgumentType.getFunctions
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.FunctionCommand
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

//  _______ _                 _             _____ _           _   _
// |__   __| |               | |           / ____| |         | | | |
//    | |  | |__   __ _ _ __ | | _____    | (___ | | ___  ___| |_| |
//    | |  | '_ \ / _` | '_ \| |/ / __|    \___ \| |/ _ \/ _ \ __| |
//    | |  | | | | (_| | | | |   <\__ \_   ____) | |  __/  __/ |_|_|
//    |_|  |_| |_|\__,_|_| |_|_|\_\___( ) |_____/|_|\___|\___|\__(_)
//                                    |/

object HTTPCommand {

  private val client: HttpClient =
    HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .followRedirects(HttpClient.Redirect.ALWAYS)
      .build()

  fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
    dispatcher.register(
      literal("http")
        .then(
          literal("get")
            .then(
              argument("url", string())
                .then(
                  argument("callback", commandFunction())
                    .suggests(FunctionCommand.SUGGESTION_PROVIDER)
                    .executes { apply(it) }
                )
            )
            .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
        )
    )
  }

  private fun apply(context: CommandContext<ServerCommandSource>): Int {
    val url = URI.create(getString(context, "url"))
    val callback = getFunctions(context, "callback")
    val gson = Gson()

    val request =
      HttpRequest.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .uri(url)
        .headers("Accept", "application/json")
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

    val status = response.statusCode()

    // Status code is not in the OK ranges
    if (!status.toString().startsWith("2")) {
      context.source.sendError(Text.translatable("command.http.status_not_ok"))
      return 0
    }

    val body = response.body()
    val json = gson.fromJson(body, JsonObject::class.java)

    // convert json to nbt
    val compound = jsonToCompound(json)
    val server = context.source.server
    val function = callback.first().id()

    server.commandManager.executeWithPrefix(
      server.commandSource.withSilent(),
      "function $function $compound",
    )

    // send feedback
    context.source.sendFeedback(
      { Text.translatable("command.http.success", function.toString()) },
      false,
    )

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
