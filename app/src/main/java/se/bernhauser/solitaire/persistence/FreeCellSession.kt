package se.bernhauser.solitaire.persistence

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.freecell.FreeCellState

const val FreeCellSessionVersion: Int = 1

@Serializable
data class FreeCellSession(
  val current: FreeCellState,
  val history: List<FreeCellState> = emptyList(),
)
