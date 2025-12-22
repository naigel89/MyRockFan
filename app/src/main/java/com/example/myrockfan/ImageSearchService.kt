package com.example.myrockfan

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Definimos cómo es la respuesta de Google (solo nos importa el link)
data class SearchResponse(val items: List<SearchItem>?)
data class SearchItem(val link: String)

// 2. Definimos la interfaz de la API
interface GoogleSearchApi {
    @GET("customsearch/v1")
    suspend fun searchImages(
        @Query("key") apiKey: String,
        @Query("cx") cx: String,
        @Query("q") query: String,
        @Query("searchType") searchType: String = "image",
        @Query("num") num: Int = 1 // Solo queremos 1 foto
    ): SearchResponse
}

// 3. Objeto singleton para usarlo desde cualquier lado
object ImageRepository {
    private const val BASE_URL = "https://www.googleapis.com/"

    // ¡¡PEGAR TU CX ID AQUÍ!!
    private const val CX_ID = "a62990b1356df43a1"

    private val api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleSearchApi::class.java)

    suspend fun searchImage(originalQuery: String): String? {
        return try {
            // 1. MEJORA DE QUERY: Añadimos filtros negativos para evitar redes sociales
            // Esto le dice a Google: "Busca esto, pero NO en tiktok, instagram, etc"
            val forbiddenSites = "-site:tiktok.com -site:instagram.com -site:facebook.com -site:pinterest.com -site:youtube.com"
            val refinedQuery = "$originalQuery $forbiddenSites"

            // CHIVATO 1: ¿Qué estamos buscando? (Ahora mostramos la query refinada)
            android.util.Log.d("FOTO_DEBUG", "🚀 Buscando en Google: '$refinedQuery'")

            // Asumimos que tu llamada api.searchImages devuelve por defecto 10 resultados
            val response = api.searchImages(BuildConfig.apiSearchKey, CX_ID, refinedQuery)
            val items = response.items

            // CHIVATO 2: ¿Qué respondió Google?
            if (!items.isNullOrEmpty()) {

                // 2. FILTRO DE CALIDAD (Kotlin)
                // En lugar de coger items[0] directo, buscamos el PRIMERO que cumpla las reglas.
                val validItem = items.firstOrNull { item ->
                    val link = item.link?.lowercase() ?: ""

                    // Regla A: Tiene que ser un archivo de imagen real
                    val isRealFile = link.contains(".jpg") ||
                            link.contains(".jpeg") ||
                            link.contains(".png") ||
                            link.contains(".webp")

                    // Regla B: Filtro de seguridad extra (por si Google cuela algo)
                    val isNotSocial = !link.contains("tiktok.com") && !link.contains("instagram.com")

                    isRealFile && isNotSocial
                }

                if (validItem != null) {
                    val url = validItem.link
                    android.util.Log.d("FOTO_DEBUG", "✅ FOTO ENCONTRADA y VALIDADA: $url")
                    url
                } else {
                    android.util.Log.w("FOTO_DEBUG", "⚠️ Google trajo resultados, pero ninguno era un archivo de imagen válido (.jpg/.png).")
                    null
                }

            } else {
                android.util.Log.e("FOTO_DEBUG", "⚠️ Google devolvió 0 resultados. ¿Query muy larga o muy restrictiva?")
                null
            }

        } catch (e: Exception) {
            // CHIVATO 3: ¿Explotó?
            android.util.Log.e("FOTO_DEBUG", "❌ ERROR CRÍTICO API: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}