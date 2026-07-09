package se.bernhauser.solitaire.configuration

sealed class Configuration<T : Any>(val key: String, val defaultValue: T) {
  // Key predates the multi-game split; keep it so existing Klondike saves survive.
  data object KlondikeSavedSession : Configuration<String>(key = "SAVED_SESSION", defaultValue = "")

  data object SpiderSavedSession : Configuration<String>(key = "SPIDER_SESSION", defaultValue = "")

  data object FreeCellSavedSession : Configuration<String>(key = "FREECELL_SESSION", defaultValue = "")

  data object TriPeaksSavedSession : Configuration<String>(key = "TRIPEAKS_SESSION", defaultValue = "")
}
