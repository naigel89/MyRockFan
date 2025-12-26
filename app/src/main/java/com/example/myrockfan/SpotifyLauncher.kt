package com.example.myrockfan

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast

/**
 * Módulo de Integración Externa (Bridge).
 *
 * Esta función actúa como puente entre nuestra App y el ecosistema de música del teléfono.
 * Implementa una estrategia de "Intent Chain" (Cadena de Intentos) para maximizar
 * la probabilidad de que la música suene.
 */
fun playOnSpotify(context: Context, artist: String, song: String) {
    try {
        val query = "$artist $song"

        // ESTRATEGIA 1: REPRODUCCIÓN DIRECTA (La opción ideal)
        // Usamos la API estándar de Android (MediaStore) diseñada para comandos de voz y automatización.
        // Si el usuario tiene Spotify Premium, esto inicia el audio automáticamente sin que toque nada.
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Metadatos específicos para ayudar al algoritmo de búsqueda de la app externa
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
            putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
            putExtra(MediaStore.EXTRA_MEDIA_TITLE, song)
            putExtra(SearchManager.QUERY, query)
            // Restricción de Paquete: Forzamos que SOLO esta app responda (evita que se abra YouTube por error)
            setPackage("com.spotify.music")
        }

        // Verificamos si el sistema operativo puede resolver este intent (¿Está la app instalada?)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // ESTRATEGIA 2: DEEP LINKING POR URI (Plan B)
            // Si la API de reproducción falla, intentamos usar el esquema de URL propietario de Spotify.
            // Esto no reproduce automáticamente, pero lleva al usuario a la pantalla de resultados.
            val searchIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = android.net.Uri.parse("spotify:search:$query")
                setPackage("com.spotify.music")
            }

            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
            } else {
                // ESTRATEGIA 3: FALLBACK UNIVERSAL (Plan C)
                // Si el usuario no tiene Spotify, no le dejamos tirado.
                // Lanzamos una petición abierta al sistema para que responda CUALQUIER app de música
                // (YouTube Music, Amazon Music, VLC, reproductor nativo, etc).
                val genericIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(SearchManager.QUERY, query)
                }
                if (genericIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(genericIntent)
                } else {
                    // Si llegamos aquí, el teléfono no tiene ninguna app capaz de reproducir audio.
                    Toast.makeText(context, "No se encontró app de música (Spotify)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } catch (e: Exception) {
        // Captura de errores de seguridad o SecurityExceptions inesperados
        Toast.makeText(context, "Error al abrir Spotify", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}