package se.bernhauser.solitaire.configuration

sealed class Configuration<T : Any>(val key: String, val defaultValue: T) {
  data object SavedSession : Configuration<String>(key = SAVED_SESSION_KEY, defaultValue = "")

  companion object {
    const val SAVED_SESSION_KEY = "SAVED_SESSION"
  }
}
