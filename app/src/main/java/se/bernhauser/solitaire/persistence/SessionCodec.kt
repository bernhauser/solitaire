package se.bernhauser.solitaire.persistence

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SessionCodec {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun <T> encode(serializer: KSerializer<T>, version: Int, session: T): String =
    json.encodeToString(Envelope.serializer(serializer), Envelope(version, session))

  fun <T> decode(serializer: KSerializer<T>, version: Int, raw: String): T? {
    if (raw.isBlank()) return null
    return runCatching {
      val env = json.decodeFromString(Envelope.serializer(serializer), raw)
      if (env.version == version) env.session else null
    }.getOrNull()
  }

  @Serializable
  private data class Envelope<T>(val version: Int, val session: T)
}
