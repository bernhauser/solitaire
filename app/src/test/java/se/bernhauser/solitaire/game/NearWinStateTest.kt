package se.bernhauser.solitaire.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearWinStateTest {
  @Test
  fun `nearWinState is not already won`() {
    assertFalse(nearWinState().isWon())
  }

  @Test
  fun `nearWinState canAutoComplete is true so the Finish button appears`() {
    assertTrue(nearWinState().canAutoComplete())
  }

  @Test
  fun `nearWinState auto-completes to a winning state`() {
    var state = nearWinState()
    while (true) {
      val next = state.nextAutoCompleteSource() ?: break
      state = state.moveToFoundation(next) ?: break
    }
    assertTrue(state.isWon())
  }

  @Test
  fun `nearWinState has exactly four cards left to play`() {
    val s = nearWinState()
    val remaining = s.waste.size + s.tableau.sumOf { it.faceUp.size }
    assertEquals(4, remaining)
    assertNotNull(s.nextAutoCompleteSource())
  }
}
