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

    suspend fun searchImage(query: String): String? {
        return try {
            // CHIVATO 1: ¿Qué estamos buscando?
            android.util.Log.d("FOTO_DEBUG", "🚀 Buscando en Google: '$query'")

            val response = api.searchImages(BuildConfig.apiSearchKey, CX_ID, query)

            // CHIVATO 2: ¿Qué respondió Google?
            val items = response.items
            if (!items.isNullOrEmpty()) {
                val url = items[0].link
                android.util.Log.d("FOTO_DEBUG", "✅ FOTO ENCONTRADA: $url")
                url
            } else {
                android.util.Log.e("FOTO_DEBUG", "⚠️ Google devolvió 0 resultados. ¿Query muy larga?")
                null
            }

        } catch (e: Exception) {
            // CHIVATO 3: ¿Explotó?
            android.util.Log.e("FOTO_DEBUG", "❌ ERROR CRÍTICO API: ${e.message}")
            // Esto suele ser error 403 (Permisos) o 400 (Bad Request)
            e.printStackTrace()
            null
        }
    }
}