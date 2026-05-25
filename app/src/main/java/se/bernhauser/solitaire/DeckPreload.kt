package se.bernhauser.solitaire

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import se.bernhauser.solitaire.game.CardBack

object DeckPreload {
  private var started = false

  fun start(context: PlatformContext, loader: ImageLoader) {
    if (started) return
    started = true

    CardBack.entries.forEach { back ->
      loader.enqueue(
        ImageRequest.Builder(context)
          .data("file:///android_asset/generated_cards/${back.assetKey}.png")
          .build()
      )
    }
  }
}
