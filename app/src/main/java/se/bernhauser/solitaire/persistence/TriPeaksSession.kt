package se.bernhauser.solitaire.persistence

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.tripeaks.TriPeaksState

const val TriPeaksSessionVersion: Int = 1

@Serializable
data class TriPeaksSession(
  val current: TriPeaksState,
  val history: List<TriPeaksState> = emptyList(),
)
