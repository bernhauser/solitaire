package se.bernhauser.solitaire.game.pyramid

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PyramidLayoutTest {
  @Test
  fun `pyramid has 28 slots in rows of 1 through 7`() {
    assertEquals(PyramidSlotCount, PyramidSlots.size)
    assertEquals(
      (1..PyramidRowCount).toList(),
      (0 until PyramidRowCount).map { row -> PyramidSlots.count { it.row == row } },
    )
  }

  @Test
  fun `base row cards are never covered`() {
    PyramidSlots.filter { it.row == PyramidRowCount - 1 }.forEach { slot ->
      assertTrue(slot.children.isEmpty())
    }
  }

  @Test
  fun `every non-base card is covered by exactly two adjacent cards in the next row`() {
    PyramidSlots.forEachIndexed { index, slot ->
      if (slot.row == PyramidRowCount - 1) return@forEachIndexed
      assertEquals("slot $index", 2, slot.children.size)
      slot.children.forEach { child ->
        assertEquals(slot.row + 1, PyramidSlots[child].row)
        assertEquals(1, abs(PyramidSlots[child].xHalf - slot.xHalf))
        assertTrue("children come after their parent", child > index)
      }
    }
  }

  @Test
  fun `known positions match the classic triangle`() {
    // The apex sits centered with the whole second row beneath it.
    assertEquals(6, PyramidSlots[0].xHalf)
    assertEquals(listOf(1, 2), PyramidSlots[0].children)
    // The last row-6 card is covered by the last two base cards.
    assertEquals(listOf(26, 27), PyramidSlots[20].children)
    // Base row spans the full width.
    assertEquals(0, PyramidSlots[21].xHalf)
    assertEquals(12, PyramidSlots[27].xHalf)
  }
}
