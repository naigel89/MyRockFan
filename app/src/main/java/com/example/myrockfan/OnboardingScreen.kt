package com.example.myrockfan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrockfan.ui.theme.RockTypography
import kotlinx.coroutines.launch

// Definición de paleta local para garantizar la estética "Dark Mode"
// independientemente del tema del sistema del usuario.
private val DarkBackground = Color(0xFF121212)
private val CardBackground = Color(0xFF1E1E1E)
private val AccentColor = Color(0xFFEF3868) // Rosa Rockero (Primary Brand Color)

/**
 * Pantalla Multifuncional: Onboarding + Ajustes.
 *
 * Diseño Inteligente:
 * Este Composable es "agnóstico" a si es la primera vez que entras o si estás editando.
 * - Si 'initialSelection' está vacío -> Actúa como Bienvenida (Onboarding).
 * - Si 'initialSelection' tiene datos -> Actúa como Pantalla de Configuración.
 */
@Composable
fun OnboardingScreen(
    initialSelection: Set<String> = emptySet(),
    onFinished: (Set<String>) -> Unit // Callback: Devuelve la lista final solo cuando el usuario confirma.
) {
    // ESTADO TRANSACCIONAL (Patrón de Borrador):
    // Creamos una copia local mutable de las preferencias.
    // Los cambios aquí NO se guardan en la base de datos hasta que se pulsa el botón "Guardar".
    // Esto permite al usuario experimentar o cancelar sin romper la configuración actual.
    val selectedBands = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }

    // Flag lógico para adaptar los textos de la interfaz (UX Contextual)
    val isEditMode = initialSelection.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {

        // --- SECCIÓN INFORMATIVA ---
        Spacer(modifier = Modifier.height(24.dp))

        // Texto dinámico según el contexto (Bienvenida vs Ajustes)
        Text(
            text = if (isEditMode) "TUS PREFERENCIAS" else "ELIGE TU SONIDO",
            style = RockTypography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecciona las bandas que quieres que aparezcan en tus historias.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )

        // --- LISTA DE SELECCIÓN (SCROLLABLE) ---
        // Usamos LazyColumn para rendimiento eficiente, ya que la lista de bandas podría crecer.
        LazyColumn(
            modifier = Modifier.weight(1f), // Ocupa todo el espacio disponible empujando el botón abajo
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RockData.allBands) { band ->
                BandItem(
                    name = band,
                    isSelected = selectedBands.contains(band),
                    // Lógica de Toggle: Añadir o Quitar de la lista temporal
                    onToggle = {
                        if (selectedBands.contains(band)) {
                            selectedBands.remove(band)
                        } else {
                            selectedBands.add(band)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTÓN DE CONFIRMACIÓN (COMMIT) ---
        Button(
            onClick = { onFinished(selectedBands.toSet()) },
            // Validación: Bloqueamos el avance si no hay al menos una banda seleccionada.
            // Esto evita errores posteriores en la generación de historias (random sobre lista vacía).
            enabled = selectedBands.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentColor,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isEditMode) "GUARDAR CAMBIOS" else "¡A ROCKEAR!",
                style = RockTypography.labelSmall.copy(fontSize = 16.sp),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Componente de Item de Lista (Stateless).
 *
 * Principio de UI: Feedback Visual Claro.
 * El componente cambia drásticamente (borde, fondo y color de texto) cuando está seleccionado,
 * facilitando el escaneo rápido visual por parte del usuario.
 */
@Composable
fun BandItem(name: String, isSelected: Boolean, onToggle: () -> Unit) {
    Surface(
        // Feedback visual: Si está seleccionado, usamos el color de acento con transparencia
        color = if (isSelected) AccentColor.copy(alpha = 0.15f) else CardBackground,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            // UX IMPORTANTE: Hacemos clickeable toda la tarjeta, no solo el checkbox (Ley de Fitts).
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nombre de la banda
            Text(
                text = name,
                style = RockTypography.bodyLarge,
                // Cambio de color para reforzar el estado activo/inactivo
                color = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.weight(1f)
            )

            // Checkbox decorativo (la acción real la maneja el Surface padre)
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentColor,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}