package se.bernhauser.solitaire.game.tripeaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriPeaksDealTest {
  @Test
  fun `deal fills the board, stock and one waste card`() {
    val s = dealNewTriPeaks(seed = 1L)
    assertEquals(TriPeaksSlotCount, s.board.size)
    assertTrue(s.board.all { it != null })
    assertEquals(23, s.stock.size)
    assertEquals(1, s.waste.size)
  }

  @Test
  fun `deal uses all 52 distinct cards`() {
    val s = dealNewTriPeaks(seed = 2L)
    val all = s.board.filterNotNull() + s.stock + s.waste
    assertEquals(52, all.size)
    assertEquals(52, all.toSet().size)
  }

  @Test
  fun `only the base row starts uncovered`() {
    val s = dealNewTriPeaks(seed = 3L)
    val uncovered = s.board.indices.filter { s.isUncovered(it) }
    assertEquals((18 until 28).toList(), uncovered)
  }

  @Test
  fun `deal is deterministic per seed`() {
    assertEquals(dealNewTriPeaks(seed = 7L), dealNewTriPeaks(seed = 7L))
    assertNotEquals(dealNewTriPeaks(seed = 7L), dealNewTriPeaks(seed = 8L))
  }
}
