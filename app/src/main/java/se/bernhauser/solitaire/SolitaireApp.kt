package se.bernhauser.solitaire

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import se.bernhauser.solitaire.repository.RepositorySupplier
import se.bernhauser.solitaire.repository.SolitaireRepositorySupplier

class SolitaireApp : Application(), SingletonImageLoader.Factory {
  lateinit var repositorySupplier: RepositorySupplier
    private set

  override fun newImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
      .components { add(SvgDecoder.Factory()) }
      .build()

  override fun onCreate() {
    super.onCreate()
    repositorySupplier = SolitaireRepositorySupplier(applicationContext)
    DeckPreload.start(this, SingletonImageLoader.get(this))
  }
}
