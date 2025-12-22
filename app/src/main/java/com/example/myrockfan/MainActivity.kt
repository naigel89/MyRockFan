package com.example.myrockfan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.* // Importante para Column, Row, Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.* // Importante para MaterialTheme, Text, Button, Card
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Importante para viewModel()
import com.example.myrockfan.ui.theme.RockTypography

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppNavigation()
            }
        }
    }
}

@Composable
fun MainAppNavigation() {
    val context = LocalContext.current
    // Recuerda que UserPreferences debe existir en tu proyecto
    val userPreferences = remember { UserPreferences(context) }

    val isOnboardingCompleted by userPreferences.isOnboardingCompleted.collectAsState(initial = null)

    when (isOnboardingCompleted) {
        null -> {
            // Cargando...
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        false -> {
            OnboardingScreen(onFinished = { })
        }
        true -> {
            StoryScreen()
        }
    }
}

@Composable
fun StoryHeader(
    title: String,
    imageUrl: String?,
    category: String = "CURIOSIDADES",
    readTime: String = "3 min de lectura"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(Color(0xFF2C2C2C))// Altura muy generosa (casi media pantalla)
    ) {
        // A. IMAGEN DE FONDO
        AsyncImage(
            model = imageUrl ?: "https://picsum.photos/800/1200", // Fallback por si acaso
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // B. EL DEGRADADO MÁGICO (Fusiona la foto con el negro)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,       // Arriba: Se ve la foto
                            Color(0xAA121212),       // Medio: Oscurece un poco
                            Color(0xFF121212)        // Abajo: Negro total (tu fondo)
                        ),
                        startY = 200f // Empieza a oscurecer desde la mitad superior
                    )
                )
        )

        // C. TEXTOS Y ETIQUETAS (Alineados abajo a la izquierda)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // 1. Píldora Morada (Categoría)
            Surface(
                color = Color(0xFF7D5260).copy(alpha = 0.8f), // Tono rojizo/morado oscuro elegante
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text(
                    text = category.uppercase(),
                    style = RockTypography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 2. Título Serif Impactante
            Text(
                text = title.uppercase(), // Mayúsculas como en la revista
                style = RockTypography.displayLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 3. Metadatos (Reloj)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏱️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = readTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // D. BOTONES FLOTANTES (Atrás y Compartir)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Botón Atrás (Simulado)
            SmallCircleButton(icon = "←")
            // Botón Compartir (Simulado)
            SmallCircleButton(icon = "🔗")
        }
    }
}

// Botoncito auxiliar redondo
@Composable
fun SmallCircleButton(icon: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = CircleShape,
        modifier = Modifier.size(44.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(icon, color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
fun StoryScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Usamos BOX para poder superponer elementos (Fondo, Contenido, Guitarra)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Fondo Negro Rock Global
    ) {

        // 1. CAPA DE CONTENIDO (La historia o el estado)
        when (val state = uiState) {
            is StoryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFEF3868)) // Rosa Hot
                }
            }

            is StoryUiState.Success -> {
                // LOGICA INTELIGENTE: Buscamos la portada
                val coverImageSegment =
                    state.segments.find { it is StorySegment.ReadyImage } as? StorySegment.ReadyImage
                val coverUrl = coverImageSegment?.url

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp) // Espacio final para que la guitarra no tape el final
                ) {
                    // A. CABECERA (Portada) - SIN PADDING para que ocupe todo el ancho
                    item {
                        StoryHeader(
                            title = cleanTitle(state.title),
                            imageUrl = coverUrl
                        )
                    }

                    // B. CUERPO DE LA HISTORIA
                    items(state.segments) { segment ->
                        // No repetimos la portada
                        if (segment == coverImageSegment) return@items

                        when (segment) {
                            is StorySegment.Text -> {
                                // AQUI SÍ ponemos padding, para que el texto respire
                                Text(
                                    text = parseMarkdownToAnnotatedString(segment.content),
                                    style = RockTypography.bodyLarge, // Asegúrate que en Type.kt bodyLarge tenga color blanco/gris
                                    color = Color(0xFFE0E0E0), // Forzamos color claro por si acaso
                                    modifier = Modifier.padding(
                                        horizontal = 24.dp,
                                        vertical = 12.dp
                                    )
                                )
                            }

                            is StorySegment.ReadyImage -> {
                                // Fotos secundarias
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    AsyncImage(
                                        model = segment.url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            is StorySegment.ImagePrompt -> {
                                // SKELETON: Un recuadro gris sutil que preserva el espacio visual
                                // Si la imagen falla, al menos se ve estructura y no un error.
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp) // Misma altura que las fotos reales para no dar saltos
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF1E1E1E) // Un gris un poco más claro que el fondo negro
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Un indicador de carga muy sutil y oscuro
                                        CircularProgressIndicator(
                                            color = Color(0xFF2C2C2C),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // C. FIRMA FINAL
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                        Text(
                            "📻 MY ROCK FAN · BACKSTAGE PASS",
                            style = RockTypography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 30.dp)
                        )
                    }
                }
            }

            is StoryUiState.Idle, is StoryUiState.Error -> {
                // ESTADO INICIAL (Pantalla de Bienvenida)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp, start = 24.dp, end = 24.dp), // 1. Mover arriba
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top // 2. Alinear al principio
                ) {
                    // Si hay error, lo mostramos discreto
                    if (state is StoryUiState.Error) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color(0xFFEF5350),
                            style = RockTypography.labelSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // TÍTULO PRINCIPAL (Estilo Revista)
                    Text(
                        text = "BIENVENIDO AL\nBACKSTAGE",
                        style = RockTypography.displayLarge.copy(
                            fontSize = 42.sp, // Un poco más grande para impactar
                            lineHeight = 44.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // SUBTÍTULO
                    Text(
                        text = "La historia del Rock no se lee,\nse invoca. Rasguea las cuerdas para\ndesenterrar una leyenda.",
                        style = RockTypography.bodyLarge.copy(
                            color = Color(0xFFB0B0B0),
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. CAPA DE LA GUITARRA (Overlay)
        // Usamos AnimatedVisibility para que desaparezca suavemente al tener éxito
        AnimatedVisibility(
            visible = uiState !is StoryUiState.Success && uiState !is StoryUiState.Loading,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(800)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp) // Altura generosa para la guitarra
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF121212), Color(0xFF121212)),
                            startY = 0f
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // TEXTO DE INSTRUCCIÓN (Más sutil y elegante)
                    Text(
                        text = "RASGUEA PARA DESBLOQUEAR",
                        style = RockTypography.labelSmall.copy(
                            letterSpacing = 2.sp, // Espaciado elegante
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Icono o flechita (Opcional)
                    Text(
                        text = "^",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // TU GUITARRA NUEVA
                    GuitarraTrigger(
                        onStrum = { viewModel.generateDailyCuriosity() },
                        isVisible = true
                    )
                }
            }
        }
    }
}
