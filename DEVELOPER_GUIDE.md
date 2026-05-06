# Guía del Desarrollador - LKS Parking

Este documento proporciona una visión técnica detallada del proyecto **LKS Parking**, su arquitectura, estructura y guías para su mantenimiento y extensión.

## 1. Arquitectura del Sistema

El proyecto sigue el patrón de arquitectura **MVVM (Model-View-ViewModel)** recomendado por Google, adaptado para un flujo reactivo con **Jetpack Compose**.

### Flujo de Datos
1.  **UI (Compose)**: Observa estados expuestos por los ViewModels mediante `StateFlow`.
2.  **ViewModel**: Gestiona la lógica de presentación y se comunica con el Repositorio. No tiene conocimiento de la plataforma Android (Context, etc.) para facilitar su testabilidad.
3.  **Repository**: Actúa como una única fuente de verdad. Actualmente, la implementación (`ParkingRepositoryImpl`) utiliza un `AuthManager` para simular persistencia en memoria.
4.  **AuthManager (Mock Data)**: Simula una base de datos y un sistema de autenticación, permitiendo que la app sea funcional sin un backend real en esta etapa de prototipado.

## 2. Estructura de Carpetas (`app/src/main/java/com/lksnext/ParkingIMayordomo`)

### `data/`
- **`model/`**: Contiene las clases de datos (`User`, `Reservation`, `Vehicle`, `Notification`).
- **`repository/`**: 
    - `ParkingRepository.kt`: Interfaz que desacopla la lógica de negocio de la implementación de datos.
    - `ParkingRepositoryImpl.kt`: Implementación que orquesta el acceso a los datos.
- **`AuthManager.kt`**: Singleton que centraliza el estado de la sesión, las listas de reservas globales y la lógica de notificaciones.

### `ui/`
- **`pages/`**: Implementaciones de las pantallas de la app.
- **`viewmodel/`**: Lógica de estado de cada pantalla. Incluye `ViewModelFactory` para la inyección manual de dependencias.
- **`components/`**: UI components reutilizables (Barras de navegación, diálogos, etc.).
- **`theme/`**: Definición de colores, tipografías y el tema Material 3.

### `utils/`
- **`ParkingUtils.kt`**: Centraliza constantes de rutas y lógica compartida (ej. `isTimeOverlapping` para validar reservas).

## 3. Explicación de Clases Clave

### `MainActivity.kt` y `AppNavigation`
Es el núcleo de la aplicación.
- Configura `EdgeToEdge` y el tema.
- Define el `NavHost` con todas las rutas.
- **Inyección de Dependencias**: Crea la instancia única del repositorio y el factory, pasándolos a cada pantalla.
- **Protección de Rutas**: Utiliza la función `ProtectedRoute` para redirigir a la pantalla de Landing si no hay una sesión activa de usuario.

### `ParkingRepository.kt`
Define el contrato de todas las acciones que la aplicación puede realizar. Al usar una interfaz, el sistema está preparado para migrar de una base de datos local (Mock) a una API REST real simplemente creando una nueva implementación de esta interfaz.

### `ViewModelFactory.kt`
Dado que no se utiliza una librería de DI (como Hilt), esta clase es crucial. Permite instanciar ViewModels que requieren parámetros en su constructor (como el `ParkingRepository`).

## 4. Stack Tecnológico
- **Lenguaje**: [Kotlin](https://kotlinlang.org/) (1.9+)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Arquitectura**: MVVM
- **Navegación**: Compose Navigation
- **Asincronía**: Kotlin Coroutines & Flow
- **Componentes**: Material Design 3

## 5. Guía de Configuración para Desarrolladores

### Requisitos Previos
- **Android Studio**: Ladybug (2024.2.1) o superior.
- **JDK**: 17 o superior.
- **Android SDK**: API 26 (Android 8.0) como mínimo.

### Pasos para empezar
1.  **Importar el proyecto**:
    - Abre Android Studio.
    - Selecciona `File > Open...` y elige la carpeta raíz del proyecto.
2.  **Sincronizar Gradle**:
    - Al abrir el proyecto, Android Studio debería iniciar automáticamente la sincronización.
    - Si no es así, haz clic en el icono de elefante (`Sync Project with Gradle Files`) en la barra de herramientas superior.
3.  **Configurar Dispositivo**:
    - Conecta un dispositivo físico o crea un Emulador (AVD) con API 30+ para una mejor experiencia visual.
4.  **Compilar y Ejecutar**:
    - Pulsa el botón verde de "Play" (`Run 'app'`).

## 6. Roadmap y Planes Futuros 
Actualmente el proyecto es un prototipo funcional con datos en memoria (mock). Los siguientes pasos incluyen:

*   **Persistencia Real**: Migración del `ParkingRepository` para conectar con Firebase (Firestore/Realtime Database).
*   **Notificaciones Cloud**: Integración con Firebase Cloud Messaging (FCM).
*   **Calidad de Código**: Implementación de SonarCloud para análisis estático.
*   **Testing**: Implementación de tests funcionales en Android con Espresso y UIAutomator.
*   **Automatización**: Configuración de un pipeline de CI/CD (GitHub Actions) para ejecución de tests unitarios y generación automática de `.apk`.

## 7. Contacto
Para dudas o soporte técnico, contactar con **imayordomo** o abrir un Issue en este repositorio.