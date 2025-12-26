package com.example.myrockfan

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * MOTOR DE RENDERIZADO DE TEXTO (Utils)
 *
 * Jetpack Compose no entiende Markdown nativamente (ej: **negrita**).
 * Estas funciones actúan como un "Traductor": toman el texto plano que nos devuelve Gemini
 * y construyen un `AnnotatedString`, que es el objeto que Compose usa para
 * aplicar estilos (SpanStyles) a partes específicas de una misma cadena de texto.
 */

// Esta función convierte el texto "sucio" de Gemini en texto con estilo para Compose
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        // ESTRATEGIA DE PARSEO: "DIVIDE Y VENCERÁS"
        // En lugar de usar Regex complejas, dividimos el string usando los delimitadores.
        // Al hacer split por "**", el array resultante alterna siempre entre:
        // [Texto Normal, Texto Negrita, Texto Normal, Texto Negrita...]
        val parts = text.split("**")

        parts.forEachIndexed { index, part ->
            // Lógica de alternancia: Los índices IMPARES (1, 3, 5...) son los que estaban
            // encerrados entre los asteriscos dobles.
            if (index % 2 == 1) {
                // Aplicamos el estilo Bold al span actual
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                // Si es índice PAR, es texto "normal" (fuera de las negritas).
                // PERO, dentro de este texto normal puede haber cursivas (*texto*).
                // Así que aplicamos recursividad lógica (segundo nivel de split).
                val italicParts = part.split("*")
                italicParts.forEachIndexed { i, subPart ->
                    if (i % 2 == 1 && italicParts.size > 1) {
                        // Misma lógica: Índices impares del sub-split son cursivas.
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(subPart)
                        }
                    } else {
                        // Texto plano final (sin negrita ni cursiva)
                        append(subPart)
                    }
                }
            }
        }
    }
}