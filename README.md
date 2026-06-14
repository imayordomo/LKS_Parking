# LKS Parking 

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)
![Compose](https://img.shields.io/badge/Jetpack-Compose-orange.svg)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-red.svg)

## Descripción
Aplicación móvil para la gestión de reservas de plazas de parking en las oficinas de LKS Next. 
**Este proyecto forma parte del Aula de Empresa de Movilidad de LKS Next y la UPV/EHU 2026.**
<br>

> [!WARNING]
> Proyecto todavía **en desarrollo**. Siguiente implementación: Testing y Analíticas. 

## Características Principales
- **Autenticación**: Registro e inicio de sesión con validación de correo corporativo (@lks.com).
- **Reservas**: Sistema de reserva de plazas con selección de fecha y tramos horarios (máx. 7 días de antelación y 9 horas de duración).
- **Visualización**: Mapa interactivo del estado del parking en tiempo real (Vista Cuadrícula y Lista).
- **Gestión de Vehículos**: Registro de múltiples vehículos (Coche, Eléctrico, Moto, Discapacitado).
- **Historial**: Consulta y filtrado de reservas pasadas y futuras (sin opción de borrado para auditoría).
- **Notificaciones**: Avisos sobre confirmaciones y recordatorios automáticos.
- **Reportes**: Sistema para informar de incidencias (daños, limpieza, ocupación indebida).

**[Prototipo interactivo en Figma](https://ardent-harp-31107545.figma.site)**

## Roadmap y Planes Futuros
Los siguientes pasos incluyen:

*   **Analíticas**: Implementación de Firebase Performance Monitoring y Crashlytics.
*   **Testing**: Implementación de tests unitarios y funcionales en Android.
*   **Automatización**: Configurar mejor el pipeline de CI/CD (GitHub Actions) para ejecución de tests unitarios y generación automática de `.apk`.

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

```text
app/src/main/java/com/lksnext/ParkingIMayordomo/
├── data/          # Modelos, Repositorios y Gestión de Datos (AuthManager)
├── ui/            # Pantallas (Pages), ViewModels y Componentes Reutilizables
├── utils/         # Lógica de validación, constantes de rutas y utilidades
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
