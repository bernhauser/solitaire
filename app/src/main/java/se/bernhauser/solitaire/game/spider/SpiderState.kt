package se.bernhauser.solitaire.game.spider

import kotlin.random.Random
import kotlinx.serialization.Serializable
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit
import se.bernhauser.solitaire.game.TableauPile

const val SpiderColumnCount: Int = 10
const val SpiderRunCount: Int = 8
const val SpiderDealSize: Int = 10

@Serializable
enum class SpiderDifficulty(val suitCount: Int, val label: String) {
  OneSuit(1, "1 suit"),
  TwoSuits(2, "2 suits"),
  FourSuits(4, "4 suits"),
}

@Serializable
data class SpiderState(
  val stock: List<Card>,
  val completedRuns: List<Suit>,
  val tableau: List<TableauPile>,
)

/** Two decks' worth of cards (104) using 1, 2, or 4 distinct suits. */
fun spiderDeck(difficulty: SpiderDifficulty, seed: Long): List<Card> {
  val suits = when (difficulty) {
    SpiderDifficulty.OneSuit -> List(SpiderRunCount) { Suit.Spades }
    SpiderDifficulty.TwoSuits ->
      List(SpiderRunCount / 2) { Suit.Spades } + List(SpiderRunCount / 2) { Suit.Hearts }
    SpiderDifficulty.FourSuits -> Suit.entries.flatMap { suit -> List(SpiderRunCount / 4) { suit } }
  }
  return suits
    .flatMap { suit -> Rank.entries.map { rank -> Card(rank, suit) } }
    .shuffled(Random(seed))
}

fun dealNewSpider(difficulty: SpiderDifficulty, seed: Long): SpiderState {
  val deck = spiderDeck(difficulty, seed)
  var offset = 0
  val tableau = List(SpiderColumnCount) { col ->
    val size = if (col < 4) 6 else 5
    val cards = deck.subList(offset, offset + size)
    offset += size
    TableauPile(faceDown = cards.dropLast(1), faceUp = listOf(cards.last()))
  }
  return SpiderState(
    stock = deck.subList(offset, deck.size),
    completedRuns = emptyList(),
    tableau = tableau,
  )
}
