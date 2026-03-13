# AgentTasker - Android App

## Arquitectura

El proyecto sigue **Clean Architecture + MVVM** con inyección de dependencias mediante **Hilt**.

### Regla de dependencias

```
presentation → domain ← data
```

- **presentation** solo importa de **domain** (entidades, repositorios, use cases)
- **domain** no importa de ninguna otra capa
- **data** implementa las interfaces definidas en **domain**

### Estructura por feature

```
features/
├── login/
│   ├── data/
│   │   ├── datasources/local/    (SecureTokenStorage, SecureDataStoreTokenStorage)
│   │   ├── datasources/remote/   (AuthDTO, AuthMapper)
│   │   ├── repositories/         (AuthRepositoryImpl)
│   │   └── services/             (GoogleAuthService)
│   ├── di/                       (AuthModule - Hilt bindings)
│   ├── domain/
│   │   ├── entities/             (User, AuthToken)
│   │   ├── repositories/         (AuthRepository)
│   │   ├── services/             (GoogleAuthProvider)
│   │   ├── usecases/             (LoginUseCase, RegisterUseCase, SignInWithGoogleUseCase, SignOutUseCase, GetCurrentUserUseCase)
│   │   └── validators/           (AuthValidator)
│   ├── navigation/               (NavigationWrapperAuth)
│   └── presentation/
│       ├── components/           (LoginForms)
│       ├── screens/              (LoginScreen, LoginUiState)
│       └── viewmodel/            (LoginViewModel)
│
├── tasks/
│   ├── data/
│   │   ├── datasources/local/    (TaskDao, TaskReminderDao, TaskEntity, TaskReminderEntity, TaskEntityMapper)
│   │   ├── datasources/remote/   (TaskDTO, TaskMapper)
│   │   ├── repositories/         (TaskRepositoryImpl, TaskReminderRepositoryImpl)
│   │   └── workers/              (TaskSyncWorker, TaskSyncScheduler)
│   ├── di/                       (TaskModule)
│   ├── domain/
│   │   ├── entities/             (Task)
│   │   ├── repositories/         (TaskRepository, TaskReminderRepository)
│   │   └── usecases/             (GetTasksUseCase, CreateTaskUseCase, UpdateTaskUseCase, DeleteTaskUseCase)
│   ├── navigation/               (NavigationWrapperTasks)
│   └── presentation/
│       ├── components/           (TaskCard)
│       ├── screens/              (TaskScreen, TaskFormDialog, TaskUiState)
│       └── viewmodel/            (TaskViewModel)
│
├── classroom/
│   ├── data/
│   │   ├── datasources/local/    (ClassroomTaskDao, ClassroomTaskEntity)
│   │   ├── datasources/remote/   (ClassroomDTOs, ClassroomMapper)
│   │   ├── repositories/         (ClassroomRepositoryImpl)
│   │   └── services/             (ClassroomAuthService)
│   ├── di/                       (ClassroomModule)
│   ├── domain/
│   │   ├── entities/             (ClassroomCourse, ClassroomTask)
│   │   ├── repositories/         (ClassroomRepository)
│   │   └── usecases/             (GetClassroomCoursesUseCase, GetClassroomTasksUseCase, ConnectClassroomUseCase)
│   ├── navigation/               (NavigationWrapperClassroom)
│   └── presentation/
│       ├── components/           (ClassroomTaskCard, ConnectClassroomPrompt)
│       ├── screens/              (ClassroomScreen, ClassroomUiState)
│       └── viewmodel/            (ClassroomViewModel)
│
├── dashboard/
│   ├── navigation/               (NavigationWrapperDashboard)
│   └── presentation/
│       ├── components/           (DashboardCard)
│       ├── screens/              (DashboardScreen)
│       └── viewmodel/            (DashboardViewModel)
│
└── kanban/
    ├── data/
    │   ├── datasources/local/    (KanbanColumnDao, KanbanColumnEntity, KanbanColumnMapper)
    │   ├── datasources/remote/   (KanbanColumnDTO)
    │   └── repositories/         (KanbanRepositoryImpl)
    ├── di/                       (KanbanModule)
    ├── domain/
    │   ├── entities/             (KanbanColumn, KanbanItem)
    │   ├── repositories/         (KanbanRepository)
    │   └── usecases/             (ObserveKanbanColumnsUseCase, CreateKanbanColumnUseCase, UpdateKanbanColumnUseCase, DeleteKanbanColumnUseCase, ReorderKanbanColumnsUseCase)
    ├── navigation/               (NavigationWrapperKanban)
    └── presentation/
        ├── components/           (KanbanItemCard, ColumnFormDialog)
        ├── screens/              (KanbanScreen, KanbanUiState)
        └── viewmodel/            (KanbanViewModel)
```

### Core

```
core/
├── database/       (AppDatabase - Room)
├── di/             (DatabaseModule, NetworkModule, StorageModule)
├── domain/         (Result<T> sealed class)
├── hardware/       (NotificationHelper, ReminderScheduler, HapticFeedbackManager, BootReceiver)
├── navigation/     (Routes, FeatureNavGraph, NavigationModule)
├── network/        (AgentTaskerApi, AuthInterceptor, TokenAuthenticator, NetworkMonitor)
└── ui/
    ├── components/ (EmptyState, LoadingState, OfflineBanner, PriorityBadge, GoogleSignInButton, etc.)
    └── theme/      (Color, Theme, Type)
```

### Patrones clave

| Patrón | Implementación |
|--------|---------------|
| **MVVM** | ViewModels exponen `StateFlow<UiState>` (privado `MutableStateFlow`, público `StateFlow`) |
| **Clean Architecture** | Capas `data → domain ← presentation` sin imports cruzados |
| **Inyección de dependencias** | Hilt con `@Binds` para interfaces, `@Provides` para instancias concretas |
| **Offline-first** | Room como cache local + `isSynced`/`pendingAction` flags + WorkManager para sync |
| **Navegación type-safe** | Kotlin Serialization `@Serializable` routes + `FeatureNavGraph` + Hilt `@IntoSet` |
| **Result wrapper** | `core.domain.Result<T>` con `Success`, `Error`, `Loading` |

### Navegación

Cada feature registra sus rutas a través de `FeatureNavGraph`:

```kotlin
interface FeatureNavGraph {
    fun registerGraph(navGraphBuilder: NavGraphBuilder, navController: NavHostController)
}
```

Los wrappers se inyectan en `MainActivity` como `Set<FeatureNavGraph>` via Hilt multi-binding.

### Stack tecnológico

- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Persistencia:** Room + DataStore (encrypted)
- **Networking:** Retrofit + OkHttp + Gson
- **Auth:** Firebase Auth + Credential Manager + AppAuth (OAuth2/PKCE)
- **Background:** WorkManager con `@HiltWorker`
- **Navegación:** Navigation Compose con Kotlin Serialization
