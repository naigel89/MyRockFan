package com.example.myrockfan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }

    // Estado para guardar qué bandas están marcadas temporalmente
    val selectedBands = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            text = "¿Qué suena en tus auriculares?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Selecciona tus artistas favoritos para personalizar las curiosidades.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Lista con Scroll
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(RockData.allBands) { band ->
                BandItem(
                    name = band,
                    isSelected = selectedBands.contains(band),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de Continuar
        Button(
            onClick = {
                scope.launch {
                    // Guardamos en disco y avisamos que terminamos
                    userPreferences.saveBands(selectedBands.toSet())
                    onFinished()
                }
            },
            enabled = selectedBands.isNotEmpty(), // Solo activo si hay al menos 1 seleccionado
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("¡A Rcanrolear!")
        }
    }
}

@Composable
fun BandItem(name: String, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
    }
}