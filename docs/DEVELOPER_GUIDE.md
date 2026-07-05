# Developer Guide - LKS Parking

This document describes the development workflow, architecture, coding standards, and best practices for contributing to **LKS Parking**.

---

## Index

* [Project Overview](#project-overview)
* [Architecture](#architecture)
* [Data Flow](#data-flow)
* [Project Structure](#project-structure)
* [Development Principles](#development-principles)
* [Coding Standards](#coding-standards)
* [Git Workflow](#git-workflow)
* [Testing](#testing)
* [CI/CD](#cicd)
* [Firebase](#firebase)
* [Performance Guidelines](#performance-guidelines)
* [Common Pitfalls](#common-pitfalls)
* [Useful Commands](#useful-commands)
* [AI Development Notes](#ai-development-notes)

---

## Project Overview

LKS Parking is an Android application developed with **Jetpack Compose** following a pure **MVVM architecture**.

The application allows employees to:
* Authenticate using their corporate account.
* Manage parking reservations.
* Register multiple vehicles.
* View parking occupancy in real time.
* Receive notifications.
* Report parking incidents.

The project prioritizes:
* Clean architecture
* Reactive programming
* Maintainability
* Testability
* Simplicity

---

## Architecture

The project follows a strict **MVVM (Model–View–ViewModel)** architecture.

```text
Compose UI (View)
      │
ViewModel
      │
Repository (Abstraction)
      │
AuthManager (Singleton / Data Source)
      │
Firebase (Firestore / Auth)
```

### Layers

#### UI (User Interface)
Responsible only for displaying data and capturing user events.
* Should be as "stateless" as possible.
* Never accesses Firebase directly.

#### ViewModels
Responsible for:
* Managing UI state exposed via immutable `StateFlow`.
* Calling repositories to perform operations.
* Handling presentation logic.
* ViewModels must never communicate directly with Firebase.

#### Repository
Acts as the abstraction layer between the UI and the data source.
* Hides Firebase implementation details.
* Exposes reactive data flows.

#### AuthManager
AuthManager is the core of the application state.
* Maintains synchronized application state using Firebase **Snapshot Listeners**.
* Whenever Firestore changes, the corresponding `StateFlow` is automatically updated.
* No polling should ever be implemented. The system is event-driven.

---

## Data Flow

The application follows a fully reactive, unidirectional data flow:

1. **Firestore** changes in the cloud.
2. **Snapshot Listener** in `AuthManager` detects the change.
3. `AuthManager` emits a new value in a `StateFlow`.
4. The **Repository** exposes this flow.
5. The **ViewModel** processes the data and updates the UI state.
6. The **Compose Screen** recompiles automatically.

---

## Project Structure

```text
app/src/main/java/com/lksnext/ParkingIMayordomo/
├── data/           # Models, repositories, and Firebase logic
│   ├── model/      # Data classes (POJOs)
│   └── repository/ # Repository implementations
├── ui/             # UI components and logic
│   ├── components/ # Reusable widgets (stateless)
│   ├── pages/      # Full-screen composables
│   ├── viewmodel/  # ViewModels and UI state
│   └── theme/      # Style, colors, and typography
├── utils/          # Constants, LocaleManager, and helpers
└── MainActivity.kt # Entry point and Navigation Host
```

---

## Development Principles

The following rules should always be respected:

* **UI**: Never access Firebase directly. Keep screens stateless whenever possible.
* **ViewModels**: Expose immutable `StateFlow`. Never expose `MutableStateFlow` publicly. Keep business logic here.
* **Repository**: Abstract Firebase completely. Reuse existing methods whenever possible.
* **Firebase**: Access Firebase only through repositories or `AuthManager`. Never from Compose.

---

## Coding Standards

### Naming Conventions
* **Classes/Objects**: `PascalCase` (e.g., `ReservationViewModel`).
* **Functions/Variables**: `camelCase` (e.g., `updateReservation`).
* **Composables**: `PascalCase` (e.g., `ParkingMap`).
* **Constants**: `SCREAMING_SNAKE_CASE` (e.g., `MAX_RESERVATION_DAYS`).

### Static Analysis
We use **Detekt** to ensure code quality. Before pushing changes, run:
```bash
./gradlew detekt
```
Code smells and complexity warnings should be resolved.

---

## Git Workflow

### Branch naming
* `main`: Stable production-ready code.
* `feature/feature-name`: New features.
* `fix/fix-name`: Bug fixes.
* `docs/document-name`: Documentation updates.

### Commits (Conventional Commits)
* `feat:` for new features.
* `fix:` for bug fixes.
* `docs:` for documentation updates.
* `refactor:` for code changes that neither fix a bug nor add a feature.

---

## Testing

Testing is a core part of the project.
* **Unit Tests (`app/src/test`)**: We use `MockK` and `Turbine` to validate ViewModel logic and `StateFlow` emissions. Every ViewModel should have unit tests.
* **Instrumented Tests (`app/src/androidTest`)**: We use Compose Test rules to validate UI flows.

---

## CI/CD

Every Pull Request should successfully complete the pipeline:
1. **Build**
2. **Unit Tests**
3. **Instrumented Tests**
4. **Detekt**
5. **JaCoCo Coverage**
6. **APK Generation**

---

## Firebase

Current services used:
* **Authentication**: Email/Password.
* **Cloud Firestore**: NoSQL Database.
* **Cloud Messaging**: Push notifications.
* **Crashlytics**: Error reporting.
* **Performance Monitoring**: App performance metrics.

---

## Performance Guidelines

* **Recomposition**: Minimize recompositions using `remember` and `derivedStateOf`.
* **Immutability**: Prefer immutable state and collections.
* **Data Fetching**: Never perform Firestore queries inside composables.

---

## Common Pitfalls

* ❌ Accessing Firebase from Compose.
* ❌ Launching coroutines unnecessarily.
* ❌ Duplicating application state.
* ❌ Exposing `MutableStateFlow` publicly.
* ❌ Adding business logic inside UI.

---

## Useful Commands

| Action | Command |
|--------|---------|
| Run Unit Tests | `./gradlew test` |
| Run UI Tests | `./gradlew connectedDebugAndroidTest` |
| Run Detekt | `./gradlew detekt` |
| Generate Coverage | `./gradlew jacocoTestReport` |
| Generate Debug APK | `./gradlew assembleDebug` |

---

## AI Development Notes

When working with AI coding assistants:
1. **Preserve the MVVM architecture**.
2. **Reuse existing repositories**; do not duplicate logic.
3. **Keep composables stateless**.
4. **Add tests** for any new ViewModel logic.
5. Keep changes consistent with the project style.
