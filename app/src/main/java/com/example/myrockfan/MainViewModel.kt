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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.Intent

/**
 * El Cerebro de la Aplicación (ViewModel).
 *
 * Responsabilidades principales:
 * 1. Orquestar la IA: Configura y comunica con Gemini.
 * 2. Gestión de Estado: Mantiene la UI informada (Cargando, Éxito, Error).
 * 3. Lógica de Negocio: Selecciona bandas, temas aleatorios y procesa la respuesta raw de la IA.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)

    // ESTADO DEL MINI-REPRODUCTOR (State Hoisting)
    // Mantenemos estos estados aquí para que sobrevivan a cambios de configuración
    // y para que la UI pueda reaccionar instantáneamente.
    var currentSongTitle by mutableStateOf("Seleccionando tema...")
    var currentArtist by mutableStateOf("My Rock Fan Radio")
    var currentCoverUrl by mutableStateOf<String?>(null) // --- NUEVO: Para la portada del album
    var showPlayer by mutableStateOf(false)

    // --- CONFIGURACIÓN DEL CEREBRO (GEMINI) ---
    // Aquí definimos la "Personalidad" de la IA. No es una simple query,
    // es un "System Prompt" complejo que define tono, formato y reglas estrictas.
    private val generativeModel = GenerativeModel(
        // Modelo experimental de baja latencia. Sigue funcionando correctamente usando otros modelos
        // Otros modelos probados: gemini-2.5-flash, gemini-2.5-flash-lite
        modelName = "gemini-3-flash-preview",
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
               - Debe ser espectacular y atmosférica.
               - Describe el LUGAR o la VIBRA.
               - AÑADE SIEMPRE al final: "wide shot wallpaper cinematic 4k"
               - EJEMPLO: [[FOTO: live stage lights fog wide shot wallpaper cinematic]] 
               (Fíjate que NO pongo "Pink Floyd" en el ejemplo, solo la escena)
               
               >>> IMÁGENES DEL CUERPO:
               - EJEMPLO: [[FOTO: recording studio microphone close up vintage]]
               - EJEMPLO: [[FOTO: fender stratocaster guitar black white background]]
               
               // --- REGLA DE ORO MUSICAL (NUEVO) ---
                Al FINAL ABSOLUTO de tu respuesta, debes recomendar la canción perfecta para cerrar esta historia.
                Debes usar ESTRICTAMENTE este formato para que la app pueda leerlo:
                
                [[CANCION: Título de la Canción | Nombre del Artista]]
                El acorde que cambió la historia
                
                Buenas noches a los despiertos. Hoy no vamos a hablar de fama, sino de un error técnico que se convirtió en leyenda...
                
                [[FOTO: recording studio microphone close up vintage]]
                
                Todo ocurrió en el salón de la casa de los Davies. **Dave Davies**, harto del sonido limpio de la época, tomó una cuchilla de afeitar...
                
                (Historia desarrollada con contexto del productor y el estudio...)
                
                Para cerrar esta noche eléctrica, nada mejor que escuchar el resultado de esa cuchilla: "You Really Got Me".
                
                [[CANCION: You Really Got Me | The Kinks]]
            """.trimIndent())
        }
    )

    // Estado Reactivo (Flow): La única fuente de verdad para la UI.
    private val _uiState = MutableStateFlow<StoryUiState>(StoryUiState.Idle)
    val uiState: StateFlow<StoryUiState> = _uiState

    // Reseteo simple para volver al estado inicial (Guitarra).
    fun resetToGuitar() {
        _uiState.value = StoryUiState.Idle
        showPlayer = false // Ocultamos el player al volver
    }

    /**
     * EL CORAZÓN DE LA APP.
     * Ejecuta el pipeline completo de generación:
     * 1. Selección de contexto (Banda + Tema).
     * 2. Generación de texto (Gemini).
     * 3. Extracción de metadatos (Canción).
     * 4. Hidratación de imágenes (Google Search en paralelo).
     */
    fun generateDailyCuriosity() {
        viewModelScope.launch {
            _uiState.value = StoryUiState.Loading // Dispara la pantalla de carga (Vinilo)
            showPlayer = false

            try {
                // PASO 1: CONTEXTO
                // Recuperamos las preferencias del usuario de forma asíncrona.
                val bands = userPreferences.getSelectedBands.first()
                if (bands.isEmpty()) {
                    _uiState.value = StoryUiState.Error("No has seleccionado bandas favoritas.")
                    return@launch
                }
                val randomBand = bands.random()

                // "Ingeniería de Caos Controlado":
                // Forzamos a la IA a salir de la biografía estándar inyectando temas dramáticos aleatorios.
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

                val randomTheme = themes.random()

                // Construcción del Prompt de Usuario (Dynamic Prompting)
                // Inyectamos las variables seleccionadas en la plantilla.
                val userPrompt = """
                    Tu misión es contar una historia sobre $randomBand.
                    
                    ENFOQUE OBLIGATORIO:
                    Quiero que busques y narres una anécdota específica relacionada con: "$randomTheme".
                    
                    REGLAS:
                    1. Si no encuentras una historia exacta con ese tema, busca la curiosidad más cercana o impactante disponible, pero mantén el tono.
                    2. Ignora la biografía general (fechas de nacimiento, discografía completa). Ve al grano de la anécdota.
                    3. Contextualiza: ¿Dónde estaban? ¿Qué año era? ¿Qué se jugaban?
                """.trimIndent()

                // PASO 2: LLAMADA A LA IA
                // Esto puede tardar unos segundos, por eso mostramos el Vinilo (LoadingComponents)
                val response = generativeModel.generateContent(userPrompt)
                var fullText = response.text ?: ""

                // PASO 3: EXTRACCIÓN DE METADATOS (Regex)
                // Buscamos la "etiqueta oculta" de la canción que le pedimos a la IA.
                val songRegex = """\[{1,2}(?i)CANci[oó]n:\s*(.*?)\s*\|\s*(.*?)]{1,2}""".toRegex()
                val songMatch = songRegex.find(fullText)

                if (songMatch != null) {
                    val trackName = songMatch.groupValues[1].trim()
                    val artistName = songMatch.groupValues[2].trim()

                    // Configuramos el player inmediatamente
                    setRecommendedTrack(trackName, artistName)

                    // Lanzamos una búsqueda en segundo plano para la portada del MiniPlayer.
                    // No bloqueamos el hilo principal esperando esto.
                    launch(Dispatchers.IO) {
                        try {
                            val albumQuery = "Album cover art $trackName $artistName high quality"
                            val coverFound = ImageRepository.searchImage(albumQuery)
                            currentCoverUrl = coverFound
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Limpiamos el texto para que la etiqueta técnica no se muestre al usuario.
                    fullText = fullText.replace(songMatch.value, "").trim()
                } else {
                    // Fallback (Plan B): Si la IA alucina y olvida la etiqueta, ponemos lo básico.
                    setRecommendedTrack("Greatest Hits", randomBand)
                }

                // PASO 4: PARSEO Y ESTRUCTURACIÓN
                // Separamos el título del cuerpo y dividimos el cuerpo en bloques.
                val lines = fullText.split("\n").filter { it.isNotBlank() }
                val titleIndex = lines.indexOfFirst { !it.trim().startsWith("[[") } // El título no es una foto

                val title = if (titleIndex != -1) {
                    // Limpiamos los "#" solo para la cabecera
                    lines[titleIndex].replace("#", "").trim()
                } else {
                    "Rock Story"
                }

                // Generamos el cuerpo EXCLUYENDO la línea del título para que no salga repetida
                val bodyText = lines.filterIndexed { index, _ -> index != titleIndex }
                    .joinToString("\n")

                val initialSegments = parseResponseToSegments(bodyText)

                // PASO 5: HIDRATACIÓN PARALELA (OPTIMIZACIÓN CLAVE)
                // Transformamos los "Prompts de Imagen" en "URLs Reales".
                // Usamos 'async' para que si hay 3 fotos, las 3 se busquen A LA VEZ,
                // reduciendo el tiempo de espera total al de la petición más lenta.
                val finalSegments = initialSegments.map { segment ->
                    async(Dispatchers.IO) { // <--- ESTO CREA LA CORRUTINA PARA CADA IMAGEN
                        if (segment is StorySegment.ImagePrompt) {
                            var url: String? = null

                            // Intento A: Google Custom Search con contexto enriquecido
                            try {
                                url = ImageRepository.searchImage(
                                    originalQuery = segment.query,
                                    contextKeywords = randomBand
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // Intento B: Fallback visual (Picsum) si Google falla o se agota la cuota.
                            if (url.isNullOrBlank()) {
                                // Usamos hashcode para que la imagen aleatoria sea consistente (siempre la misma para el mismo texto)
                                val seed = segment.query.hashCode()
                                url = "https://picsum.photos/seed/$seed/800/1200"
                            }

                            // Transformamos el Prompt abstracto en una Imagen Lista para renderizar
                            StorySegment.ReadyImage(url!!, segment.query)
                        } else {
                            // Los segmentos de texto pasan sin cambios
                            segment
                        }
                    }
                }.awaitAll() // Esperamos a que todas las corrutinas paralelas terminen

                // PASO 6: ACTUALIZACIÓN DE UI
                // Todo listo, cambiamos el estado a Success para mostrar la historia.
                _uiState.value = StoryUiState.Success(title, finalSegments)

            } catch (e: Exception) {
                _uiState.value = StoryUiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    // Helper para actualizar estado del reproductor
    fun setRecommendedTrack(title: String, artist: String) {
        currentSongTitle = title
        currentArtist = artist
        currentCoverUrl = null
        showPlayer = true
    }

    /**
     * Motor de análisis sintáctico.
     * Escanea el texto generado por la IA buscando las etiquetas [[FOTO:...]].
     * Divide el string en una lista secuencial de segmentos TEXTO -> FOTO -> TEXTO.
     */
    private fun parseResponseToSegments(text: String): List<StorySegment> {
        val segments = mutableListOf<StorySegment>()
        val regex = """\[\[(?i)FOTO:\s*(.*?)]]""".toRegex(RegexOption.DOT_MATCHES_ALL)
        var lastIndex = 0

        regex.findAll(text).forEach { matchResult ->
            // 1. Capturar todo el texto que hay ANTES de la foto actual
            val textBefore = text.substring(lastIndex, matchResult.range.first).trim()
            if (textBefore.isNotEmpty()) {
                segments.add(StorySegment.Text(textBefore))
            }

            // 2. Extraer el contenido de la etiqueta (el prompt visual para Google)
            val rawQuery = matchResult.groupValues[1].trim().replace("\n", " ")
            segments.add(StorySegment.ImagePrompt(rawQuery))

            lastIndex = matchResult.range.last + 1
        }

        // 3. Capturar el texto restante DESPUÉS de la última foto
        if (lastIndex < text.length) {
            segments.add(StorySegment.Text(text.substring(lastIndex).trim()))
        }

        return segments
    }
}