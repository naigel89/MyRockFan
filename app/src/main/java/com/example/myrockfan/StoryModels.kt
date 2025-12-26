package com.example.myrockfan

/**
 * MODELADO DE CONTENIDO HETEROGÉNEO (Polimorfismo).
 *
 * En lugar de tener una lista simple de Strings, usamos una 'Sealed Class' para definir
 * los bloques de construcción de nuestra narrativa.
 *
 * Esto permite que el `LazyColumn` (la lista visual) pueda renderizar cosas muy distintas
 * (un párrafo de texto, una tarjeta de imagen o un reproductor) en el orden exacto
 * en que la IA las generó, manteniendo el tipo de dato seguro.
 */
sealed class StorySegment {
    // Bloque básico de narración (Párrafos).
    data class Text(val content: String) : StorySegment()

    // ESTADO INTERMEDIO (Intención):
    // Representa una instrucción de la IA ("Quiero una foto de X").
    // La UI muestra un "Skeleton" o cargando mientras este prompt se procesa en segundo plano.
    data class ImagePrompt(val query: String) : StorySegment()

    // ESTADO FINAL (Resolución):
    // Representa el resultado tras la búsqueda en Google. Contiene la URL real para Coil.
    // El ViewModel transforma los 'ImagePrompt' en 'ReadyImage'.
    data class ReadyImage(val url: String, val description: String) : StorySegment()

    // Bloque de metadatos musicales (para futuras integraciones o visualización especial).
    data class TrackRecommendation(val artist: String, val track: String, val coverUrl: String? = null) : StorySegment()
}

/**
 * MÁQUINA DE ESTADOS DE LA UI (State Management).
 *
 * Implementación del patrón MVI/MVVM para la Vista.
 * Garantiza que la pantalla solo pueda estar en UNA situación lógica a la vez,
 * evitando bugs visuales como mostrar un spinner de carga encima de un mensaje de error.
 */
sealed class StoryUiState {
    // El usuario está esperando. Mostramos la animación del Vinilo.
    object Loading : StoryUiState()

    // Estado neutro/inicial. Mostramos la Guitarra interactiva.
    object Idle : StoryUiState()

    // La historia se generó y procesó correctamente.
    // Contiene la "payload" completa: el Título y la lista ordenada de segmentos listos para pintar.
    data class Success(val title: String, val segments: List<StorySegment>) : StoryUiState()

    // Algo falló (API caída, sin internet, cuota agotada).
    // Contiene el mensaje para feedback al usuario.
    data class Error(val message: String) : StoryUiState()
}