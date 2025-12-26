package com.example.myrockfan

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Modelos de datos anémicos para mapear la respuesta JSON.
// Capturamos 'title' y 'snippet' específicamente para validar el contexto semántico de la imagen.
data class SearchResponse(val items: List<SearchItem>?)
data class SearchItem(
    val link: String,
    val title: String?,   // <--- NUEVO: Necesitamos leer el título
    val snippet: String?  // <--- NUEVO: A veces la info está aquí
)

interface GoogleSearchApi {
    @GET("customsearch/v1")
    suspend fun searchImages(
        @Query("key") apiKey: String,
        @Query("cx") cx: String,
        @Query("q") query: String,
        @Query("searchType") searchType: String = "image",
        // ESTRATEGIA DE VOLUMEN:
        // Pedimos 10 resultados (en lugar de 1) para alimentar nuestro algoritmo de filtrado local.
        // Si el primero es malo (ej: TikTok), tendremos 9 candidatos más en la recámara.
        @Query("num") num: Int = 10,
        @Query("imgSize") imgSize: String = "xlarge",
        @Query("safe") safe: String = "active"
    ): SearchResponse
}

object ImageRepository {
    private const val BASE_URL = "https://www.googleapis.com/"
    private const val CX_ID = BuildConfig.cxId

    private val api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleSearchApi::class.java)

    // FILTRADO NEGATIVO (Noise Reduction):
    // Esta lista negra elimina dominios que:
    // 1. Tienen alto SEO pero imágenes irrelevantes (Pinterest, Amazon).
    // 2. Bloquean el hotlinking (Instagram, Facebook -> Error 403).
    // 3. Venden fotos con marcas de agua (Getty, Shutterstock).
    private val forbiddenSites = """
        -site:tiktok.com -site:instagram.com -site:facebook.com 
        -site:pinterest.com -site:youtube.com -site:amazon.com 
        -site:ebay.com -site:gettyimages.com -site:alamy.com 
        -site:vectorstock.com -site:shutterstock.com -site:lookaside.fbsbx.com
        -site:media-amazon.com -site:gstatic.com -site:stock.adobe.com
        -site:etsy.com -site:mercadolibre.com -site:wallapop.com
    """.trimIndent().replace("\n", " ")

    /**
     * Orquestador principal de la búsqueda.
     * Implementa un patrón de "Doble Intento" (Two-Tier Strategy) para equilibrar precisión y disponibilidad.
     */
    suspend fun searchImage(originalQuery: String, contextKeywords: String = ""): String? {
        // TIER 1: PRECISIÓN QUIRÚRGICA
        // Buscamos la escena exacta que narró la IA (ej: "grabación estudio").
        // Aplicamos validación estricta: el título debe contener TODAS las palabras clave de la banda.
        val specificUrl = internalSearch(
            query = "$contextKeywords $originalQuery $forbiddenSites",
            requiredKeywords = contextKeywords,
            isStrict = true
        )

        if (specificUrl != null) {
            return specificUrl
        }

        // TIER 2: RED DE SEGURIDAD (FALLBACK)
        // Si la búsqueda específica falló (demasiado restrictiva o sin resultados),
        // sacrificamos precisión contextual para garantizar que al menos mostramos a la banda correcta.
        // Buscamos algo genérico ("wallpaper") y relajamos la validación (isStrict = false).
        android.util.Log.w("FOTO_DEBUG", "⚠️ Escena específica no encontrada. Activando PLAN B (Genérico).")

        val genericUrl = internalSearch(
            query = "$contextKeywords band wallpaper live concert rock high quality $forbiddenSites",
            requiredKeywords = contextKeywords,
            isStrict = false
        )

        return genericUrl
    }

    /**
     * Motor de búsqueda y validación lógica.
     * Aquí reside el algoritmo "Portero de Discoteca" que decide si una imagen es digna de mostrarse.
     */
    private suspend fun internalSearch(query: String, requiredKeywords: String, isStrict: Boolean): String? {
        return try {
            android.util.Log.d("FOTO_DEBUG", "🔎 Buscando ($isStrict): '$query'")

            val response = api.searchImages(BuildConfig.apiSearchKey, CX_ID, query)
            val items = response.items

            if (items.isNullOrEmpty()) return null

            // HEURÍSTICA DE SELECCIÓN:
            // Iteramos sobre los candidatos y nos quedamos con el PRIMERO que cumpla todas las reglas.
            val validItem = items.firstOrNull { item ->
                // Normalización: Eliminamos acentos y mayúsculas para comparar (Bogotá == bogota).
                val link = item.link?.normalize() ?: ""
                val title = (item.title ?: "").normalize()
                val snippet = (item.snippet ?: "").normalize()
                val bandNameClean = requiredKeywords.normalize()

                // REGLA 1: HIGIENE TÉCNICA
                // - Debe ser un archivo de imagen estático.
                // - Evitamos URLs "sucias" (con query params '?' o muy largas) que suelen ser redirecciones o fallar en Coil.
                val isImage = link.endsWith(".jpg") || link.endsWith(".jpeg") || link.endsWith(".png") || link.endsWith(".webp")
                val isCleanUrl = link.length < 400 && !link.contains("?")

                if (!isImage || !isCleanUrl) return@firstOrNull false

                // REGLA 2: VALIDACIÓN DE IDENTIDAD SEMÁNTICA
                if (bandNameClean.isNotEmpty()) {
                    // Tokenizamos el nombre de la banda para buscar coincidencias parciales.
                    // Filtramos artículos/conectores cortos (< 2 chars) para evitar falsos positivos con "The", "El".
                    val nameParts = bandNameClean.split(" ").filter { it.length > 2 }

                    // Verificamos presencia en metadatos (Título, Snippet o la propia URL)
                    val matches = nameParts.count { part ->
                        title.contains(part) || snippet.contains(part) || link.contains(part)
                    }

                    if (isStrict) {
                        // MODO ESTRICTO (Tier 1):
                        // Exigimos coincidencia TOTAL. Si buscamos "Arde Bogotá", deben aparecer "arde" Y "bogota".
                        // Esto evita que "Bogotá Music Festival" salga cuando buscamos a la banda.
                        matches == nameParts.size
                    } else {
                        // MODO LAXO (Tier 2):
                        // Aceptamos coincidencia PARCIAL. Útil para nombres largos o complejos.
                        // Garantiza que al menos hay una relación fuerte con la búsqueda.
                        matches >= 1
                    }
                } else {
                    true // Si no hay contexto (búsqueda libre), confiamos en Google.
                }
            }

            validItem?.link

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Utilidad de normalización de cadenas.
 * Crucial para bandas hispanas o con caracteres especiales (Mötley Crüe, Arde Bogotá).
 * Convierte todo a ASCII básico lowercase para comparaciones robustas.
 */
fun String.normalize(): String {
    var result = this.lowercase()
    result = result.replace("á", "a")
    result = result.replace("é", "e")
    result = result.replace("í", "i")
    result = result.replace("ó", "o")
    result = result.replace("ú", "u")
    result = result.replace("ñ", "n")
    return result
}