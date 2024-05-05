package me.cobble.cocktail.commands.utils

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture
import net.minecraft.server.command.ServerCommandSource

class AnimationsSuggestionProvider : SuggestionProvider<ServerCommandSource> {
  override fun getSuggestions(
    context: CommandContext<ServerCommandSource?>,
    builder: SuggestionsBuilder,
  ): CompletableFuture<Suggestions> {
    builder.suggest("swing_main")
    builder.suggest("damage")
    builder.suggest("leave_bed")
    builder.suggest("swing_offhand")
    builder.suggest("crit")
    builder.suggest("magic_crit")

    return builder.buildFuture()
  }
}
