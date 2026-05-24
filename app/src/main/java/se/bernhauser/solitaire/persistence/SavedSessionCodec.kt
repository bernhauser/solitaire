package se.bernhauser.solitaire.persistence

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SavedSessionCodec {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun encode(session: SavedSession): String =
    json.encodeToString(Envelope.serializer(), Envelope(VERSION, session))

  fun decode(raw: String): SavedSession? {
    if (raw.isBlank()) return null
    return runCatching {
      val env = json.decodeFromString(Envelope.serializer(), raw)
      if (env.version == VERSION) env.session else null
    }.getOrNull()
  }

  private const val VERSION = 1

  @Serializable
  private data class Envelope(val version: Int, val session: SavedSession)
}
