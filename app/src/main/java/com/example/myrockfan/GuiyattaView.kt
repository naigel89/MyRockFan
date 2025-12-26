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

/**
 * Paleta cromática diseñada para evocar materiales orgánicos (madera) y metálicos (cuerdas).
 * La gradación de tonos permite simular profundidad y desgaste, elementos clave en la estética Rock.
 */
val WoodDark = Color(0xFF1A0F0A)
val WoodReddish = Color(0xFF4E2A1A)
val BronzeDark = Color(0xFF8B5A2B)
val BronzeLight = Color(0xFFE6C68C)
val SteelDark = Color(0xFF555555)
val SteelLight = Color(0xFFDDDDDD)

/**
 * Disparador visual y auditivo de la aplicación.
 * Transforma un gesto táctil en una acción de "invocación" de historias, utilizando una metáfora física 
 * (el rasgueo) para crear una conexión emocional con el usuario antes de mostrar el contenido.
 */
@Composable
fun GuitarraTrigger(
    modifier: Modifier = Modifier,
    onStrum: () -> Unit,
    isVisible: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /**
     * Gestión del recurso de audio. 
     * Se utiliza MediaPlayer de forma controlada para proporcionar feedback inmediato al gesto, 
     * reforzando la sensación de "tocar" un instrumento real.
     */
    val mediaPlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.guitar_strum)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { mediaPlayer?.release() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * Sistema de física y deformación.
     * Utiliza Animatable para simular la elasticidad de las cuerdas. Cada cuerda es independiente, 
     * permitiendo una respuesta visual dinámica basada en la posición exacta del dedo.
     */
    var touchY by remember { mutableFloatStateOf(0f) }
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
                            /**
                             * Efecto de retorno elástico.
                             * Al soltar, las cuerdas regresan a su posición de equilibrio usando una física de muelle (Spring), 
                             * simulando la vibración natural de una cuerda de tensión real.
                             */
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

                            val width = size.width
                            val totalGuitarWidth = width * 0.85f
                            val startX = (width - totalGuitarWidth) / 2
                            val stringSpacing = totalGuitarWidth / 5

                            /**
                             * Cálculo de proximidad y deformación.
                             * El dedo solo afecta a las cuerdas que se encuentran dentro de su radio de acción, 
                             * aplicando un desplazamiento lateral (snapTo) para una respuesta de latencia cero.
                             */
                            stringDeformations.forEachIndexed { index, anim ->
                                val stringX = startX + (stringSpacing * index)
                                val fingerX = change.position.x
                                val dist = abs(fingerX - stringX)

                                if (dist < 90f) {
                                    scope.launch {
                                        val newTarget = (anim.value + dragAmount.x).coerceIn(-70f, 70f)
                                        anim.snapTo(newTarget)
                                    }
                                } else {
                                    if (abs(anim.value) > 0.1f) {
                                        scope.launch {
                                            anim.animateTo(0f, spring(dampingRatio = 0.4f))
                                        }
                                    }
                                }
                            }

                            /**
                             * Umbral de activación.
                             * Define cuánta fuerza/distancia se requiere para considerar que el rasgueo ha tenido éxito, 
                             * evitando disparos accidentales y asegurando que la acción sea deliberada.
                             */
                            if (!hasTriggered && abs(dragAmount.x) > 15) {
                                hasTriggered = true
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

            /**
             * Representación visual del cuerpo (Fondo).
             * Aplica degradados lineales y radiales para imitar el lacado de una guitarra de alta gama y su volumen.
             */
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(WoodDark, WoodReddish, WoodDark),
                    startY = 0f,
                    endY = height
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    center = center,
                    radius = height * 0.9f
                )
            )

            val bridgeHeight = 40.dp.toPx()
            val bridgeY = height - bridgeHeight

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF333333), Color(0xFF111111))
                ),
                topLeft = Offset(0f, bridgeY),
                size = Size(width, bridgeHeight)
            )

            /**
             * Renderizado de cuerdas dinámicas.
             * 1. Diferencia calibres y materiales (bronce vs acero) para realismo visual.
             * 2. Utiliza curvas de Bézier cuadráticas para representar la deformación física en tiempo real.
             * 3. Aplica degradados metálicos para simular el brillo del metal bajo los focos del escenario.
             */
            val totalGuitarWidth = width * 0.85f
            val startX = (width - totalGuitarWidth) / 2
            val stringSpacing = totalGuitarWidth / 5

            for (i in 0..5) {
                val baseX = startX + (stringSpacing * i)
                val deformation = stringDeformations[i].value

                val baseThickness = when(i) {
                    0 -> 9f; 1 -> 8f; 2 -> 7f
                    3 -> 5f; 4 -> 4f; 5 -> 3f
                    else -> 2f
                }

                val path = Path().apply {
                    moveTo(baseX, 0f)
                    quadraticBezierTo(
                        baseX + deformation,
                        touchY.coerceIn(0f, height),
                        baseX,
                        height
                    )
                }

                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = 0.5f),
                    style = Stroke(width = baseThickness * 1.5f, cap = StrokeCap.Round),
                )

                val isWound = i < 3
                val stringBrush = Brush.horizontalGradient(
                    colors = if (isWound) {
                        listOf(BronzeDark, BronzeLight, BronzeDark)
                    } else {
                        listOf(SteelDark, SteelLight, SteelDark)
                    },
                    startX = baseX - 10,
                    endX = baseX + 10
                )

                drawPath(
                    path = path,
                    brush = stringBrush,
                    style = Stroke(width = baseThickness, cap = StrokeCap.Round)
                )
            }
        }
    }
}
