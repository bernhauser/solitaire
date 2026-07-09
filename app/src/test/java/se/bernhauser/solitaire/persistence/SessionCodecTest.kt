package se.bernhauser.solitaire.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import se.bernhauser.solitaire.game.klondike.FoundationMoveSource
import se.bernhauser.solitaire.game.klondike.dealNewGame
import se.bernhauser.solitaire.game.klondike.drawFromStock
import se.bernhauser.solitaire.game.klondike.moveToFoundation
import se.bernhauser.solitaire.game.spider.SpiderDifficulty
import se.bernhauser.solitaire.game.spider.dealFromStock
import se.bernhauser.solitaire.game.spider.dealNewSpider

class SessionCodecTest {
  private fun encode(session: KlondikeSession): String =
    SessionCodec.encode(KlondikeSession.serializer(), KlondikeSessionVersion, session)

  private fun decode(raw: String): KlondikeSession? =
    SessionCodec.decode(KlondikeSession.serializer(), KlondikeSessionVersion, raw)

  @Test
  fun emptyStringReturnsNull() {
    assertNull(decode(""))
    assertNull(decode("   "))
  }

  @Test
  fun garbledBlobReturnsNull() {
    assertNull(decode("{not-json"))
    assertNull(decode("{\"hello\":1}"))
  }

  @Test
  fun roundTripFreshDeal() {
    val original = KlondikeSession(current = dealNewGame(seed = 42L))
    val encoded = encode(original)
    val decoded = decode(encoded)
    assertNotNull(decoded)
    assertEquals(original, decoded)
  }

  @Test
  fun roundTripWithHistory() {
    val s0 = dealNewGame(seed = 7L)
    val s1 = s0.drawFromStock() ?: error("draw failed")
    val s2 = s1.moveToFoundation(FoundationMoveSource.WasteTop) ?: s1
    val session = KlondikeSession(current = s2, history = listOf(s1, s0))
    val encoded = encode(session)
    val decoded = decode(encoded)
    assertEquals(session, decoded)
  }

  @Test
  fun versionMismatchReturnsNull() {
    val futureBlob = """{"version":999,"session":{"current":{"stock":[],"waste":[],"foundations":[[],[],[],[]],"tableau":[]},"history":[]}}"""
    assertNull(decode(futureBlob))
  }

  @Test
  fun legacySingleGameBlobStillDecodes() {
    // Exact envelope shape written by the app before persistence became game-scoped.
    val legacy = """{"version":1,"session":{"current":{"stock":[],"waste":[],"foundations":[[],[],[],[]],"tableau":[]},"history":[],"movePossibleSinceLastRecycle":true}}"""
    assertNotNull(decode(legacy))
  }

  @Test
  fun spiderSessionRoundTrip() {
    val s0 = dealNewSpider(SpiderDifficulty.TwoSuits, seed = 9L)
    val s1 = s0.dealFromStock() ?: error("deal failed")
    val session = SpiderSession(difficulty = SpiderDifficulty.TwoSuits, current = s1, history = listOf(s0))
    val encoded = SessionCodec.encode(SpiderSession.serializer(), SpiderSessionVersion, session)
    val decoded = SessionCodec.decode(SpiderSession.serializer(), SpiderSessionVersion, encoded)
    assertEquals(session, decoded)
  }
}
