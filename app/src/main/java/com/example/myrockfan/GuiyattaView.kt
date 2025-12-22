package com.example.myrockfan

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

// --- PALETA DE COLORES (Madera y cuerdas) ---
val WoodDark = Color(0xFF1A0F0A) // Casi negro
val WoodReddish = Color(0xFF4E2A1A) // Caoba
val BronzeDark = Color(0xFF8B5A2B) // Cuerda grave sombra
val BronzeLight = Color(0xFFE6C68C) // Cuerda grave brillo
val SteelDark = Color(0xFF555555) // Cuerda aguda sombra
val SteelLight = Color(0xFFDDDDDD) // Cuerda aguda brillo

@Composable
fun GuitarraTrigger(
    modifier: Modifier = Modifier,
    onStrum: () -> Unit,
    isVisible: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- SONIDO (Punto 1: Recuperado) ---
    val mediaPlayer = remember {
        try { MediaPlayer.create(context, R.raw.guitar_strum) } catch (e: Exception) { null }
    }

    // Aseguramos liberar memoria cuando el componente muere
    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    // --- FÍSICA ---
    var touchY by remember { mutableFloatStateOf(0f) }
    // 6 Cuerdas (Punto 2)
    val stringDeformations = remember { List(6) { Animatable(0f) } }
    var hasTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp)
            .pointerInput(isVisible) {
                if (isVisible) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            hasTriggered = false
                            touchY = offset.y
                        },
                        onDragEnd = {
                            // Rebote elástico al soltar
                            stringDeformations.forEach { anim ->
                                scope.launch {
                                    anim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioHighBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            touchY = change.position.y

                            // LÓGICA DE INTERACCIÓN
                            val width = size.width
                            val totalGuitarWidth = width * 0.85f // Ocupa un poco más de ancho
                            val startX = (width - totalGuitarWidth) / 2
                            val stringSpacing = totalGuitarWidth / 5 // Espacio entre 6 cuerdas

                            // Calculamos qué cuerdas se ven afectadas por el dedo
                            stringDeformations.forEachIndexed { index, anim ->
                                val stringX = startX + (stringSpacing * index)
                                val fingerX = change.position.x
                                val dist = abs(fingerX - stringX)

                                // Radio de acción del dedo (aumentado un poco para mejor tacto)
                                if (dist < 90f) {
                                    scope.launch {
                                        val newTarget = (anim.value + dragAmount.x).coerceIn(-70f, 70f)
                                        anim.snapTo(newTarget)
                                    }
                                } else {
                                    // Efecto estela suave si te alejas
                                    if (abs(anim.value) > 0.1f) {
                                        scope.launch {
                                            anim.animateTo(0f, spring(dampingRatio = 0.4f))
                                        }
                                    }
                                }
                            }

                            // DISPARADOR DE SONIDO Y ACCIÓN
                            if (!hasTriggered && abs(dragAmount.x) > 15) {
                                hasTriggered = true
                                // Reiniciar sonido si ya estaba sonando
                                if (mediaPlayer?.isPlaying == true) {
                                    mediaPlayer.seekTo(0)
                                } else {
                                    mediaPlayer?.start()
                                }
                                onStrum()
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // =========================================================
            // CAPA 1: CUERPO DE MADERA (FONDO REALISTA)
            // =========================================================
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(WoodDark, WoodReddish, WoodDark),
                    startY = 0f,
                    endY = height
                )
            )
            // Viñeteado radial para dar volumen al cuerpo
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    center = center,
                    radius = height * 0.9f
                )
            )

            // =========================================================
            // CAPA 2: PUENTE/HARDWARE (Visual)
            // =========================================================
            val bridgeHeight = 40.dp.toPx()
            val bridgeY = height - bridgeHeight

            // Barra metálica del puente
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF333333), Color(0xFF111111))
                ),
                topLeft = Offset(0f, bridgeY),
                size = Size(width, bridgeHeight)
            )

            // =========================================================
            // CAPA 3: LAS 6 CUERDAS (Punto 2 y 3)
            // =========================================================
            val totalGuitarWidth = width * 0.85f
            val startX = (width - totalGuitarWidth) / 2
            val stringSpacing = totalGuitarWidth / 5

            for (i in 0..5) {
                val baseX = startX + (stringSpacing * i)
                val deformation = stringDeformations[i].value

                // Grosor decreciente: La 0 es la más gorda (Mi grave), la 5 la fina (Mi agudo)
                val baseThickness = when(i) {
                    0 -> 9f; 1 -> 8f; 2 -> 7f // Entorchadas (Gruesas)
                    3 -> 5f; 4 -> 4f; 5 -> 3f // Lisas (Finas)
                    else -> 2f
                }

                // Cálculo de la Curva (Tu lógica Bezier intacta)
                val path = Path().apply {
                    moveTo(baseX, 0f)
                    quadraticBezierTo(
                        baseX + deformation,
                        touchY.coerceIn(0f, height),
                        baseX,
                        height
                    )
                }

                // --- DIBUJO DE SOMBRA (Para que floten) ---
                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = 0.5f),
                    style = Stroke(width = baseThickness * 1.5f, cap = StrokeCap.Round),
                    // Desplazamos la sombra un poco a la derecha
                )
                // (Nota: drawPath no tiene offset nativo fácil sin translate,
                // pero al ser cuerdas finas, la sombra centrada ancha funciona bien como "ambient occlusion")

                // --- DIBUJO DE LA CUERDA 3D ---
                // Diferenciamos materiales: Bronce para graves, Acero para agudas
                val isWound = i < 3 // Las 3 primeras son entorchadas

                val stringBrush = Brush.horizontalGradient(
                    colors = if (isWound) {
                        listOf(BronzeDark, BronzeLight, BronzeDark) // Efecto cilíndrico dorado
                    } else {
                        listOf(SteelDark, SteelLight, SteelDark) // Efecto cilíndrico plateado
                    },
                    startX = baseX - 10, // Ajuste aproximado para el degradado
                    endX = baseX + 10
                )

                drawPath(
                    path = path,
                    brush = stringBrush, // Usamos Brush en vez de Color plano
                    style = Stroke(width = baseThickness, cap = StrokeCap.Round)
                )
            }
        }
    }
}