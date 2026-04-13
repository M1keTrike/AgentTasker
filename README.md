# AgentTasker

Aplicacion de gestion de tareas con integracion de Google Classroom e inteligencia artificial para Android.

## Stack tecnologico

### Android
- **Lenguaje:** Kotlin 2.x
- **UI:** Jetpack Compose + Material Theme 3.0
- **Arquitectura:** MVVM + Clean Architecture
- **DI:** Hilt
- **Base de datos local:** Room (offline-first)
- **Red:** Retrofit + OkHttp + Gson
- **Background:** WorkManager (expedited + foreground) + Started/Bound Services
- **Push:** Firebase Cloud Messaging (FCM)
- **IA:** DeepSeek API (chat completions, JSON mode)
- **OCR:** Google ML Kit Text Recognition (on-device)
- **Imagenes:** Coil Compose
- **Auth:** Credential Manager (Google Sign-In) + AppAuth (Classroom OAuth2 PKCE)
- **Seguridad:** Tink (encrypted DataStore)

### Backend
- **Runtime:** Node.js + NestJS
- **ORM:** TypeORM + PostgreSQL
- **Auth:** JWT (access + refresh tokens)
- **Push:** firebase-admin (FCM HTTP v1)
- **Cron:** @nestjs/schedule (recordatorios cada minuto)
- **Classroom:** google-auth-library (OAuth2 + Classroom API v1)
- **Despliegue:** Docker Compose (backend + postgres)

## Arquitectura

```
android-app/
  app/src/main/java/com/agentasker/
    core/
      ai/              DeepSeek client (Api, Interceptor, Service, Prompts)
      database/        Room AppDatabase (v9, 5 entidades)
      di/              Hilt modules (Network, Database, AI, Storage)
      hardware/        NotificationHelper, HapticFeedback, ReminderScheduler
      navigation/      Routes, FeatureNavGraph, NavigationModule
      network/         AgentTaskerApi, AuthInterceptor, TokenAuthenticator
      notifications/   FCM Service, DeepLink, FcmTokenRepository
      ui/              Theme, componentes reutilizables
    features/
      login/           Login + Register + Google Sign-In
      dashboard/       Panel de estado + Integraciones Classroom + Archivados
      tasks/           CRUD tasks + subtasks + IA split + sync offline-first
      kanban/          Tablero drag-and-drop + color picker
      classroom/       OAuth Classroom + sync selectivo por curso
      analyzer/        Foto a tarea (camara/galeria + OCR + DeepSeek)

backend-api/src/
  tasks/               CRUD + findArchived + subtasks eager load
  subtasks/            CRUD + bulk create (para IA)
  users/               Register + Login + JWT + FCM token
  auth/                Google OAuth + Classroom OAuth + refresh tokens
  classroom/           Courses + Tasks + Submissions (cache TTL 5min)
  kanban/              Columns CRUD + reorder
  notifications/       FCM push service + cron de recordatorios
```

## Funcionalidades principales

### Gestion de tareas
- Crear, editar, eliminar tareas con titulo, descripcion, prioridad y fecha
- Subtareas manuales (agregar, editar, eliminar desde el dialog de edicion)
- Recordatorios locales (AlarmManager) + push remoto (FCM cron)
- Archivar tareas completadas (boton "Completar y archivar" al terminar todas las subtasks)
- Seccion de archivados en el Dashboard con restaurar y eliminar permanente

### Inteligencia artificial (DeepSeek)
- **Dividir con IA:** descompone una tarea en 3-8 subtareas accionables a partir de su descripcion
- **Analyzer:** toma una foto, extrae texto con ML Kit OCR on-device, y DeepSeek genera titulo, descripcion, prioridad y subtareas
- Ambos flujos corren en WorkManager expedited con foreground notification, sobreviven al kill de la app
- Si la tarea ya tenia subtareas y se vuelve a dividir con IA, las anteriores se sobreescriben

### Google Classroom
- Conexion OAuth2 con PKCE desde el Dashboard
- Selector de cursos: el usuario elige de que cursos sincronizar
- Solo importa tareas pendientes (NEW, CREATED, RECLAIMED_BY_STUDENT)
- Las tareas se mapean al modelo local con badge verde "Classroom"
- Cache TTL 5 minutos en el backend para evitar rate limiting (429)
- Las tareas de Classroom se pueden editar, archivar y eliminar localmente
- Re-sync respeta el estado local (isArchived, status, delete)

### Kanban
- Columnas personalizables con color picker (12 swatches preset)
- Drag-and-drop de tarjetas entre columnas (long-press + arrastrar)
- Sincronizacion con el backend (columnas + status de tareas)

### Offline-first
- Todas las operaciones escriben primero en Room con `pendingAction`
- TaskSyncWorker sincroniza al recuperar red (CONNECTED constraint + exponential backoff)
- Tasks, subtasks, y archivados se sincronizan al backend
- Classroom tasks se crean en el backend automaticamente tras el sync

### Notificaciones
- Push FCM para recordatorios (cron cada minuto en el backend)
- Notificaciones locales de progreso IA (canal LOW, silenciosas)
- Deep links: tocar la notificacion navega al tab correcto
- Recuperacion de sesion: si el backend reinicia, la app detecta el 401 y redirige a login

## Configuracion

### Requisitos
- Android Studio con Kotlin 2.x y AGP 8.13+
- Node.js 18+ y pnpm
- Docker y Docker Compose
- Cuenta de Google Cloud con Classroom API habilitada
- API key de DeepSeek

### Variables de entorno

**Android** (`android-app/local.properties`):
```properties
api.base.url=https://tu-dominio.com/
google.web.client.id=TU_CLIENT_ID.apps.googleusercontent.com
deepseek.api.key=sk-tu-key-de-deepseek
```

**Backend** (`backend-api/.env`):
```env
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=agentasker
DB_PASSWORD=tu_password
DB_NAME=agentasker
JWT_SECRET=tu_jwt_secret
JWT_EXPIRES_IN=86400
GOOGLE_CLIENT_ID=TU_CLIENT_ID.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu_client_secret
```

**Firebase** (`backend-api/fcm-service-account.json`):
Descargar desde Firebase Console > Project Settings > Service Accounts.

### Build

```bash
# Backend
cd backend-api
pnpm install
pnpm run build
docker compose up --build -d

# Android
cd android-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Base de datos

### Room (Android) - 5 entidades
| Tabla | Campos clave |
|-------|-------------|
| `tasks` | id, title, description, priority, status, dueDate, source, externalId, courseName, isArchived, isSynced, pendingAction |
| `subtasks` | id, taskId (FK CASCADE), title, isCompleted, position, isSynced, pendingAction |
| `kanban_columns` | id, title, statusKey, color, position |
| `classroom_tasks` | id, courseId, courseName, title, submissionState (legacy, pre-merge) |
| `task_reminders` | id, taskId, title, body, reminderAt |

### PostgreSQL (Backend) - mismas tablas principales
| Tabla | Relaciones |
|-------|-----------|
| `users` | hasMany tasks, hasOne classroom_data |
| `tasks` | belongsTo user, hasMany subtasks |
| `subtasks` | belongsTo task (CASCADE) |
| `kanban_columns` | belongsTo user |
| `classroom_data` | belongsTo user (tokens OAuth Google) |

## Endpoints del API

### Tasks
| Metodo | Ruta | Descripcion |
|--------|------|------------|
| GET | /tasks | Listar tasks activas (no archivadas) |
| GET | /tasks/archived | Listar tasks archivadas |
| GET | /tasks/:id | Obtener task con subtasks |
| POST | /tasks | Crear task |
| PATCH | /tasks/:id | Actualizar task |
| DELETE | /tasks/:id | Eliminar task |

### Subtasks
| Metodo | Ruta | Descripcion |
|--------|------|------------|
| GET | /tasks/:taskId/subtasks | Listar subtasks |
| POST | /tasks/:taskId/subtasks | Crear subtask |
| POST | /tasks/:taskId/subtasks/bulk | Crear multiples (usado por IA) |
| PATCH | /subtasks/:id | Actualizar subtask |
| DELETE | /subtasks/:id | Eliminar subtask |

### Auth
| Metodo | Ruta | Descripcion |
|--------|------|------------|
| POST | /users/login | Login username/password |
| POST | /users/register | Registro |
| POST | /auth/google | Google Sign-In |
| POST | /auth/refresh | Refresh token |
| POST | /auth/google-classroom | Conectar Classroom (OAuth) |

### Classroom
| Metodo | Ruta | Descripcion |
|--------|------|------------|
| GET | /classroom/status | Verificar conexion |
| GET | /classroom/courses | Listar cursos activos |
| GET | /classroom/courses/:id/tasks | Tasks de un curso |
| GET | /classroom/tasks | Todas las tasks (cache 5min) |

### Kanban
| Metodo | Ruta | Descripcion |
|--------|------|------------|
| GET | /kanban/columns | Listar columnas |
| POST | /kanban/columns | Crear columna |
| PATCH | /kanban/columns/:id | Actualizar columna |
| DELETE | /kanban/columns/:id | Eliminar columna |
| PATCH | /kanban/columns/reorder | Reordenar columnas |

## Hardware utilizado

| Hardware | Uso | Archivo |
|----------|-----|---------|
| Vibracion | Haptic feedback en acciones (success, warning, notification) | `HapticFeedbackManager.kt` |
| Camara | Captura de fotos para el Analyzer | `AnalyzerScreen.kt` (TakePicture) |
| Conectividad | Deteccion online/offline en tiempo real | `ConnectivityManagerNetworkMonitor.kt` |
| AlarmManager | Recordatorios locales programados | `ReminderScheduler.kt` |
| NotificationManager | 4 canales con importancias diferenciadas | `NotificationHelper.kt` |

## Licencia

Proyecto academico - Universidad Politecnica de Chiapas - Ingenieria en Software.
