package se.bernhauser.solitaire.repository

import android.content.Context
import se.bernhauser.solitaire.configuration.ConfigStorage
import se.bernhauser.solitaire.configuration.Configuration
import se.bernhauser.solitaire.persistence.SavedSession
import se.bernhauser.solitaire.persistence.SavedSessionCodec

interface RepositorySupplier {
  val gameRepo: SolitaireRepository
}

interface SolitaireRepository {
  suspend fun loadSession(): SavedSession?
  suspend fun saveSession(session: SavedSession)
  suspend fun clearSession()
}

class SolitaireRepositoryImpl(applicationContext: Context) : SolitaireRepository {
  private val configStorage = ConfigStorage(applicationContext)

  override suspend fun loadSession(): SavedSession? =
    SavedSessionCodec.decode(configStorage.get(Configuration.SavedSession))

  override suspend fun saveSession(session: SavedSession) {
    configStorage.saveConfig(Configuration.SavedSession, SavedSessionCodec.encode(session))
  }

  override suspend fun clearSession() {
    configStorage.saveConfig(Configuration.SavedSession, "")
  }
}

class SolitaireRepositorySupplier(private val applicationContext: Context) : RepositorySupplier {
  override val gameRepo: SolitaireRepository by lazy { SolitaireRepositoryImpl(applicationContext) }
}
