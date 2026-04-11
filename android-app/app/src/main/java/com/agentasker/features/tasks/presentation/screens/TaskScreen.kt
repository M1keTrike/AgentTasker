package com.agentasker.features.tasks.presentation.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agentasker.core.ui.components.EmptyState
import com.agentasker.core.ui.components.LoadingState
import com.agentasker.features.tasks.presentation.components.TaskCard
import com.agentasker.features.tasks.presentation.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearInfoMessage()
        }
    }

    if (uiState.showDialog) {
        TaskFormDialog(
            task = uiState.taskToEdit,
            title = uiState.formTitle,
            description = uiState.formDescription,
            priority = uiState.formPriority,
            reminderAt = uiState.formReminderAt,
            onTitleChange = viewModel::updateFormTitle,
            onDescriptionChange = viewModel::updateFormDescription,
            onPriorityChange = viewModel::updateFormPriority,
            onReminderAtChange = viewModel::updateFormReminderAt,
            onDismiss = {
                viewModel.hideDialog()
            },
            onSave = { title, description, priority, reminderAt ->
                if (uiState.taskToEdit != null) {
                    viewModel.updateTask(uiState.taskToEdit!!.id, title, description, priority, uiState.formStatus, uiState.formDueDate, reminderAt)
                } else {
                    viewModel.createTask(title, description, priority, uiState.formStatus, uiState.formDueDate, reminderAt)
                }
                viewModel.hideDialog()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Tareas", fontWeight = FontWeight.ExtraBold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.showCreateDialog()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear tarea"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.tasks.isEmpty() -> {
                    LoadingState()
                }
                uiState.tasks.isEmpty() && !uiState.isLoading -> {
                    EmptyState(
                        message = "No hay tareas.\n¡Crea una nueva!",
                        icon = Icons.Outlined.TaskAlt
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(
                            items = uiState.tasks,
                            key = { it.id }
                        ) { task ->
                            TaskCard(
                                task = task,
                                onEdit = {
                                    viewModel.showEditDialog(task)
                                },
                                onDelete = {
                                    viewModel.deleteTask(task.id)
                                },
                                onSplitWithAi = { viewModel.splitWithAi(task.id) },
                                onToggleSubtask = { viewModel.toggleSubtask(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
