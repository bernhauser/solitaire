package se.bernhauser.solitaire.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeckTest {
  @Test
  fun `shuffledDeck contains all 52 cards`() {
    val deck = shuffledDeck(seed = 1L)
    assertEquals(52, deck.size)
    assertEquals(FullDeck.toSet(), deck.toSet())
  }

  @Test
  fun `shuffledDeck is deterministic for the same seed`() {
    val a = shuffledDeck(seed = 42L)
    val b = shuffledDeck(seed = 42L)
    assertEquals(a, b)
  }

  @Test
  fun `shuffledDeck produces different orders for different seeds`() {
    val a = shuffledDeck(seed = 1L)
    val b = shuffledDeck(seed = 2L)
    assertNotEquals(a, b)
  }

  @Test
  fun `shuffledDeck is not the trivial identity order`() {
    val deck = shuffledDeck(seed = 1L)
    assertNotEquals(FullDeck, deck)
  }
}
