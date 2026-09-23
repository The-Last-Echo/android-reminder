# Architecture

This project follows a clean architecture with MVI (Model-View-Intent) pattern and unidirectional data flow.

## Project Structure

```
app/src/main/java/com/thelastecho/reminder/
├── presentation/          # UI Layer
│   ├── home/             # Home screen (reminder list)
│   ├── editor/           # Reminder creation/editing
│   ├── settings/         # Settings screens
│   ├── components/       # Reusable UI components
│   └── navigation/       # Navigation graph
├── domain/               # Business Logic Layer
│   ├── model/            # Domain models
│   ├── usecase/          # Business use cases
│   └── repository/       # Repository interfaces
├── data/                 # Data Layer
│   ├── local/            # Local data (Room database)
│   └── repository/       # Repository implementations
└── core/                 # Core/Infrastructure
    ├── alarm/            # Alarm scheduling
    ├── notification/     # Notification management
    ├── preferences/      # User preferences
    └── designsystem/     # UI theming
```

## Layers

### Presentation Layer
- **Screens**: Compose UI screens that observe ViewModels
- **ViewModels**: Handle UI intents, emit UI states, coordinate use cases
- **Components**: Reusable Compose components
- **Navigation**: Navigation graph for screen routing

### Domain Layer
- **Models**: Pure data classes representing business entities
- **Use Cases**: Single responsibility business operations
- **Repository Interfaces**: Abstract data access contracts

### Data Layer
- **Entities**: Database entities mapped to domain models
- **DAOs**: Room database access objects
- **Repository Implementations**: Concrete implementations of repository interfaces

### Core Layer
- **Alarm Scheduling**: Android AlarmManager integration
- **Notification Management**: Android notification system
- **Preferences**: DataStore for user settings
- **Design System**: Material 3 theming and styling

## MVI Pattern

The application uses the MVI pattern with unidirectional data flow:

1. **Intent**: User actions (e.g., click, input)
2. **ViewModel**: Processes intents, coordinates use cases
3. **State**: Immutable UI state emitted by ViewModel
4. **Effect**: One-time events (navigation, snackbar)

### Example Flow

```
User Action → Intent → ViewModel → Use Case → Repository → Database
                                                      ↓
                                              UI State ← ViewModel
```

## Data Flow

- **Bottom-up**: Database → Repository → Use Case → ViewModel → UI
- **Top-down**: User → Intent → ViewModel → Use Case → Repository → Database

## Key Principles

1. **Separation of Concerns**: Each layer has distinct responsibilities
2. **Dependency Inversion**: Domain layer doesn't depend on data layer
3. **Single Source of Truth**: Database is the primary data source
4. **Immutability**: UI states are immutable
5. **Reactive Programming**: Uses Kotlin Flow for data streams

## Technology Stack

- **Language**: Kotlin 2.1.0
- **UI**: Jetpack Compose + Material 3
- **Database**: Room (SQLite)
- **Async**: Coroutines + Flow
- **DI**: Manual (no framework)
- **Preferences**: DataStore
