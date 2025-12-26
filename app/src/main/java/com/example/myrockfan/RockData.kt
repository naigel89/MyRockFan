package com.example.myrockfan

/**
 * REPOSITORIO ESTÁTICO DE DATOS (Hardcoded Database).
 *
 * Este objeto actúa como la "base de datos" local y ligera de la aplicación.
 *
 * Ventajas de este enfoque para esta App:
 * 1. Simplicidad: Evita configurar Room o SQLite para una lista fija que rara vez cambia.
 * 2. Rendimiento: Al ser un 'object' (Singleton), esta lista se carga una vez en memoria
 * y es accesible instantáneamente desde cualquier pantalla (Onboarding, ViewModel, etc).
 * 3. Mantenibilidad: Si quieres añadir una banda nueva, solo tocas este archivo y
 * automáticamente aparece en la pantalla de selección.
 * (NOTA: Se podria haber hecho una base de datos para esto, pero debido a la sencillez de
 * la aplicación se decidió hacer así)
 */
object RockData {
    // Inventario maestro de bandas soportadas.
    // Mezcla deliberada de:
    // - Clásicos internacionales (AC/DC, Queen)
    // - Rock Español (Extremoduro, Héroes del Silencio)
    // - Escena Indie/Moderna (Arde Bogotá, Viva Suecia)
    // (muy buena música en conclusión)
    val allBands = listOf(
        "AC/DC", "Metallica", "Iron Maiden", "Nirvana",
        "Arde Bogotá", "Extremoduro", "Héroes del Silencio",
        "The Beatles", "Queen", "Foo Fighters", "Arctic Monkeys",
        "Viva Suecia", "Led Zeppelin", "Pink Floyd", "Rolling Stones",
        "Guns N' Roses", "Estopa", "Mago de Oz", "Muse", "Red Hot Chili Peppers",
        "Radiohead", "Megadeth", "Robe", "The Warning", "The Offspring",
        "Dio", "Black Sabbath", "Ozzy Osbourne", "Barón Rojo", "Angeles del Infierno",
        "Fito y Fitipaldis", "Marea", "Mötorhead", "Manowar", "Green Day",
        "Queens of the Stone Age", "Montley Crüe", "Rammstein", "Måneskin",
        "Linkin Park", "System of a Down", "Limp Bizkit", "Avatar", "Greta Van Fleet",
        "Drowning Pool", "Barns Courtney", "Fleetwood Mac", "Scorpions", "Eric Clapton",
        "Van Halen", "El Canto del Loco", "Imagine Dragons", "Amyl and the Sniffers",
        "Volbeat", "Pantera", "Hombres G", "Five Finger Death Punch", "Judas Priest",
        "Antrax", "Barricada", "Rage Against the Machine", "Slipknot", "Muro", "Electric Callboy",
        "Twisted Sister", "Alice Cooper", "Depeche Mode", "Avenged Sevenfold", "Blur",
        "REM", "My Chemical Romance", "ZZ Top", "The Cure", "Talco", "The Cranberries",
        "Dover", "Dorothy", "Sisters of Mercy", "Amaral", "Korn", "Aerosmith", "Bon Jovi",
        "Deep Purple", "The Who", "The Doors", "Credence Clearwater Revival", "Jimi Hendrix",
        "The Police", "U2", "Ghost", "Bring Me The Horizon", "Distrubed", "Papa Roach", "Sabaton",
        "Gojira", "Pearl Jam", "Alice in Chains", "Gorillaz", "Platero y Tú", "Ska-P", "La Raíz",
        "La M.O.D.A"
    )
        // DECISIÓN DE UX (User Experience):
        // Ordenamos alfabéticamente en tiempo de ejecución.
        // Esto facilita enormemente que el usuario encuentre a su banda favorita en la lista
        // de selección sin tener que hacer scroll aleatorio.
        .sorted()
}