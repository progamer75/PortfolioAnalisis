package com.tvsoft.portfolioanalysis.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PA_PREFERENCES_NAME = "pa_preferences"

// Create a DataStore instance using the preferencesDataStore delegate, with the Context as
// receiver.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PA_PREFERENCES_NAME
)

class SettingsDataStore(preference_datastore: DataStore<Preferences>) {
    private val PREF_TOKEN = stringPreferencesKey("token")

    val preferenceFlow: Flow<String> = preference_datastore.data
        .catch {
            if (it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[PREF_TOKEN] ?: ""
        }

    suspend fun saveTokenPreferencesStore(token: String, context: Context) {
        context.dataStore.edit { preferences ->
            preferences[PREF_TOKEN] = token
        }
    }
}