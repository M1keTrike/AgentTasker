package com.agentasker.features.tasks.presentation.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio de sincronización de tareas.
 *
 * Implementa los dos patrones del `Services.pdf` en una sola clase:
 *
 *  1. **Started (Background) Service** – arrancado con `startService(...)`.
 *     Operación fire-and-forget: ejecuta la sincronización y se autodestruye
 *     con `stopSelf()` al terminar. Devuelve `START_NOT_STICKY`.
 *
 *  2. **Bound Service** – vinculado con `bindService(...)`. Devuelve un
 *     [LocalBinder] que expone:
 *        - `syncState: StateFlow<SyncState>` para que la UI observe el progreso.
 *        - `triggerSync()` para disparar una sincronización desde el cliente.
 *     El service vive mientras haya clientes ligados; cuando todos hacen
 *     `unbindService`, el sistema lo destruye.
 *
 * Flujo Clean Architecture:
 *   KanbanScreen → KanbanViewModel → bindService / triggerSync
 *      → TaskSyncService → TaskRepository (domain) → DataSource
 *
 * Limitaciones (API 26+):
 *  - Como Started Service solo puede arrancarse con la app visible.
 *  - Como Bound Service no tiene esa restricción si el cliente que bindea
 *    es un `Activity`/`Service`/`ContentProvider` en estado válido.
 */
@AndroidEntryPoint
class TaskSyncService : Service() {

    @Inject
    lateinit var taskRepository: TaskRepository

    // ───────── Estado observable por clientes ligados ─────────
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // ───────── Binder (patrón Local Binder) ─────────
    private val binder = LocalBinder()

    /**
     * Binder local: solo funciona si el cliente está en el mismo proceso
     * (que es nuestro caso). Expone la instancia del servicio directamente.
     */
    inner class LocalBinder : Binder() {
        fun getService(): TaskSyncService = this@TaskSyncService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        // false = no queremos recibir onRebind si se vuelve a ligar.
        return false
    }

    // ───────── Scope de corrutinas del servicio ─────────
    private val serviceJob: Job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    /**
     * Entrada para el patrón Started Service.
     * También permite que un broadcast/WorkManager dispare la sincronización
     * sin necesidad de estar ligado.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf(startId)
                return START_NOT_STICKY
            }
            else -> {
                triggerSync(onFinished = { stopSelf(startId) })
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Dispara una sincronización. Expuesto vía [LocalBinder] para clientes
     * ligados y también usado internamente por `onStartCommand`.
     *
     * - Reentrante: si ya hay una sincronización en curso, no hace nada.
     * - `onFinished` permite al caller (ej. Started Service) detener el
     *   servicio al terminar. Los clientes ligados lo pasan como null.
     */
    fun triggerSync(onFinished: (() -> Unit)? = null) {
        if (_syncState.value is SyncState.Running) {
            Log.d(TAG, "triggerSync ignorado: ya hay una sincronización en curso")
            onFinished?.invoke()
            return
        }

        serviceScope.launch {
            _syncState.value = SyncState.Running
            try {
                taskRepository.refreshTasks()
                _syncState.value = SyncState.Success(System.currentTimeMillis())
                Log.d(TAG, "Sincronización completada")
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error desconocido")
                Log.e(TAG, "Error durante la sincronización", e)
            } finally {
                onFinished?.invoke()
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Estados publicados por el servicio. */
    sealed class SyncState {
        data object Idle : SyncState()
        data object Running : SyncState()
        data class Success(val finishedAt: Long) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    companion object {
        private const val TAG = "TaskSyncService"
        const val ACTION_START = "com.agentasker.action.SYNC_START"
        const val ACTION_STOP = "com.agentasker.action.SYNC_STOP"

        /** Arranca el servicio en modo Started (fire-and-forget). */
        fun startSync(context: Context) {
            val intent = Intent(context, TaskSyncService::class.java).apply {
                action = ACTION_START
            }
            try {
                context.startService(intent)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "No se pudo iniciar el service: ${e.message}")
            }
        }

        /** Bindea el servicio. Devuelve true si el bind fue aceptado. */
        fun bind(context: Context, connection: ServiceConnection): Boolean {
            val intent = Intent(context, TaskSyncService::class.java)
            return context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun unbind(context: Context, connection: ServiceConnection) {
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {
                // Ya estaba desbindeado; ignorar.
                Log.w(TAG, "unbind ignorado: ${e.message}")
            }
        }
    }
}

/**
 * Helper para consumir [TaskSyncService] desde un ViewModel sin que éste
 * tenga que implementar [ServiceConnection] directamente.
 *
 * Uso:
 *   val connection = TaskSyncServiceConnection(appContext)
 *   connection.bind()                         // llamado en init
 *   connection.syncState.collect { … }        // flow combinado
 *   connection.triggerSync()                  // botón
 *   connection.unbind()                       // onCleared
 */
class TaskSyncServiceConnection(
    private val context: Context
) : ServiceConnection {

    private var service: TaskSyncService? = null

    private val _syncState = MutableStateFlow<TaskSyncService.SyncState>(
        TaskSyncService.SyncState.Idle
    )
    val syncState: StateFlow<TaskSyncService.SyncState> = _syncState.asStateFlow()

    private var collectorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected(name: ComponentName?, binderIBinder: IBinder?) {
        val localBinder = binderIBinder as? TaskSyncService.LocalBinder ?: return
        val svc = localBinder.getService()
        service = svc
        // Replicamos el StateFlow del servicio al del connection
        collectorJob?.cancel()
        collectorJob = scope.launch {
            svc.syncState.collect { _syncState.value = it }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        collectorJob?.cancel()
        collectorJob = null
        service = null
    }

    fun bind(): Boolean = TaskSyncService.bind(context, this)

    fun unbind() {
        collectorJob?.cancel()
        collectorJob = null
        service = null
        TaskSyncService.unbind(context, this)
    }

    fun triggerSync() {
        service?.triggerSync() ?: run {
            // Si el bind aún no se estableció, caemos al patrón Started.
            TaskSyncService.startSync(context)
        }
    }
}
