# LKS Parking

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)
![Compose](https://img.shields.io/badge/Jetpack-Compose-orange.svg)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-red.svg)

## Descripción
Aplicación móvil para la gestión de reservas de plazas de parking en las oficinas de LKS Next.
**Este proyecto forma parte del Aula de Empresa de Movilidad de LKS Next y la UPV/EHU 2026.**

## Características Principales
- **Autenticación**: Registro e inicio de sesión con validación de correo corporativo (@lksnext.com).
- **Reservas**: Sistema de reserva de plazas con selección de fecha y tramos horarios (máx. 7 días de antelación y 9 horas de duración). Posibilidad de cancelación.
- **Visualización**: Mapa interactivo del estado del parking en tiempo real (Vista Cuadrícula y Lista).
- **Gestión de Vehículos**: Registro de múltiples vehículos (Coche, Eléctrico, Moto, Discapacitado).
- **Historial**: Consulta de reservas pasadas, activas y futuras con indicador de estado.
- **Notificaciones**: Avisos sobre confirmaciones, cancelaciones y recordatorios automáticos.
- **Reportes**: Sistema para informar de incidencias (daños, limpieza, ocupación indebida).

**[Prototipo interactivo en Figma](https://ardent-harp-31107545.figma.site)**

## Roadmap y Planes Futuros

- [x] **Firebase**: Integración de Firestore, Auth, Cloud Messaging y Crashlytics.
- [x] **Testing**: Tests unitarios de todos los ViewModels (cobertura 100%).
- [ ] **Analíticas**: Implementación de Firebase Performance Monitoring.
- [ ] **Automatización**: Pipeline de CI/CD (GitHub Actions) para tests y generación de `.apk`.

## Información para Desarrolladores

Toda la información técnica relativa a la arquitectura del sistema, la estructura de carpetas, la explicación de las clases clave y la guía de configuración del entorno de desarrollo se encuentra disponible en el siguiente archivo:

**[CONSULTAR DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)**

## Stack Tecnológico
- **Lenguaje**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Navegación**: Compose Navigation
- **Inyección de Dependencias**: Provisión manual vía `ViewModelFactory`
- **Componentes**: Material Design 3
- **Asincronía**: Kotlin Coroutines & StateFlow
- **Backend**: Firebase (Auth, Firestore, Cloud Messaging, Crashlytics)
- **Soporte Multiidioma**: Español, Inglés, Euskera

```text
app/src/main/java/com/lksnext/ParkingIMayordomo/
├── data/          # Modelos, Repositorios y AuthManager (Firebase)
├── ui/            # Pantallas (Pages), ViewModels y Componentes Reutilizables
├── utils/         # Validación, constantes de rutas, LocaleManager
└── MainActivity   # Punto de entrada, NavHost y Protección de Rutas
```

## Instalación y Configuración
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/imayordomo/LKS_Parking.git
   ```
2. Abrir el proyecto en **Android Studio (Ladybug o superior)**.
3. Sincronizar Gradle.
4. Ejecutar en un emulador o dispositivo físico con **Android 8.0 (API 26) o superior**.

## Contacto
Para dudas o soporte técnico, contactar con **imayordomo** o abrir un Issue en este repositorio.
