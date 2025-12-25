# 🎸 MyRockFan: El Backstage de la Historia

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-Enabled-green) ![AI](https://img.shields.io/badge/Powered%20by-Gemini%20AI-blue) ![Status](https://img.shields.io/badge/Status-In%20Development-orange)

**MyRockFan** no es solo una app de música; es un cronista digital impulsado por Inteligencia Artificial. Genera narrativas inmersivas sobre anécdotas ocultas, grabaciones caóticas y leyendas del Rock, acompañadas de imágenes contextuales buscadas en tiempo real.

> *"La historia del Rock no se lee, se invoca."*

## 📱 Screenshots

| Pantalla de Inicio | Generando Historia | Historia & Curiosidad |
|:---:|:---:|:---:|
| <img src="screenshots/home.png" width="250"> | <img src="screenshots/loading.png" width="250"> | <img src="screenshots/story.png" width="250"> |

*(Nota: Sube tus capturas a una carpeta llamada 'screenshots' en tu repo)*

## 🔥 Características Principales

* **🎙️ Narrador IA (Gemini 1.5 Flash):** Integra la API de Google Gemini con un "System Prompt" diseñado para actuar como un locutor de radio experto, generando historias únicas, no repetitivas y emocionalmente ricas.
* **🎸 Interacción Física (Canvas):** Pantalla de inicio con una guitarra interactiva dibujada con `Canvas`. Las cuerdas responden al tacto usando curvas de Bézier y física de rebote (Spring Animation), simulando la vibración real.
* **🖼️ Búsqueda Inteligente de Imágenes:** Sistema propio de filtrado (`ImageRepository`) que conecta con Google Custom Search API. Implementa lógica de "Portero de Discoteca" para validar que las imágenes pertenezcan realmente a la banda, descartando resultados basura o irrelevantes.
* **💿 UI/UX Temática:** Componentes personalizados como un disco de vinilo giratorio con reflejos anisotrópicos (`SweepGradient`) y tipografía estilo revista musical.
* **🔒 Seguridad:** Gestión de claves API mediante `local.properties` y `BuildConfig` para evitar exponer secretos en el control de versiones.

## 🛠️ Stack Tecnológico

* **Lenguaje:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material3)
* **Arquitectura:** MVVM (Model-View-ViewModel)
* **IA:** Google Generative AI SDK (Gemini)
* **Red:** Retrofit + OkHttp
* **Imágenes:** Coil (con interceptores personalizados para simular User-Agent de navegador)
* **Corrutinas:** Kotlin Coroutines & Flow para gestión asíncrona.

## 🚀 Configuración e Instalación

Este proyecto utiliza claves de API que no están incluidas en el repositorio por seguridad. Para ejecutarlo:

1.  **Clona el repositorio:**
    ```bash
    git clone [https://github.com/TuUsuario/MyRockFan.git](https://github.com/TuUsuario/MyRockFan.git)
    ```

2.  **Configura las claves:**
    Crea un archivo llamado `local.properties` en la raíz del proyecto (si no existe) y añade tus propias claves:

    ```properties
    sdk.dir=RUTA_A_TU_SDK_ANDROID
    
    # Tus Claves Secretas
    GEMINI_API_KEY=Tu_Clave_De_Google_AI_Studio
    GOOGLE_SEARCH_KEY=Tu_Clave_De_Google_Cloud
    SEARCH_CX_ID=Tu_ID_De_Buscador_Personalizado
    ```

3.  **Sincroniza y Ejecuta:**
    Abre el proyecto en Android Studio, dale a "Sync Project with Gradle Files" y ejecuta la app en un emulador o dispositivo físico.

## 🧠 Retos Técnicos Superados

* **Filtrado de Alucinaciones Visuales:** La IA a veces pide fotos de conceptos abstractos. Se implementó un algoritmo de doble paso (Búsqueda Específica -> Fallback a Búsqueda Genérica) para asegurar que siempre se muestre una foto relevante de la banda.
* **Animación de Cuerdas:** Lograr que las cuerdas de la guitarra se sintieran "tensas" y volvieran a su sitio requirió el uso de `Animatable` con configuraciones de `Spring.DampingRatioHighBouncy`.
* **Bloqueo de Imágenes:** Muchas webs bloquean la carga de imágenes en apps (Error 403). Se solucionó inyectando cabeceras `User-Agent` falsas en el cliente de `Coil`.

## 🤘 Contribución

¡Las Pull Requests son bienvenidas! Si tienes una idea para añadir integración con Spotify o mejorar las animaciones:

1.  Haz un Fork del proyecto.
2.  Crea tu rama (`git checkout -b feature/AmazingFeature`).
3.  Haz Commit de tus cambios (`git commit -m 'Add some AmazingFeature'`).
4.  Haz Push a la rama (`git push origin feature/AmazingFeature`).
5.  Abre una Pull Request.

## 📄 Licencia

Distribuido bajo la licencia MIT.

---
*Desarrollado con distorsión y volumen al 11.* 🎸
