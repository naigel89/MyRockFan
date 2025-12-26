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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myrockfan.ui.theme.RockTypography
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent

// Punto de entrada de la aplicación Android.
// Configura el tema visual y delega el control a la navegación principal.
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

/**
 * Gestor de Navegación y Estado Global.
 *
 * Esta función actúa como el "cerebro" que decide qué pantalla mostrar basándose en el estado del usuario:
 * 1. ¿Está cargando las preferencias? -> Muestra Spinner.
 * 2. ¿Es la primera vez (Onboarding incompleto)? -> Muestra pantalla de selección de bandas.
 * 3. ¿El usuario pidió editar ajustes? -> Reutiliza la pantalla de Onboarding.
 * 4. ¿Todo listo? -> Muestra la pantalla principal de historias (StoryScreen).
 */
@Composable
fun MainAppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Instancia de persistencia de datos (DataStore) para recordar si el usuario ya entró.
    val userPreferences = remember { UserPreferences(context) }

    // Leemos si completó el onboarding
    val isOnboardingCompleted by userPreferences.isOnboardingCompleted.collectAsState(initial = null)

    // Leemos las bandas actuales (para poder editarlas)
    val currentBands by userPreferences.getSelectedBands.collectAsState(initial = emptySet())

    // Nuevo estado: ¿Estamos mostrando ajustes?
    var showSettings by remember { mutableStateOf(false) }

    when {
        // Estado 1: Incertidumbre inicial (DataStore aún no ha respondido). Evita parpadeos.
        isOnboardingCompleted == null -> {
            // Cargando inicial...
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFEF3868))
            }
        }
        // Estado 2: Flujo de bienvenida. El usuario debe elegir bandas por primera vez.
        isOnboardingCompleted == false -> {
            OnboardingScreen(
                initialSelection = emptySet(),
                onFinished = { selected ->
                    // Al terminar, guardamos y esto cambiará isOnboardingCompleted a true automáticamente.
                    scope.launch { userPreferences.saveBands(selected) }
                }
            )
        }
        // Estado 3: Modo Edición. El usuario ya existe pero quiere cambiar sus gustos.
        showSettings -> {
            OnboardingScreen(
                initialSelection = currentBands,
                onFinished = { newSelection ->
                    scope.launch {
                        userPreferences.saveBands(newSelection)
                        showSettings = false
                    }
                }
            )
        }
        // Estado 4: Flujo Principal. El usuario ya está configurado y ve la "Radio/Guitarra".
        else -> {
            StoryScreen(
                // Callback: Cuando en StoryScreen toquen el engranaje, activamos el modo ajustes aquí.
                onSettingsClick = { showSettings = true }
            )
        }
    }
}

/**
 * Componente visual para la cabecera de la historia.
 *
 * Utiliza una técnica de diseño de capas:
 * 1. Imagen de fondo (AsyncImage).
 * 2. Degradado vertical (Gradient) para asegurar que el texto blanco sea legible sobre cualquier foto.
 * 3. Textos y metadatos superpuestos.
 */
@Composable
fun StoryHeader(
    title: String,
    imageUrl: String?,
    category: String = "CURIOSIDADES",
    readTime: String = "3 min de lectura",
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(Color(0xFF2C2C2C))
    ) {
        // A. CAPA BASE: IMAGEN DE FONDO
        AsyncImage(
            model = imageUrl ?: "https://picsum.photos/800/1200",
            contentDescription = "Imagen de portada para la historia: $title",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // B. CAPA INTERMEDIA: DEGRADADO DE LEGIBILIDAD
        // Esencial para diseño UI: oscurece la parte inferior para que el texto resalte.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,       // Arriba: Se ve la foto pura.
                            Color(0xAA121212),       // Medio: Transición suave.
                            Color(0xFF121212)        // Abajo: Negro total para fusionarse con el cuerpo.
                        ),
                        startY = 200f // Empieza a oscurecer desde la mitad superior
                    )
                )
        )

        // C. CAPA SUPERIOR: INFORMACIÓN
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // 1. Etiqueta de Categoría (Estilo "Píldora")
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

            // 2. Título Principal
            Text(
                text = title.uppercase(), // Mayúsculas como en la revista
                style = RockTypography.displayLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 3. Metadatos (Tiempo de lectura)
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

        // D. BOTONES DE NAVEGACIÓN SUPERIOR
        // Se colocan en una fila separada en la parte superior.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmallCircleButton(
                icon = "←",
                onClick = onBackClick
            )

            SmallCircleButton(
                icon = "🔗",
                onClick = onShareClick
            )
        }
    }
}

/**
 * Componente UI reutilizable: Botón circular translúcido (efecto cristal).
 * Se usa para "Atrás", "Compartir" y "Ajustes".
 */
@Composable
fun SmallCircleButton(icon: String, onClick: () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = CircleShape,
        modifier = Modifier
            .size(44.dp)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(icon, color = Color.White, fontSize = 20.sp)
        }
    }
}

/**
 * Lógica de negocio para compartir:
 * Convierte la lista de segmentos (texto + imágenes) en un único String formateado
 * para que sea legible al enviarlo por WhatsApp/Telegram.
 */
fun buildFullStoryText(title: String, segments: List<StorySegment>): String {
    val sb = StringBuilder()

    // Formato Markdown simple para énfasis
    sb.append("*$title*\n\n")

    segments.forEach { segment ->
        when (segment) {
            is StorySegment.Text -> {
                sb.append(segment.content.trim())
                sb.append("\n\n")
            }
            // Aquí ignoramos las imágenes porque en texto plano no se pueden incrustar fácilmente,
            // pero podríamos añadir la URL si quisiéramos.
            else -> {}
        }
    }

    sb.append("🎸 _Generado por My Rock Fan App_")

    return sb.toString()
}

/**
 * Utilidad de Android para invocar el "Share Sheet" nativo del sistema.
 */
fun shareStory(context: Context, content: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        // Ponemos el texto completo generado
        putExtra(Intent.EXTRA_TEXT, content)
        type = "text/plain"
    }
    // Muestra el menú para elegir app (WhatsApp, Telegram, etc.)
    val shareIntent = Intent.createChooser(sendIntent, "Compartir historia completa con...")
    context.startActivity(shareIntent)
}

/**
 * Pantalla Principal del Flujo (Core).
 *
 * Maneja dos estados visuales principales superpuestos:
 * 1. "Idle" (Reposo): Muestra la guitarra interactiva y bienvenida.
 * 2. "Success" (Historia): Muestra el contenido generado y el reproductor de música.
 * 3. "Loading": Muestra la animación de carga (vinilo).
 */
@Composable
fun StoryScreen(
    viewModel: MainViewModel = viewModel(),
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Box actúa como un "FrameLaout", permitiendo apilar la guitarra sobre el fondo,
    // o el reproductor sobre el texto.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {

        // --- CAPA 1: CONTENIDO LÓGICO ---
        when (val state = uiState) {
            is StoryUiState.Loading -> {
                RockLoadingScreen()
            }
            // Caso: Historia generada correctamente
            is StoryUiState.Success -> {
                // Estrategia para la portada: Buscamos el primer segmento que sea una imagen válida.
                val coverImageSegment =
                    state.segments.find { it is StorySegment.ReadyImage } as? StorySegment.ReadyImage
                val coverUrl = coverImageSegment?.url

                // Efecto secundario: Si no hay un reproductor activo, iniciamos la "banda sonora"
                // usando metadatos de la historia actual.
                LaunchedEffect(state) {
                    if (!viewModel.showPlayer) {
                        viewModel.setRecommendedTrack(
                            title = "Greatest Hits",
                            artist = state.title
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {

                    // A. LISTA DE SCROLL (TEXTO E IMÁGENES)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Padding inferior CRÍTICO: asegura que el usuario pueda hacer scroll hasta el final
                        // sin que el MiniPlayer flotante tape el último párrafo.
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // El contenido se llena dinámicamente abajo..
                    }

                    // B. ELEMENTO FLOTANTE: MINI REPRODUCTOR
                    if (viewModel.showPlayer) {
                        MiniPlayer(
                            songTitle = viewModel.currentSongTitle,
                            artistName = viewModel.currentArtist,
                            coverUrl = null, // Usamos null para que use el icono por defecto o lógica interna
                            onPlayClick = {
                                // Redirección externa a Spotify
                                playOnSpotify(
                                    context,
                                    viewModel.currentArtist,
                                    viewModel.currentSongTitle
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter) // Pegado abajo
                                .padding(16.dp)
                        )
                    }
                }

                // Renderizado detallado de la lista (LazyColumn)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
                    // Ítem 0: La Cabecera (Header)
                    item {
                        StoryHeader(
                            title = state.title,
                            imageUrl = coverUrl,
                            category = viewModel.currentArtist,
                            onBackClick = {
                                viewModel.resetToGuitar()
                            },
                            onShareClick = {
                                val fullText = buildFullStoryText(state.title, state.segments)
                                shareStory(context, fullText)
                            }
                        )
                    }

                    // Ítems 1..N: Segmentos dinámicos (Párrafos o Fotos)
                    items(state.segments) { segment ->
                        // Filtramos la portada para no repetirla dentro del cuerpo del texto
                        if (segment == coverImageSegment) return@items

                        when (segment) {
                            is StorySegment.Text -> {
                                // AQUI SÍ ponemos padding, para que el texto respire
                                Text(
                                    // Parseo de Markdown (negritas) a estilos de Compose
                                    text = parseMarkdownToAnnotatedString(segment.content),
                                    style = RockTypography.bodyLarge,
                                    color = Color(0xFFE0E0E0),
                                    modifier = Modifier.padding(
                                        horizontal = 24.dp,
                                        vertical = 12.dp
                                    )
                                )
                            }

                            is StorySegment.ReadyImage -> {
                                // Renderizado de imágenes secundarias insertadas en el texto
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
                                // "Skeleton Loader": Un marcador de posición mientras la imagen carga
                                // Mantiene la estructura visual de la historia evitando saltos bruscos.
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

                            else -> {}
                        }
                    }

                    // Ítem Final: Branding / Pie de página
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

                // Reproductor persistente (se repite la lógica visual para asegurar z-index superior)
                if (viewModel.showPlayer) {
                    MiniPlayer(
                        songTitle = viewModel.currentSongTitle,
                        artistName = viewModel.currentArtist,
                        coverUrl = viewModel.currentCoverUrl ?: coverUrl, // Usamos la portada encontrada o la de la historia
                        onPlayClick = {
                            playOnSpotify(context, viewModel.currentArtist, viewModel.currentSongTitle)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }

            // Caso: Pantalla de Inicio (Reposo) o Error
            is StoryUiState.Idle, is StoryUiState.Error -> {
                val state = uiState

                // Botón de Ajustes (Engranaje) alineado arriba a la derecha
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    SmallCircleButton(
                        icon = "⚙️",
                        onClick = onSettingsClick
                    )
                }

                // Contenedor de Texto de Bienvenida
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp, start = 24.dp, end = 24.dp), // 1. Mover arriba
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top // 2. Alinear al principio
                ) {
                    // Feedback visual si hubo un error en la generación anterior
                    if (state is StoryUiState.Error) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color(0xFFEF5350),
                            style = RockTypography.labelSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Título de la App ("Welcome")
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

                    // Subtítulo explicativo / Call to Action narrativo
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

        // --- CAPA 2: INTERACCIÓN ESPECIAL (GUITARRA) ---
        // Overlay inferior que contiene la guitarra interactiva.
        // AnimatedVisibility gestiona la transición suave (Fade In/Out) para que la guitarra
        // no desaparezca de golpe cuando carga la historia.
        AnimatedVisibility(
            // Solo visible si NO estamos viendo una historia y NO está cargando.
            visible = uiState !is StoryUiState.Success && uiState !is StoryUiState.Loading,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(800)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF121212), Color(0xFF121212)),
                            startY = 0f
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Indicador visual para que el usuario sepa qué hacer
                    Text(
                        text = "RASGUEA PARA DESBLOQUEAR",
                        style = RockTypography.labelSmall.copy(
                            letterSpacing = 2.sp, // Espaciado elegante
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Elemento decorativo
                    Text(
                        text = "^",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // COMPONENTE DE GUITARRA (Lógica de Canvas y Gestos)
                    // Al rasguear, dispara viewModel.generateDailyCuriosity()
                    GuitarraTrigger(
                        onStrum = { viewModel.generateDailyCuriosity() },
                        isVisible = true
                    )
                }
            }
        }
    }
}
