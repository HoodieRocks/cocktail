package me.cobble.cocktail.commands

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.CommandFunctionArgumentType
import net.minecraft.command.argument.NbtCompoundArgumentType
import net.minecraft.command.argument.NbtPathArgumentType
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.FunctionCommand
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object HTTPCommand {
  private val client: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .followRedirects(HttpClient.Redirect.ALWAYS)
    .build()

  fun register(dispatcher: CommandDispatcher<ServerCommandSource?>) {
    dispatcher.register(
      CommandManager.literal("http").then(
        CommandManager.literal("get").then(
          CommandManager.argument("url", StringArgumentType.string()).then(
            CommandManager.argument("source", NbtCompoundArgumentType.nbtCompound()).then(
              CommandManager.argument("path", NbtPathArgumentType.nbtPath()).then(
                CommandManager.argument("callback", CommandFunctionArgumentType.commandFunction())
                  .suggests(FunctionCommand.SUGGESTION_PROVIDER)
                  .executes { context: CommandContext<ServerCommandSource>? ->
                    val url = URI.create(StringArgumentType.getString(context, "url"))
                    val source = NbtCompoundArgumentType.getNbtCompound(context, "source")
                    val path = NbtPathArgumentType.getNbtPath(context, "path")
                    val callback = CommandFunctionArgumentType.getFunctions(context, "callback")
                    val gson = Gson()

                    val request = HttpRequest.newBuilder()
                      .version(HttpClient.Version.HTTP_2)
                      .uri(url)
                      .headers("Accept", "application/json")
                      .timeout(Duration.ofSeconds(10))
                      .GET()
                      .build()

                    try {
                      val response =
                        client.send(request, HttpResponse.BodyHandlers.ofString())

                      val body = response.body()
                      val json = gson.fromJson(body, JsonObject::class.java)

                      // convert json to nbt
                      val compound = jsonToCompound(json)

                      // apply nbt to source
                      source.put(path.string, compound)

                      // send feedback
                      context?.source?.sendFeedback({ Text.of("Applied nbt to source") }, false)

                      return@executes 1
                    } catch (e: Exception) {
                      e.printStackTrace()
                    }
                    1
                  })
            )
          )
        )
      )
    )
  }

  /**
   * Converts a json object to a nbt compound
   *
   * @see net.minecraft.stat.ServerStatHandler#jsonToNBT
   */
  private fun jsonToCompound(json: JsonObject): NbtCompound {
    val nbtCompound = NbtCompound()
    val jsonIterator: Iterator<*> = json.entrySet().iterator()

    while (jsonIterator.hasNext()) {
      val entry: Map.Entry<String, JsonElement> = jsonIterator.next() as Map.Entry<String, JsonElement>
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
