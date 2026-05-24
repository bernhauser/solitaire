package se.bernhauser.solitaire

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import java.util.concurrent.atomic.AtomicInteger
import se.bernhauser.solitaire.game.CardBack
import se.bernhauser.solitaire.game.FullDeck

object DeckPreload {
  var backsReady by mutableStateOf(false)
    private set

  private var started = false

  fun start(context: PlatformContext, loader: ImageLoader) {
    if (started) return
    started = true

    val backs = CardBack.entries.map { it.assetKey }
    val remaining = AtomicInteger(backs.size)
    backs.forEach { key ->
      loader.enqueue(
        ImageRequest.Builder(context)
          .data("file:///android_asset/cards/$key.svg")
          .listener(
            onSuccess = { _, _ -> if (remaining.decrementAndGet() == 0) backsReady = true },
            onError = { _, _ -> if (remaining.decrementAndGet() == 0) backsReady = true },
          )
          .build()
      )
    }

    FullDeck.forEach { card ->
      loader.enqueue(
        ImageRequest.Builder(context)
          .data("file:///android_asset/cards/${card.assetKey}.svg")
          .build()
      )
    }
  }
}
