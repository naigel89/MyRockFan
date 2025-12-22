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
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.apiKey,
        systemInstruction = content {
            text("""
                // --- PERSONALIDAD (EL ALMA DEL PROGRAMA) ---
                Eres el "Cronista de Radio Especializado", esa voz calmada y experta que acompaña al oyente en la madrugada.
                Tu tono es: Inmersivo, auténtico, culto y cercano. No eres un presentador estridente; eres un compañero que revela secretos.
                Tu misión: No contar biografías, sino narrar "El Momento". La historia detrás de la grabación, el accidente que creó un sonido, la tensión en el estudio.
                
                // --- PROFUNDIDAD NARRATIVA Y REGLAS DE ORO ---
                Para lograr la atmósfera deseada, sigue estas directrices:
                
                1. FOCO LÁSER: Elige UN solo arco argumental o anécdota específica. No hagas resúmenes de carrera.
                2. CONTEXTUALIZACIÓN VITAL: Cuando aparezca un personaje secundario (productor, ingeniero, pareja), explica brevemente QUIÉN es y POR QUÉ importa en esta historia. Dales vida.
                3. AMBIENTACIÓN REALISTA: Describe la escena con datos sensoriales. El sonido de los amplificadores, el humo, la acústica de la sala, las miradas. Haz que el lector sienta que está allí.
                4. EXTENSIÓN: Desarrolla la historia con calma. Mínimo 3 o 4 párrafos densos y ricos en matices entre foto y foto.
                5. CIERRE MUSICAL: Termina siempre sugiriendo una canción o videoclip específico que cierre el círculo de la historia contada.
                
                // --- FORMATO TÉCNICO (LA LEY - NO TOCAR) ---
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
               - Intenta poner entre unas 3-5 fotos relacionadas con el artista/grupo y la historia que se cuenta
               - EJEMPLO CUERPO: [[FOTO: David Gilmour fender stratocaster black close up]]
               
                // --- EJEMPLO DE RESPUESTA PERFECTA ---
                [[FOTO: Query de Portada (Wide/Wallpaper)]]
                
                El acorde que cambió la historia
                
                Buenas noches a los despiertos. Hoy no vamos a hablar de fama, sino de un error técnico que se convirtió en leyenda...
                
                [[FOTO: Dave Davies Kinks amplifier razor blade 1964]]
                
                Todo ocurrió en el salón de la casa de los Davies. **Dave Davies**, harto del sonido limpio de la época, tomó una cuchilla de afeitar...
                
                (Historia desarrollada con contexto del productor y el estudio...)
                
                Para cerrar esta noche eléctrica, nada mejor que escuchar el resultado de esa cuchilla: "You Really Got Me".
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

                val themes = listOf(
                    // --- CONFLICTO Y DRAMA ---
                    "una pelea intensa o tensión insoportable durante la grabación (Guerra Civil)",
                    "una canción que la discográfica odiaba y quería censurar o descartar",
                    "la salida dramática o despido de un miembro clave de la banda",
                    "un triángulo amoroso o ruptura sentimental que inspiró el álbum",
                    "una rivalidad pública o 'beef' con otra banda de la época",
                    "un encuentro con la policía, arresto o problema legal absurdo",

                    // --- MAGIA DE ESTUDIO (TÉCNICA) ---
                    "un error técnico o ruido accidental que se dejó en la mezcla final (Happy Accident)",
                    "una técnica de grabación extraña, innovadora o casera (Ingeniería Imposible)",
                    "un instrumento raro, prestado o comprado barato que definió el sonido",
                    "la intervención decisiva de un productor que cambió el rumbo de una canción",
                    "una sesión de grabación en un lugar insólito (castillo, baño, pasillo)",

                    // --- ORIGENES E HISTORIAS OCULTAS ---
                    "la historia real y poco conocida detrás de la portada del álbum",
                    "el destinatario secreto (musa u odiado) de una letra famosa",
                    "una canción escrita en cuestión de minutos en un momento de inspiración súbita",
                    "una canción que nació siendo una broma o parodia y se volvió un himno",
                    "el origen curioso del nombre de la banda o de un apodo",
                    "una demo perdida que fue recuperada años después",

                    // --- EN VIVO Y CAOS ---
                    "un concierto desastroso donde todo salió mal (técnica o públicamente)",
                    "una exigencia excéntrica o absurda en el backstage (Rider)",
                    "el rodaje de un videoclip que fue peligroso o caótico",
                    "la historia de su primer concierto lamentable o su último concierto emotivo",
                    "un telonero que casi eclipsa a la banda principal"
                )

                // Selección aleatoria
                val randomTheme = themes.random()

                // Construcción del Prompt
                val userPrompt = """
                    Tu misión es contar una historia sobre $randomBand.
                    
                    ENFOQUE OBLIGATORIO:
                    Quiero que busques y narres una anécdota específica relacionada con: "$randomTheme".
                    
                    REGLAS:
                    1. Si no encuentras una historia exacta con ese tema, busca la curiosidad más cercana o impactante disponible, pero mantén el tono.
                    2. Ignora la biografía general (fechas de nacimiento, discografía completa). Ve al grano de la anécdota.
                    3. Contextualiza: ¿Dónde estaban? ¿Qué año era? ¿Qué se jugaban?
                """.trimIndent()

                val response = generativeModel.generateContent(userPrompt)
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