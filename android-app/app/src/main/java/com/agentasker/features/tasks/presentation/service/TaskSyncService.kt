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

@AndroidEntryPoint
class TaskSyncService : Service() {

    @Inject
    lateinit var taskRepository: TaskRepository

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TaskSyncService = this@TaskSyncService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return false
    }

    private val serviceJob: Job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

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

        fun bind(context: Context, connection: ServiceConnection): Boolean {
            val intent = Intent(context, TaskSyncService::class.java)
            return context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun unbind(context: Context, connection: ServiceConnection) {
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "unbind ignorado: ${e.message}")
            }
        }
    }
}

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
            TaskSyncService.startSync(context)
        }
    }
}
