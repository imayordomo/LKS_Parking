# LKS Parking — Command Reference

This document contains the most useful commands for development, testing, and maintenance of the project.

## Build & Installation

| Command | Description |
|---------|-------------|
| `.\gradlew.bat clean` | Deletes the `build` folder and temporary files. |
| `.\gradlew.bat assembleDebug` | Generates the debug APK in `app/build/outputs/apk/debug/`. |
| `.\gradlew.bat installDebug` | Compiles and installs the app on the connected device. |
| `.\gradlew.bat bundleRelease` | Generates the Android App Bundle (.aab) for production. |

## Testing & Coverage

### Unit Tests (Local)
| Command | Description |
|---------|-------------|
| `.\gradlew.bat testDebugUnitTest` | Runs all unit tests for the logic layer. |
| `.\gradlew.bat testDebugUnitTest --tests "*ViewModelTest*"` | Runs only tests that match the pattern. |
| `.\gradlew.bat testDebugUnitTest --rerun-tasks` | Forces test execution by ignoring cache. |

### Instrumentation Tests (Device)
| Command | Description |
|---------|-------------|
| `.\gradlew.bat connectedDebugAndroidTest` | Runs UI tests on an emulator or real device. |

### Coverage Reports
| Command | Description |
|---------|-------------|
| `.\gradlew.bat jacocoTestReport` | Generates the JaCoCo coverage report (HTML/XML) in `app/build/reports/jacoco/`. |

## Quality & Static Analysis

| Command | Description |
|---------|-------------|
| `.\gradlew.bat detekt` | Runs style analysis and checks for "code smells" using Detekt. |
| `.\gradlew.bat lintDebug` | Runs the official Android Lint analysis. |
| `.\gradlew.bat sonar` | Uploads analysis and coverage results to **SonarCloud**. |

## 🛠Development Utilities

### Database Seeding (Firestore)
To populate the database with test data (users, vehicles, and reservations):
```bash
cd scripts
npm install                     # First time only
node seed.js                    # Populate (prevents duplicates)
node seed.js --clear            # Delete everything and repopulate
```
*Requires `serviceAccountKey.json` in the `scripts/` folder.*

### Gradle & Dependency Management
| Command                                   | Description                                   |
|-------------------------------------------|-----------------------------------------------|
| `.\gradlew.bat :app:dependencies`         | Shows the full dependency tree.               |
| `.\gradlew.bat --stop`                    | Stops the Gradle daemon (useful if it hangs). |
| `.\gradlew.bat cleanBuildCache`           | Clears the Gradle build cache.                |

## Useful ADB Commands

| Command | Description |
|---------|-------------|
| `adb devices` | Lists connected devices. |
| `adb shell pm clear com.lksnext.ParkingIMayordomo` | Clears all local app data (Total reset). |
| `adb shell am force-stop com.lksnext.ParkingIMayordomo` | Force stops the application. |
| `adb logcat *:E` | Shows only errors in system logs. |

---

*Note: If you use a Unix-based terminal (Git Bash, WSL, Mac), replace `.\gradlew.bat` with `./gradlew`.*
