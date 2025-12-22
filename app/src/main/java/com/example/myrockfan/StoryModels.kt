package com.example.myrockfan

// Esto define qué tipo de bloques puede tener nuestra historia
sealed class StorySegment {
    data class Text(val content: String) : StorySegment()
    data class ImagePrompt(val query: String) : StorySegment() // Gemini pide foto
    data class ReadyImage(val url: String, val description: String) : StorySegment() // ¡YA TENEMOS LA FOTO!
}

// El estado de la pantalla (Cargando, Éxito o Error)
sealed class StoryUiState {
    object Loading : StoryUiState()
    object Idle : StoryUiState() // Estado inicial, esperando al usuario
    data class Success(val title: String, val segments: List<StorySegment>) : StoryUiState()
    data class Error(val message: String) : StoryUiState()
}