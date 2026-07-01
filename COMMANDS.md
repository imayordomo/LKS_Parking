# LKS Parking — Comandos Referencia

## Build & Instalación

| Comando | Descripción |
|---------|-------------|
| `.\gradlew.bat assembleDebug` | Compila APK debug |
| `.\gradlew.bat assembleRelease` | Compila APK release (firmado) |
| `.\gradlew.bat clean` | Limpia builds anteriores |
| `.\gradlew.bat --no-configuration-cache clean` | Limpia sin caché de configuración |

## Tests

### Unit tests (JVM — rápidos)
| Comando | Descripción |
|---------|-------------|
| `.\gradlew.bat testDebugUnitTest` | Ejecuta todos los tests unitarios |
| `.\gradlew.bat testDebugUnitTest --rerun-tasks` | Fuerza re-ejecución (ignora caché) |
| `.\gradlew.bat testDebugUnitTest --tests "*DashboardViewModelTest*"` | Test específico |

### Instrumentation tests (requieren emulador/dispositivo)
| Comando | Descripción |
|---------|-------------|
| `.\gradlew.bat connectedDebugAndroidTest` | Ejecuta todos los tests de instrumentación |
| `.\gradlew.bat assembleDebugAndroidTest` | Solo compila el APK de test (sin ejecutar) |

### Ambos
| Comando | Descripción |
|---------|-------------|
| `.\gradlew.bat testDebugUnitTest connectedDebugAndroidTest` | Unit + instrumentation |

## Cobertura & Calidad

| Comando | Descripción |
|---------|-------------|
| `.\gradlew.bat jacocoTestReport` | Genera reporte JaCoCo (`build/reports/jacoco/`) |
| `.\gradlew.bat detekt` | Análisis estático con Detekt |
| `.\gradlew.bat sonar` | Sube a SonarCloud (requiere `SONAR_TOKEN`) |

## Seed de Firestore

Poblar la base de datos con datos de prueba (60 usuarios, vehículos, 100-150 reservas/día).

```bash
cd scripts
npm install                     # Solo la primera vez
node seed.js                    # Poblar (aborta si ya hay datos)
node seed.js --clear            # Borrar y repoblar desde cero
```

**Requisito:** Colocar `serviceAccountKey.json` en `scripts/` (Firebase Console > Ajustes > Cuentas de servicio > Generar clave privada).

Credenciales de usuarios generados: `{nombre}.{apellido}{N}@lksnext.com` / `Test1234`

## Depuración

| Comando | Descripción |
|---------|-------------|
| `adb devices` | Lista dispositivos/emuladores conectados |
| `.\gradlew.bat lint` | Análisis de código Android Lint |
| `.\gradlew.bat build --scan` | Build con build scan (Gradle Enterprise) |

## Gradle

| Comando | Descripción |
|---------|-------------|
| `.\gradlew.bat --version` | Versión de Gradle |
| `.\gradlew.bat :app:dependencies` | Árbol de dependencias |
| `.\gradlew.bat --no-configuration-cache <task>` | Ejecuta tarea sin caché de configuración |

## Proyecto

| Ruta | Propósito |
|------|-----------|
| `app/src/main/java/` | Código fuente principal |
| `app/src/test/java/` | Tests unitarios (JVM) |
| `app/src/androidTest/java/` | Tests de instrumentación |
| `scripts/seed.js` | Script de seed para Firestore |
| `gradle/libs.versions.toml` | Catálogo de versiones |
