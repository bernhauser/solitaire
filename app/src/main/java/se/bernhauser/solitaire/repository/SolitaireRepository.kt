package se.bernhauser.solitaire.repository

import android.content.Context
import kotlinx.serialization.KSerializer
import se.bernhauser.solitaire.configuration.ConfigStorage
import se.bernhauser.solitaire.configuration.Configuration
import se.bernhauser.solitaire.persistence.FreeCellSession
import se.bernhauser.solitaire.persistence.FreeCellSessionVersion
import se.bernhauser.solitaire.persistence.KlondikeSession
import se.bernhauser.solitaire.persistence.KlondikeSessionVersion
import se.bernhauser.solitaire.persistence.SessionCodec
import se.bernhauser.solitaire.persistence.SpiderSession
import se.bernhauser.solitaire.persistence.SpiderSessionVersion

interface RepositorySupplier {
  val klondikeRepo: GameSessionStore<KlondikeSession>
  val spiderRepo: GameSessionStore<SpiderSession>
  val freeCellRepo: GameSessionStore<FreeCellSession>
}

interface GameSessionStore<T : Any> {
  suspend fun load(): T?
  suspend fun save(session: T)
  suspend fun clear()
}

class DataStoreSessionStore<T : Any>(
  private val configStorage: ConfigStorage,
  private val config: Configuration<String>,
  private val serializer: KSerializer<T>,
  private val version: Int,
) : GameSessionStore<T> {
  override suspend fun load(): T? =
    SessionCodec.decode(serializer, version, configStorage.get(config))

  override suspend fun save(session: T) {
    configStorage.saveConfig(config, SessionCodec.encode(serializer, version, session))
  }

  override suspend fun clear() {
    configStorage.saveConfig(config, "")
  }
}

class SolitaireRepositorySupplier(applicationContext: Context) : RepositorySupplier {
  private val configStorage = ConfigStorage(applicationContext)

  override val klondikeRepo: GameSessionStore<KlondikeSession> by lazy {
    DataStoreSessionStore(
      configStorage = configStorage,
      config = Configuration.KlondikeSavedSession,
      serializer = KlondikeSession.serializer(),
      version = KlondikeSessionVersion,
    )
  }

  override val spiderRepo: GameSessionStore<SpiderSession> by lazy {
    DataStoreSessionStore(
      configStorage = configStorage,
      config = Configuration.SpiderSavedSession,
      serializer = SpiderSession.serializer(),
      version = SpiderSessionVersion,
    )
  }

  override val freeCellRepo: GameSessionStore<FreeCellSession> by lazy {
    DataStoreSessionStore(
      configStorage = configStorage,
      config = Configuration.FreeCellSavedSession,
      serializer = FreeCellSession.serializer(),
      version = FreeCellSessionVersion,
    )
  }
}
