package se.bernhauser.solitaire.game

import org.junit.Assert.assertNotNull
import org.junit.Test

class StuckMovesTest {
  @Test
  fun `7H moves onto 8S then KS moves to the freed empty column`() {
    val s0 = stuckState()
    // Move 7-Hearts (col 2 = index 1, single face-up card at index 0) onto 8-Spades (col 1 = index 0)
    val s1 = s0.moveToTableau(TableauMoveSource.TableauRun(column = 1, fromIndex = 0), destColumn = 0)
    assertNotNull("7H should move onto 8S", s1)
    // Col 2 (index 1) is now empty. Move K-Spades from waste into it.
    val s2 = s1!!.moveToTableau(TableauMoveSource.WasteTop, destColumn = 1)
    assertNotNull("KS should move from waste to the empty column", s2)
  }
}
