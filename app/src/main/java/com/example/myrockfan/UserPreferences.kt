package com.example.myrockfan

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gestión de persistencia ligera.
 * Se utiliza Jetpack DataStore para almacenar preferencias simples sin la sobrecarga de una base de datos SQL, 
 * garantizando operaciones de lectura y escritura asíncronas seguras en el hilo principal.
 */
val Context.dataStore by preferencesDataStore(name = "rock_settings")

/**
 * Repositorio de configuración de usuario.
 * Centraliza el acceso a las preferencias, actuando como una única fuente de verdad para el estado de personalización de la app.
 */
class UserPreferences(private val context: Context) {
    
    /**
     * Definición de esquemas de datos.
     * Utiliza claves tipadas para evitar errores de casting en tiempo de ejecución y asegurar la integridad de los datos persistidos.
     */
    companion object {
        val SELECTED_BANDS_KEY = stringSetPreferencesKey("selected_bands")
        val IS_ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    /**
     * Flujo reactivo de selección de bandas.
     * Expone un Flow que emite actualizaciones automáticamente cada vez que el usuario modifica su lista de favoritos, 
     * permitiendo que la UI responda en tiempo real sin recargas manuales.
     */
    val getSelectedBands: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_BANDS_KEY] ?: emptySet()
        }

    /**
     * Indicador de estado del ciclo de vida del usuario.
     * Determina si la aplicación debe mostrar la pantalla de bienvenida o saltar directamente a la experiencia principal 
     * basándose en la finalización previa del flujo de configuración.
     */
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETED_KEY] ?: false
        }

    /**
     * Persistencia atómica de configuración.
     * Agrupa la actualización de bandas y el estado del tutorial en una sola transacción para evitar 
     * inconsistencias (como marcar el tutorial como terminado sin haber guardado las bandas).
     */
    suspend fun saveBands(bands: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_BANDS_KEY] = bands
            preferences[IS_ONBOARDING_COMPLETED_KEY] = true
        }
    }
}
