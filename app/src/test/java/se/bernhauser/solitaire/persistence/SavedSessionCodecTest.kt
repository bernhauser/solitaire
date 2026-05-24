package se.bernhauser.solitaire.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import se.bernhauser.solitaire.game.dealNewGame
import se.bernhauser.solitaire.game.drawFromStock
import se.bernhauser.solitaire.game.moveToFoundation
import se.bernhauser.solitaire.game.FoundationMoveSource

class SavedSessionCodecTest {

  @Test
  fun emptyStringReturnsNull() {
    assertNull(SavedSessionCodec.decode(""))
    assertNull(SavedSessionCodec.decode("   "))
  }

  @Test
  fun garbledBlobReturnsNull() {
    assertNull(SavedSessionCodec.decode("{not-json"))
    assertNull(SavedSessionCodec.decode("{\"hello\":1}"))
  }

  @Test
  fun roundTripFreshDeal() {
    val original = SavedSession(current = dealNewGame(seed = 42L))
    val encoded = SavedSessionCodec.encode(original)
    val decoded = SavedSessionCodec.decode(encoded)
    assertNotNull(decoded)
    assertEquals(original, decoded)
  }

  @Test
  fun roundTripWithHistory() {
    val s0 = dealNewGame(seed = 7L)
    val s1 = s0.drawFromStock() ?: error("draw failed")
    val s2 = s1.moveToFoundation(FoundationMoveSource.WasteTop) ?: s1
    val session = SavedSession(current = s2, history = listOf(s1, s0))
    val encoded = SavedSessionCodec.encode(session)
    val decoded = SavedSessionCodec.decode(encoded)
    assertEquals(session, decoded)
  }

  @Test
  fun versionMismatchReturnsNull() {
    val futureBlob = """{"version":999,"session":{"current":{"stock":[],"waste":[],"foundations":[[],[],[],[]],"tableau":[]},"history":[]}}"""
    assertNull(SavedSessionCodec.decode(futureBlob))
  }
}
