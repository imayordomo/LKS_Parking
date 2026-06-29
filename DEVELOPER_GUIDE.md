# Guía del Desarrollador - LKS Parking

Este documento proporciona una visión técnica detallada del proyecto **LKS Parking**, su arquitectura, estructura y guías para su mantenimiento y extensión.

## 1. Arquitectura del Sistema

El proyecto sigue el patrón de arquitectura **MVVM (Model-View-ViewModel)** recomendado por Google, adaptado para un flujo reactivo con **Jetpack Compose** y **StateFlow**.

### Flujo de Datos
1.  **Firestore**: Datos persistentes en la nube. `AuthManager` escucha cambios en tiempo real mediante snapshot listeners.
2.  **AuthManager (Singleton)**: Centraliza el estado global (usuario, reservas, vehículos, notificaciones, reportes). Expone `StateFlow<T>` para cada colección, actualizados reactivamente desde Firestore.
3.  **ParkingRepositoryImpl**: Implementa `ParkingRepository`. Delega todos sus `StateFlow` directamente a `AuthManager` (sin polling ni `syncState()`). Actúa como capa de abstracción para los ViewModels.
4.  **ViewModel**: Gestiona la lógica de presentación. Recibe el repositorio por constructor (inyección manual vía `ViewModelFactory`). Expone estados a la UI con `StateFlow`.
5.  **UI (Compose)**: Observa los `StateFlow` de los ViewModels mediante `collectAsState()`.

## 2. Estructura de Carpetas (`app/src/main/java/com/lksnext/ParkingIMayordomo`)

### `data/`
- **`model/`**: Clases de datos (`User`, `Reservation`, `Vehicle`, `Notification`, `Report`).
- **`repository/`**:
    - `ParkingRepository.kt`: Interfaz que desacopla la lógica de negocio de la implementación de datos.
    - `ParkingRepositoryImpl.kt`: Implementación que delega todos los `StateFlow` a `AuthManager`.
- **`AuthManager.kt`**: Singleton con lógica Firebase Auth, snapshot listeners en Firestore, y métodos CRUD para todas las entidades.

### `ui/`
- **`pages/`**: Implementaciones de las pantallas de la app.
- **`viewmodel/`**: Lógica de estado de cada pantalla. Incluye `ViewModelFactory` para la inyección manual de dependencias.
- **`components/`**: UI components reutilizables (barras de navegación, diálogos, calendario, etc.).
- **`theme/`**: Definición de colores, tipografías y el tema Material 3.
- **`navigation/`**: `ParkingNavigation.kt` define el `NavHost`, las rutas protegidas con `ProtectedRoute` y la configuración del `DrawerLayout`.

### `utils/`
- **`ParkingUtils.kt`**: Constantes de rutas y lógica compartida (ej. `isTimeOverlapping` para validar reservas).
- **`LocaleManager.kt`**: Gestión del cambio de idioma (ES/EN/EU) con `AppCompatDelegate.setApplicationLocales()` y reinicio de actividad.

## 3. Explicación de Clases Clave

### `AuthManager.kt`
Singleton que actúa como fuente única de verdad. En su `init`:
- Configura Firebase Auth y escucha cambios de sesión.
- Inicia snapshot listeners en las colecciones `reservas`, `vehiculos`, `notificaciones`, `reportes` filtradas por `userId`.
- Expone `StateFlow<T>` (`user`, `reservations`, `vehicles`, `notifications`, `reports`) para que el resto de la app consuma datos reactivamente.

### `ParkingRepositoryImpl.kt`
Implementación que delega directamente a `AuthManager`:
```kotlin
override val user: StateFlow<User?> = AuthManager.user
override val reservations: StateFlow<List<Reservation>> = AuthManager.reservations
```
Sin polling, sin temporizadores, sin `syncState()`.

### `MainActivity.kt` y `ParkingNavigation.kt`
- `MainActivity.kt`: Configura `EdgeToEdge` y el tema. Punto de entrada.
- `ParkingNavigation.kt`: Define el `NavHost`, rutas protegidas mediante `ProtectedRoute` (redirige a Landing si no hay sesión), crea el repositorio y el `ViewModelFactory`.

### `ViewModelFactory.kt`
Permite instanciar ViewModels que requieren parámetros en su constructor (como `ParkingRepository`) sin usar una librería de DI externa.

## 4. Stack Tecnológico
- **Lenguaje**: [Kotlin](https://kotlinlang.org/) (1.9+)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Arquitectura**: MVVM
- **Navegación**: Compose Navigation
- **Asincronía**: Kotlin Coroutines & StateFlow
- **Componentes**: Material Design 3
- **Backend**: Firebase (Auth, Firestore, Cloud Messaging, Crashlytics)
- **Soporte Multiidioma**: Español, Inglés, Euskera (locale switch con reinicio de actividad)

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
3.  **Configurar Firebase**:
    - El proyecto incluye `google-services.json` con la configuración de Firebase. Si trabajas con una copia propia, descarga tu archivo desde Firebase Console y colócalo en la carpeta `app/`.
4.  **Configurar Dispositivo**:
    - Conecta un dispositivo físico o crea un Emulador (AVD) con API 30+ para una mejor experiencia visual.
5.  **Compilar y Ejecutar**:
    - Pulsa el botón verde de "Play" (`Run 'app'`).

### Ejecutar Tests
```bash
./gradlew testDebugUnitTest
```

## 6. Roadmap y Planes Futuros

- [x] **Firebase**: Integración de Auth, Firestore, Cloud Messaging y Crashlytics.
- [x] **Testing**: Tests unitarios de todos los ViewModels (cobertura 100%).
- [ ] **Analíticas**: Implementación de Firebase Performance Monitoring.
- [ ] **Calidad de Código**: Implementación de SonarCloud para análisis estático.
- [ ] **Testing Funcional**: Tests en Android con Espresso y UIAutomator.
- [ ] **Automatización**: Configuración de un pipeline de CI/CD (GitHub Actions) para ejecución de tests unitarios y generación automática de `.apk`.

## 7. Contacto
Para dudas o soporte técnico, contactar con **imayordomo** o abrir un Issue en este repositorio.
