package se.bernhauser.solitaire.game.freecell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeCellDealTest {
  @Test
  fun `deal has four columns of seven and four of six, all face up`() {
    val s = dealNewFreeCell(seed = 1L)
    assertEquals(listOf(7, 7, 7, 7, 6, 6, 6, 6), s.tableau.map { it.faceUp.size })
    assertTrue(s.tableau.all { it.faceDown.isEmpty() })
  }

  @Test
  fun `deal uses all 52 distinct cards`() {
    val s = dealNewFreeCell(seed = 2L)
    val all = s.tableau.flatMap { it.faceUp }
    assertEquals(52, all.size)
    assertEquals(52, all.toSet().size)
  }

  @Test
  fun `deal starts with empty cells and foundations`() {
    val s = dealNewFreeCell(seed = 3L)
    assertEquals(FreeCellCount, s.cells.size)
    assertTrue(s.cells.all { it == null })
    assertEquals(4, s.foundations.size)
    assertTrue(s.foundations.all { it.isEmpty() })
  }

  @Test
  fun `deal is deterministic per seed`() {
    assertEquals(dealNewFreeCell(seed = 7L), dealNewFreeCell(seed = 7L))
    assertNotEquals(dealNewFreeCell(seed = 7L), dealNewFreeCell(seed = 8L))
  }
}
