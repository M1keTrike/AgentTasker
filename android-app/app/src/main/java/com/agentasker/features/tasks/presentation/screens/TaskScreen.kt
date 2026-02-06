package com.agentasker.features.tasks.presentation.screens

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentasker.core.ui.components.EmptyState
import com.agentasker.core.ui.components.LoadingState
import com.agentasker.features.tasks.presentation.components.TaskCard
import com.agentasker.features.tasks.presentation.viewmodel.TaskViewModel
import com.agentasker.features.tasks.presentation.viewmodel.TaskViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    factory: TaskViewModelFactory
) {
    val viewModel: TaskViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (uiState.showDialog) {
        TaskFormDialog(
            task = uiState.taskToEdit,
            title = uiState.formTitle,
            description = uiState.formDescription,
            priority = uiState.formPriority,
            onTitleChange = viewModel::updateFormTitle,
            onDescriptionChange = viewModel::updateFormDescription,
            onPriorityChange = viewModel::updateFormPriority,
            onDismiss = {
                viewModel.hideDialog()
            },
            onSave = { title, description, priority ->
                if (uiState.taskToEdit != null) {
                    viewModel.updateTask(uiState.taskToEdit!!.id, title, description, priority)
                } else {
                    viewModel.createTask(title, description, priority)
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

