package com.example.myrockfan

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

// Esta función convierte el texto "sucio" de Gemini en texto con estilo para Compose
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        // 1. Dividimos el texto para buscar negritas (**texto**)
        val parts = text.split("**")

        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Es una parte impar -> Estaba entre dobles asteriscos -> NEGRITA
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                // Es texto normal, pero buscamos cursivas simples (*texto*)
                val italicParts = part.split("*")
                italicParts.forEachIndexed { i, subPart ->
                    if (i % 2 == 1 && italicParts.size > 1) {
                        // Cursiva
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(subPart)
                        }
                    } else {
                        // Texto normal
                        append(subPart)
                    }
                }
            }
        }
    }
}

// Función extra para limpiar títulos (Quita los ### y espacios extra)
fun cleanTitle(rawTitle: String): String {
    return rawTitle
        .replace("#", "")
        .replace("*", "")
        .trim()
}