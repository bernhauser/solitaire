package se.bernhauser.solitaire.persistence

import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.spider.SpiderDifficulty
import se.bernhauser.solitaire.game.spider.SpiderState

const val SpiderSessionVersion: Int = 1

@Serializable
data class SpiderSession(
  val difficulty: SpiderDifficulty,
  val current: SpiderState,
  val history: List<SpiderState> = emptyList(),
)
