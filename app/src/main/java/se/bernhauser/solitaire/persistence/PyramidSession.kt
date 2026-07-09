package se.bernhauser.solitaire.persistence

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.pyramid.PyramidState

const val PyramidSessionVersion: Int = 1

@Serializable
data class PyramidSession(
  val current: PyramidState,
  val history: List<PyramidState> = emptyList(),
)
