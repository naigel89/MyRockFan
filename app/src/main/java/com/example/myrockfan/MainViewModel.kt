package com.example.myrockfan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)

    // Configuración de Gemini
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.apiKey,
        systemInstruction = content {
            text("""
                // --- PERSONALIDAD (EL ALMA) ---
                Eres el "Cronista de la Madrugada", un locutor de radio legendario que habla a los amantes del Rock en la intimidad de la noche.
                Tu tono es: Evocador, algo canalla, nostálgico y épico. No das datos de Wikipedia, cuentas LEYENDAS.
                Usa un español neutro pero con carácter. Usa pausas dramáticas.
                
                // --- PROFUNDIDAD NARRATIVA (¡IMPORTANTE!) ---
                No quiero un resumen rápido. TÓMATE TU TIEMPO para contar la historia.
                1. AMBIENTACIÓN: Antes de contar el hecho, describe el lugar. ¿Huele a cerveza rancia? ¿Llueve en Londres? ¿Hay humo en el estudio?
                2. SENSACIONES: Describe el sonido, la tensión en el aire, las miradas entre los músicos.
                3. EXTENSIÓN: La historia debe tener cuerpo. Mínimo 3 o 4 párrafos bien desarrollados entre foto y foto.
                
                // --- FORMATO TÉCNICO (LA LEY) ---
                Para que la app funcione, debes seguir estas reglas ESTRICTAS de estructura:
                
                1. FORMATO DE TEXTO: Usa Markdown para dar énfasis (ej: **Angus Young**, *Back in Black*).
                2. TÍTULO: Empieza siempre con un título corto e impactante (sin símbolos #).
                
                3. IMÁGENES: Aquí es donde debes ser un ROBOT.
                   - NO describas la escena ("Una foto de la banda sonriendo...").
                   - Escribe la etiqueta EXACTA: [[FOTO: keywords]]
                   - DENTRO de la etiqueta, usa SOLO palabras clave en INGLÉS para el buscador (Google entiende mejor inglés).
                   
               >>> REGLA DE ORO PARA LA PORTADA (La primera foto):
               La PRIMERA imagen que pidas debe ser SIEMPRE para la CABECERA (Header).
               - Debe ser espectacular y atmosférica.
               - AÑADE SIEMPRE estas palabras clave al final de la query: "wide shot wallpaper cinematic 4k horizontal"
               - EJEMPLO PORTADA: [[FOTO: Pink Floyd live Pompeii wide shot cinematic lighting wallpaper]]
                   
               >>> IMÁGENES DEL CUERPO (Las siguientes):
               - Pueden ser primeros planos o detalles.
               - EJEMPLO CUERPO: [[FOTO: David Gilmour fender stratocaster black close up]]
               
                // --- EJEMPLO DE RESPUESTA PERFECTA ---
                [[FOTO: Query de Portada (Wide/Wallpaper)]]
                
                La noche que el diablo afinó la guitarra
                
                Cuentan las malas lenguas que esa noche hacía un frío que cortaba la respiración...
                
                [[FOTO: Robert Johnson crossroad blues guitar 1930s]]
                
                Pero él no tenía miedo. **Robert Johnson** se plantó en el cruce de caminos...
            """.trimIndent())
        }
    )

    // Estado para la UI
    private val _uiState = MutableStateFlow<StoryUiState>(StoryUiState.Idle)
    val uiState: StateFlow<StoryUiState> = _uiState

    fun generateDailyCuriosity() {
        viewModelScope.launch {
            _uiState.value = StoryUiState.Loading

            try {
                // 1. Obtener bandas guardadas
                val bands = userPreferences.getSelectedBands.first()
                if (bands.isEmpty()) {
                    _uiState.value = StoryUiState.Error("No has seleccionado bandas favoritas.")
                    return@launch
                }
                val randomBand = bands.random()

                // 2. Llamar a Gemini
                val response = generativeModel.generateContent("Historia curiosa sobre: $randomBand")
                val fullText = response.text ?: ""

                // 3. Procesar la respuesta
                val lines = fullText.split("\n").filter { it.isNotBlank() }

                val title = lines.firstOrNull { !it.trim().startsWith("[[") }
                    ?.replace("#", "")?.trim()
                    ?: "Rock Story"

                val bodyText = lines.drop(1).joinToString("\n")

                val initialSegments = parseResponseToSegments(bodyText)

                // 4. Parsear los segmentos
                val finalSegments = initialSegments.map { segment ->
                    async(Dispatchers.IO) { // <--- ESTO CREA LA CORRUTINA PARA CADA IMAGEN
                        if (segment is StorySegment.ImagePrompt) {
                            var url: String? = null

                            // A. Intentamos Google Search
                            try {
                                url = ImageRepository.searchImage(segment.query)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // B. El Paracaídas (Picsum) si Google falla
                            if (url.isNullOrBlank()) {
                                val seed = segment.query.hashCode()
                                url = "https://picsum.photos/seed/$seed/800/1200"
                            }

                            // Devolvemos la imagen lista
                            StorySegment.ReadyImage(url!!, segment.query)
                        } else {
                            // Si es texto, lo dejamos tal cual
                            segment
                        }
                    }
                }.awaitAll()

                _uiState.value = StoryUiState.Success(title, finalSegments)

            } catch (e: Exception) {
                _uiState.value = StoryUiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    private fun parseResponseToSegments(text: String): List<StorySegment> {
        val segments = mutableListOf<StorySegment>()
        val regex = """\[\[(?i)FOTO:\s*(.*?)]]""".toRegex(RegexOption.DOT_MATCHES_ALL)
        var lastIndex = 0

        regex.findAll(text).forEach { matchResult ->
            // 1. Texto narrativo ANTES de la foto
            val textBefore = text.substring(lastIndex, matchResult.range.first).trim()
            if (textBefore.isNotEmpty()) {
                segments.add(StorySegment.Text(textBefore))
            }

            // 2. Extraer las keywords limpias
            // groupValues[1] coge lo que hay dentro de los paréntesis del regex (.*?)
            val rawQuery = matchResult.groupValues[1].trim().replace("\n", " ")
            segments.add(StorySegment.ImagePrompt(rawQuery))

            lastIndex = matchResult.range.last + 1
        }

        // 3. Texto narrativo RESTANTE
        if (lastIndex < text.length) {
            segments.add(StorySegment.Text(text.substring(lastIndex).trim()))
        }

        return segments
    }
}