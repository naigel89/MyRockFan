package com.example.myrockfan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// --- FRASES DE CARGA ROCKERAS ---
// Banco de textos para rotar durante la espera.
// Objetivo: Reducir la percepción de tiempo de espera (latencia de IA)
// entreteniendo al usuario con "lore" del contexto musical.
val loadingPhrases = listOf(
    "Afinando la sexta cuerda...",
    "Conectando los amplificadores Marshall...",
    "Buscando púas perdidas en el suelo...",
    "Despertando al baterista...",
    "Subiendo el volumen al 11...",
    "Escribiendo el setlist de esta noche...",
    "Probando micrófono: 1, 2, sí, probando...",
    "Llamando al espíritu de Lemmy..."
)

/**
 * Pantalla de Carga Principal.
 * Orquesta los dos elementos visuales: el vinilo animado y el texto cíclico.
 * Usa un diseño centrado simple para focalizar la atención.
 */
@Composable
fun RockLoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. EL VINILO GIRATORIO (Elemento gráfico principal)
        SpinningVinyl()
        Spacer(modifier = Modifier.height(32.dp))

        // 2. TEXTO CAMBIANTE (Feedback narrativo)
        LoadingTextCycle()
    }
}

/**
 * Componente gráfico complejo: Un disco de vinilo dibujado vectorialmente en tiempo real.
 * No usa imágenes PNG/JPG, sino dibujo nativo en Canvas para nitidez infinita y rendimiento.
 */
@Composable
fun SpinningVinyl() {
    // Motor de Animación:
    // Configuración de rotación infinita de 0 a 360 grados.
    // 1500ms es una velocidad elegida deliberadamente para transmitir energía (Rock)
    // sin llegar a marear (40 RPM aprox).
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing) // 1.5 segundos por vuelta (40 RPM aprox)
        ), label = "rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(140.dp) // Tamaño prominente en pantalla
            .rotate(angle) // Aplicamos la rotación a todo el contenedor
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)

            // 1. CUERPO DEL VINILO Y FÍSICA DE LA LUZ
            // Truco visual clave: Usamos 'SweepGradient' (degradado de barrido).
            // Esto alterna colores claros y oscuros en forma de cono. Al girar el canvas,
            // simula el efecto físico de la luz reflejándose en los surcos del plástico (Anisotropía).
            val vinylBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF111111), // Negro base
                    Color(0xFF333333), // Reflejo de luz (Gris)
                    Color(0xFF111111),
                    Color(0xFF333333), // Segundo reflejo opuesto
                    Color(0xFF111111)  // Cierre para evitar cortes visuales
                ),
                center = center
            )

            drawCircle(
                brush = vinylBrush,
                radius = radius
            )

            // 2. TEXTURA DE LOS SURCOS
            // Dibujamos anillos concéntricos semitransparentes para romper la uniformidad
            // y que parezca un disco real, no solo un círculo degradado.
            for (i in 1..6) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = radius * (1 - (i * 0.1f)),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 3. ETIQUETA CENTRAL (LABEL)
            // Degradado lineal vibrante para contrastar con la oscuridad del vinilo.
            val labelRadius = radius * 0.35f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEF3868), Color(0xFF9C27B0)), // Rosa a Morado
                    start = Offset(center.x - labelRadius, center.y - labelRadius),
                    end = Offset(center.x + labelRadius, center.y + labelRadius)
                ),
                radius = labelRadius
            )

            // 4. EL INDICADOR DE GIRO (RAYO)
            // Problema de diseño: Un círculo perfecto girando parece estático.
            // Solución: Dibujamos una forma asimétrica (un rayo) en el centro.
            // Esto hace que el cerebro perciba inmediatamente la velocidad y el movimiento.
            val path = androidx.compose.ui.graphics.Path().apply {
                // Coordenadas relativas al centro para dibujar un rayo
                val size = labelRadius * 0.6f
                moveTo(center.x + size * 0.3f, center.y - size)       // Arriba derecha
                lineTo(center.x - size * 0.5f, center.y + size * 0.1f) // Centro izquierda
                lineTo(center.x - size * 0.1f, center.y + size * 0.1f) // Pequeño retroceso
                lineTo(center.x - size * 0.3f, center.y + size)       // Abajo izquierda (punta)
                lineTo(center.x + size * 0.5f, center.y - size * 0.1f) // Centro derecha
                lineTo(center.x + size * 0.1f, center.y - size * 0.1f) // Pequeño retroceso
                close()
            }

            drawPath(
                path = path,
                color = Color.White
            )

            // 5. AGUJERO DEL EJE (Spindle Hole)
            // Detalle final de realismo: agujero pasante (color del fondo de la app).
            drawCircle(
                color = Color(0xFF121212), // Del mismo color que el fondo de la app
                radius = radius * 0.05f
            )
        }
    }
}

/**
 * Gestor de Textos Cíclicos.
 * Mantiene la UI viva cambiando el mensaje cada 2 segundos.
 */
@Composable
fun LoadingTextCycle() {
    // Estado local para saber qué frase mostrar
    var phraseIndex by remember { mutableIntStateOf(0) }

    // Corrutina de temporizador:
    // Se ejecuta independientemente de la UI y actualiza el índice infinitamente
    // mientras el componente esté en pantalla.
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            phraseIndex = (phraseIndex + 1) % loadingPhrases.size
        }
    }

    // Transición Animada:
    // En lugar de cambiar el texto de golpe (que se ve tosco), usamos AnimatedContent.
    // Efecto: El texto viejo sube y desaparece (FadeOut + SlideUp),
    // el nuevo entra desde abajo (FadeIn + SlideUp).
    AnimatedContent(
        targetState = phraseIndex,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) + slideInVertically { 20 } togetherWith
                    fadeOut(animationSpec = tween(400)) + slideOutVertically { -20 }
        }, label = "textChange"
    ) { index ->
        Text(
            text = loadingPhrases[index],
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = Color(0xFFE0E0E0),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}