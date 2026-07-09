package se.bernhauser.solitaire.game.tripeaks

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriPeaksLayoutTest {
  @Test
  fun `board has 28 slots in rows of 3, 6, 9 and 10`() {
    assertEquals(TriPeaksSlotCount, TriPeaksSlots.size)
    assertEquals(
      listOf(3, 6, 9, 10),
      (0 until TriPeaksRowCount).map { row -> TriPeaksSlots.count { it.row == row } },
    )
  }

  @Test
  fun `base row cards are never covered`() {
    TriPeaksSlots.filter { it.row == 3 }.forEach { slot ->
      assertTrue(slot.children.isEmpty())
    }
  }

  @Test
  fun `every non-base card is covered by exactly two adjacent cards in the next row`() {
    TriPeaksSlots.forEachIndexed { index, slot ->
      if (slot.row == 3) return@forEachIndexed
      assertEquals("slot $index", 2, slot.children.size)
      slot.children.forEach { child ->
        assertEquals(slot.row + 1, TriPeaksSlots[child].row)
        assertEquals(1, abs(TriPeaksSlots[child].xHalf - slot.xHalf))
        assertTrue("children come after their parent", child > index)
      }
    }
  }

  @Test
  fun `known positions match the classic layout`() {
    assertEquals(listOf(3, 4), TriPeaksSlots[0].children)
    assertEquals(listOf(5, 6), TriPeaksSlots[1].children)
    assertEquals(listOf(7, 8), TriPeaksSlots[2].children)
    assertEquals(listOf(18, 19), TriPeaksSlots[9].children)
    assertEquals(listOf(26, 27), TriPeaksSlots[17].children)
    // Peaks sit centered above their pair, base row spans the full width.
    assertEquals(listOf(3, 9, 15), TriPeaksSlots.filter { it.row == 0 }.map { it.xHalf })
    assertEquals(0, TriPeaksSlots[18].xHalf)
    assertEquals(18, TriPeaksSlots[27].xHalf)
  }
}
