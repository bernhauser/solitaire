package se.bernhauser.solitaire.game.pyramid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PyramidDealTest {
  @Test
  fun `deal fills the pyramid and stock with an empty waste`() {
    val s = dealNewPyramid(seed = 1L)
    assertEquals(PyramidSlotCount, s.board.size)
    assertTrue(s.board.all { it != null })
    assertEquals(24, s.stock.size)
    assertTrue(s.waste.isEmpty())
    assertEquals(PyramidRedeals, s.redealsLeft)
  }

  @Test
  fun `deal uses all 52 distinct cards`() {
    val s = dealNewPyramid(seed = 2L)
    val all = s.board.filterNotNull() + s.stock + s.waste
    assertEquals(52, all.size)
    assertEquals(52, all.toSet().size)
  }

  @Test
  fun `only the base row starts uncovered`() {
    val s = dealNewPyramid(seed = 3L)
    val uncovered = s.board.indices.filter { s.isUncovered(it) }
    assertEquals((21 until 28).toList(), uncovered)
  }

  @Test
  fun `deal is deterministic per seed`() {
    assertEquals(dealNewPyramid(seed = 7L), dealNewPyramid(seed = 7L))
    assertNotEquals(dealNewPyramid(seed = 7L), dealNewPyramid(seed = 8L))
  }
}
