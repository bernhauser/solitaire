package se.bernhauser.solitaire.game.spider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.game.Suit

class SpiderDealTest {
  @Test
  fun `deck has 104 cards for every difficulty`() {
    SpiderDifficulty.entries.forEach { difficulty ->
      assertEquals(104, spiderDeck(difficulty, seed = 1L).size)
    }
  }

  @Test
  fun `one suit deck is all spades`() {
    val deck = spiderDeck(SpiderDifficulty.OneSuit, seed = 1L)
    assertTrue(deck.all { it.suit == Suit.Spades })
    assertEquals(8, deck.count { it.rank == Rank.Ace })
  }

  @Test
  fun `two suit deck is half spades half hearts`() {
    val deck = spiderDeck(SpiderDifficulty.TwoSuits, seed = 1L)
    assertEquals(52, deck.count { it.suit == Suit.Spades })
    assertEquals(52, deck.count { it.suit == Suit.Hearts })
  }

  @Test
  fun `four suit deck has two of each card`() {
    val deck = spiderDeck(SpiderDifficulty.FourSuits, seed = 1L)
    Suit.entries.forEach { suit ->
      assertEquals(26, deck.count { it.suit == suit })
    }
    assertEquals(2, deck.count { it.rank == Rank.Queen && it.suit == Suit.Diamonds })
  }

  @Test
  fun `new game deals 54 tableau cards and 50 stock cards`() {
    val state = dealNewSpider(SpiderDifficulty.OneSuit, seed = 7L)
    val tableauCount = state.tableau.sumOf { it.faceDown.size + it.faceUp.size }
    assertEquals(54, tableauCount)
    assertEquals(50, state.stock.size)
    assertEquals(0, state.completedRuns.size)
  }

  @Test
  fun `first four columns get six cards and the rest five`() {
    val state = dealNewSpider(SpiderDifficulty.TwoSuits, seed = 7L)
    val sizes = state.tableau.map { it.faceDown.size + it.faceUp.size }
    assertEquals(listOf(6, 6, 6, 6, 5, 5, 5, 5, 5, 5), sizes)
    assertTrue(state.tableau.all { it.faceUp.size == 1 })
  }
}
