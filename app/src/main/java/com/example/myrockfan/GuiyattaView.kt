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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GuitarraTrigger(
    onStrum: () -> Unit,
    isVisible: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Altura donde estamos tocando (para doblar la cuerda ahí)
    var touchY by remember { mutableFloatStateOf(0f) }

    // Desplazamiento de cada cuerda (cuánto se dobla)
    val stringDeformations = remember { List(6) { Animatable(0f) } }

    val mediaPlayer = remember {
        try { MediaPlayer.create(context, R.raw.guitar_strum) } catch (e: Exception) { null }
    }

    // Evitar múltiples disparos
    var hasTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            // DETECTOR DE GESTOS
            .pointerInput(isVisible) {
                if (isVisible) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            hasTriggered = false
                            touchY = offset.y // Guardamos donde empieza el toque
                        },
                        onDragEnd = {
                            // Al soltar, todas las cuerdas rebotan a 0 (rectas)
                            stringDeformations.forEach { anim ->
                                scope.launch {
                                    anim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioHighBouncy, // Muy elástico
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            touchY = change.position.y // Actualizamos altura del dedo

                            // LÓGICA DE INTERACCIÓN FÍSICA
                            val width = size.width
                            val totalGuitarWidth = width * 0.7f
                            val startX = (width - totalGuitarWidth) / 2
                            val stringSpacing = totalGuitarWidth / 5

                            // Recorremos las cuerdas para ver cuál estamos "empujando"
                            stringDeformations.forEachIndexed { index, anim ->
                                val stringX = startX + (stringSpacing * index)
                                val fingerX = change.position.x

                                // Distancia del dedo a esta cuerda
                                val dist = abs(fingerX - stringX)

                                // Si el dedo está cerca (zona de influencia de 60px), doblamos la cuerda
                                if (dist < 80f) {
                                    scope.launch {
                                        // La cuerda se mueve con el dedo (dragAmount.x)
                                        // pero con un límite para que no parezca chicle infinito
                                        val newTarget = (anim.value + dragAmount.x).coerceIn(-60f, 60f)
                                        anim.snapTo(newTarget)
                                    }
                                } else {
                                    // Si el dedo se aleja, la cuerda empieza a volver (efecto estela)
                                    if (abs(anim.value) > 0.1f) {
                                        scope.launch {
                                            anim.animateTo(0f, spring(dampingRatio = 0.4f))
                                        }
                                    }
                                }
                            }

                            // Disparar la acción si hay movimiento brusco general
                            if (!hasTriggered && abs(dragAmount.x) > 10) {
                                hasTriggered = true
                                if (mediaPlayer?.isPlaying == true) mediaPlayer.seekTo(0) else mediaPlayer?.start()
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
            val totalGuitarWidth = width * 0.7f
            val startX = (width - totalGuitarWidth) / 2
            val stringSpacing = totalGuitarWidth / 5

            val colorNickel = Color(0xFFC0C0C0)
            val colorBronze = Color(0xFFCD7F32)

            for (i in 0..5) {
                val baseX = startX + (stringSpacing * i)

                // AQUÍ ESTÁ LA MAGIA DE LA CURVA
                // La cuerda empieza arriba (baseX, 0) y acaba abajo (baseX, height)
                // Pero tiene un punto de control en (baseX + deformación, touchY)

                val deformation = stringDeformations[i].value
                val path = Path().apply {
                    moveTo(baseX, 0f) // Punto inicio (Arriba)

                    // Curva cuadrática hacia donde está el dedo
                    quadraticBezierTo(
                        baseX + deformation, // X del punto de control (doblado)
                        touchY.coerceIn(0f, height), // Y del punto de control (dedo)
                        baseX, // X final (recto)
                        height // Y final (Abajo)
                    )
                }

                val stringColor = if (i < 3) colorBronze else colorNickel
                val thickness = if (i < 3) (5f - i) else (2f)

                drawPath(
                    path = path,
                    color = stringColor,
                    style = Stroke(width = thickness, cap = StrokeCap.Round)
                )
            }
        }
    }
}