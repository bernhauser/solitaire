package se.bernhauser.solitaire.persistence

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.klondike.KlondikeState

const val UndoLimit: Int = 10

const val KlondikeSessionVersion: Int = 1

@Serializable
data class KlondikeSession(
  val current: KlondikeState,
  val history: List<KlondikeState> = emptyList(),
  val movePossibleSinceLastRecycle: Boolean = true,
)
