package com.example.myrockfan

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creamos la extensión para tener acceso a la base de datos pequeña
val Context.dataStore by preferencesDataStore(name = "rock_settings")

class UserPreferences(private val context: Context) {
    // Claves para identificar los datos
    companion object {
        val SELECTED_BANDS_KEY = stringSetPreferencesKey("selected_bands")
        val IS_ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    // LEER: Obtener bandas guardadas (devuelve un Flujo de datos)
    val getSelectedBands: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_BANDS_KEY] ?: emptySet()
        }

    // LEER: Saber si ya hizo el tutorial
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETED_KEY] ?: false
        }

    // GUARDAR: Guardar la selección
    suspend fun saveBands(bands: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_BANDS_KEY] = bands
            preferences[IS_ONBOARDING_COMPLETED_KEY] = true // Marcamos que ya terminó
        }
    }
}