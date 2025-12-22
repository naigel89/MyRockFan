package com.example.myrockfan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.myrockfan.R

// Configuración del proveedor de fuentes de Google
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val PlayfairFont = GoogleFont("Playfair Display") // Fuente elegante para títulos

val PlayfairFamily = FontFamily(
    Font(googleFont = PlayfairFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Definimos la fuente Roboto
val RobotoFont = GoogleFont("Roboto")

val RobotoFontFamily = FontFamily(
    Font(googleFont = RobotoFont, fontProvider = provider),
    Font(googleFont = RobotoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = RobotoFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Definimos nuestros estilos de texto usando Roboto
val RockTypography = Typography(
    // TÍTULO GIGANTE (Serif)
    displayLarge = TextStyle(
        fontFamily = PlayfairFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = Color.White
    ),
    // ETIQUETA "CURIOSIDADES"
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, // O Roboto si la tienes
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    ),
    // TEXTO DEL CUERPO (Lectura cómoda)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // O Roboto
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        color = Color(0xFFE0E0E0) // Blanco roto
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp, // Título grande pero controlado (Punto 1)
        lineHeight = 34.sp
    ),
    bodyMedium = TextStyle( // Para la query de la imagen
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        fontStyle = FontStyle.Italic
    )
)