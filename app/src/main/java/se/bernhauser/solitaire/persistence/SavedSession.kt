package se.bernhauser.solitaire.persistence

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.GameState

const val UndoLimit: Int = 10

@Serializable
data class SavedSession(
  val current: GameState,
  val history: List<GameState> = emptyList(),
  val movePossibleSinceLastRecycle: Boolean = true,
)
