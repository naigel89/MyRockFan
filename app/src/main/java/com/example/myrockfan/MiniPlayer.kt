package com.example.myrockfan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Componente "Floating Action Player".
 *
 * No es un reproductor real que procese audio, sino un "Deep Link enriquecido".
 * Su objetivo es conectar la narrativa (la historia que acabas de leer) con la experiencia auditiva,
 * sirviendo como puente hacia la aplicación de música externa (Spotify).
 */
@Composable
fun MiniPlayer(
    songTitle: String,
    artistName: String,
    coverUrl: String?,
    onPlayClick: () -> Unit, // Callback para lanzar el Intent de Android
    modifier: Modifier = Modifier
) {
    // Contenedor Flotante:
    // Diseñado con elevación (sombra) y color gris oscuro para destacar sobre el fondo negro de la app,
    // imitando la estética nativa de los "Now Playing" bars de apps de streaming.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp) // Altura estándar táctil cómoda
            .padding(8.dp)
            // UX: Hacemos clickable toda la tarjeta (Big Touch Target), no solo el botón pequeño,
            // para facilitar la acción con una sola mano.
            .clickable { onPlayClick() },
        color = Color(0xFF282828), // Gris oscuro (Surface color estándar en Dark Mode)
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp // Elevación para separarlo visualmente del contenido de la lista
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. IDENTIDAD VISUAL (PORTADA)
            // Carga asíncrona de la carátula. Si falla o es null,
            // Coil muestra automáticamente el placeholder definido (Picsum) para no romper el layout.
            AsyncImage(
                model = coverUrl ?: "https://picsum.photos/200",
                contentDescription = "Portada",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp)) // Suavizado de bordes para consistencia estética
            )

            // 2. METADATOS (TÍTULO Y ARTISTA)
            // Usamos una columna con peso (weight) para ocupar todo el espacio disponible
            // entre la foto y el botón de play.
            Column(
                modifier = Modifier
                    .weight(1f) // Gestión de espacio flexible
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Jerarquía Visual 1: El Título (Blanco, Negrita)
                Text(
                    text = songTitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // Truncamos si es muy largo para no deformar la tarjeta
                    overflow = TextOverflow.Ellipsis
                )
                // Jerarquía Visual 2: El Artista (Gris, Pequeño)
                Text(
                    text = artistName,
                    color = Color(0xFFB3B3B3), // Color específico "Spotify Gray" para subtítulos
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. CALL TO ACTION (CTA)
            // Botón explícito para indicar interactividad.
            // Usamos el color verde marca de Spotify para indicar subconscientemente
            // que la acción nos llevará a esa plataforma.
            IconButton(onClick = onPlayClick) {
                Text(
                    text = "▶",
                    fontSize = 28.sp,
                    color = Color(0xFF1DB954) // Brand Color: Spotify Green
                )
            }
        }
    }
}