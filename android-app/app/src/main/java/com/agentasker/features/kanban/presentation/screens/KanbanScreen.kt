package com.agentasker.features.kanban.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.agentasker.features.tasks.presentation.service.TaskSyncService
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agentasker.core.ui.components.EmptyState
import com.agentasker.core.ui.components.LoadingState
import com.agentasker.features.kanban.domain.entities.KanbanColumn
import com.agentasker.features.kanban.domain.entities.KanbanItem
import com.agentasker.features.kanban.presentation.components.ColumnFormDialog
import com.agentasker.features.kanban.presentation.components.KanbanItemCard
import com.agentasker.features.kanban.presentation.viewmodel.KanbanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(
    viewModel: KanbanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (uiState.showColumnDialog) {
        ColumnFormDialog(
            isEditing = uiState.columnToEdit != null,
            title = uiState.formTitle,
            statusKey = uiState.formStatusKey,
            color = uiState.formColor,
            onTitleChange = viewModel::updateFormTitle,
            onStatusKeyChange = viewModel::updateFormStatusKey,
            onColorChange = viewModel::updateFormColor,
            onDismiss = viewModel::hideColumnDialog,
            onSave = viewModel::saveColumn
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tablero Kanban", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.startTaskSyncService() },
                        enabled = uiState.syncState !is TaskSyncService.SyncState.Running
                    ) {
                        when (uiState.syncState) {
                            is TaskSyncService.SyncState.Running -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(6.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is TaskSyncService.SyncState.Error -> {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Error de sincronización",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            is TaskSyncService.SyncState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Sincronización completada"
                                )
                            }
                            TaskSyncService.SyncState.Idle -> {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sincronizar tareas"
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateColumnDialog() }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar columna"
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
                uiState.isLoading && uiState.columns.isEmpty() -> {
                    LoadingState()
                }
                uiState.columns.isEmpty() && !uiState.isLoading -> {
                    EmptyState(
                        message = "No hay columnas.\nAgrega una para comenzar.",
                        icon = Icons.Outlined.ViewColumn
                    )
                }
                else -> {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.columns,
                            key = { it.id }
                        ) { column ->
                            KanbanColumnView(
                                column = column,
                                items = uiState.tasksByStatus[column.statusKey] ?: emptyList(),
                                onEditColumn = { viewModel.showEditColumnDialog(column) },
                                onDeleteColumn = { viewModel.deleteColumn(column.id) },
                                allColumns = uiState.columns,
                                onMoveTask = { item, newStatus -> viewModel.moveTask(item, newStatus) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KanbanColumnView(
    column: KanbanColumn,
    items: List<KanbanItem>,
    onEditColumn: () -> Unit,
    onDeleteColumn: () -> Unit,
    allColumns: List<KanbanColumn>,
    onMoveTask: (KanbanItem, String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val columnColor = column.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) }
        catch (_: Exception) { MaterialTheme.colorScheme.primaryContainer }
    } ?: MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
                .background(columnColor.copy(alpha = 0.7f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${items.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = {
                        showMenu = false
                        onEditColumn()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    onClick = {
                        showMenu = false
                        onDeleteColumn()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin tareas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(
                    items = items,
                    key = { "${it.type}_${it.id}" }
                ) { item ->
                    var showMoveMenu by remember { mutableStateOf(false) }

                    Box {
                        KanbanItemCard(
                            item = item,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (item is KanbanItem.TaskItem) {
                                        showMoveMenu = true
                                    }
                                }
                            )
                        )

                        DropdownMenu(
                            expanded = showMoveMenu,
                            onDismissRequest = { showMoveMenu = false }
                        ) {
                            Text(
                                text = "Mover a:",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                            allColumns.filter { it.statusKey != column.statusKey }.forEach { targetColumn ->
                                DropdownMenuItem(
                                    text = { Text(targetColumn.title) },
                                    onClick = {
                                        showMoveMenu = false
                                        onMoveTask(item, targetColumn.statusKey)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
