package se.bernhauser.solitaire.configuration

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ConfigStorage(private val context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {

  suspend fun <T : Any> saveConfig(config: Configuration<T>, value: T) {
    withContext(ioDispatcher) {
      set(config, value)
    }
  }

  @Suppress("UNCHECKED_CAST")
  suspend fun <T : Any> get(config: Configuration<T>): T {
    val prefs = context.configurationDataStore.data.first()
    val key = config.key

    return when (val default = config.defaultValue) {
      is String -> (prefs[stringPreferencesKey(name = key)] ?: default) as T
      is Boolean -> (prefs[booleanPreferencesKey(name = key)] ?: default) as T
      is Long -> (prefs[longPreferencesKey(name = key)] ?: default) as T
      is Float -> (prefs[floatPreferencesKey(name = key)] ?: default) as T
      is Int -> (prefs[intPreferencesKey(name = key)] ?: default) as T
      else -> throw IllegalArgumentException("ConfigStorage.get(): Unsupported type: $key")
    }
  }

  private suspend fun <T : Any> set(config: Configuration<T>, value: T) {
    try {
      val dataStore = context.configurationDataStore
      when (config.defaultValue) {
        is String -> dataStore.edit { it[stringPreferencesKey(name = config.key)] = value as String }
        is Boolean -> dataStore.edit { it[booleanPreferencesKey(name = config.key)] = value as Boolean }
        is Long -> dataStore.edit { it[longPreferencesKey(name = config.key)] = value as Long }
        is Float -> dataStore.edit { it[floatPreferencesKey(name = config.key)] = value as Float }
        is Int -> dataStore.edit { it[intPreferencesKey(name = config.key)] = value as Int }
      }
    } catch (e: Exception) {
      Log.e(TAG, "ConfigStorage.set() Error saving config for key: ${config.key}, value: $value", e)
    }
  }

  companion object {
    private const val TAG = "ConfigStorage"
    private val Context.configurationDataStore by preferencesDataStore(name = "solitaire_config")
  }
}
