package com.agentasker.features.kanban.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import com.agentasker.features.tasks.presentation.service.TaskSyncService

/**
 * Estado global del drag. Se mantiene fuera del UiState del VM porque es
 * puramente visual (offset del dedo) y re-emitirlo a 60fps vía Flow sería
 * un desperdicio.
 */
private class DragState {
    var draggingItem: KanbanItem? by mutableStateOf(null)
    var pointer: Offset by mutableStateOf(Offset.Zero)
    var sourceStatus: String? by mutableStateOf(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(
    viewModel: KanbanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Bounds de cada columna en coordenadas de root para resolver el drop.
    val columnBounds = remember { mutableStateMapOf<String, Rect>() }
    val dragState = remember { DragState() }

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
                            val isHoverTarget = dragState.draggingItem != null &&
                                dragState.sourceStatus != column.statusKey &&
                                columnBounds[column.statusKey]?.contains(dragState.pointer) == true

                            KanbanColumnView(
                                column = column,
                                items = uiState.tasksByStatus[column.statusKey] ?: emptyList(),
                                isHoverTarget = isHoverTarget,
                                onBoundsChanged = { rect ->
                                    columnBounds[column.statusKey] = rect
                                },
                                onEditColumn = { viewModel.showEditColumnDialog(column) },
                                onDeleteColumn = { viewModel.deleteColumn(column.id) },
                                onStartDrag = { item, pointer ->
                                    dragState.draggingItem = item
                                    dragState.sourceStatus = column.statusKey
                                    dragState.pointer = pointer
                                },
                                onDragMove = { pointer ->
                                    dragState.pointer = pointer
                                },
                                onDragEnd = {
                                    val dragged = dragState.draggingItem
                                    val target = columnBounds.entries
                                        .firstOrNull { (_, rect) -> rect.contains(dragState.pointer) }
                                        ?.key
                                    if (dragged is KanbanItem.TaskItem &&
                                        target != null &&
                                        target != dragState.sourceStatus
                                    ) {
                                        viewModel.moveTask(dragged, target)
                                    }
                                    dragState.draggingItem = null
                                    dragState.sourceStatus = null
                                    dragState.pointer = Offset.Zero
                                }
                            )
                        }
                    }
                }
            }

            // Ghost card que sigue al dedo mientras se arrastra.
            val dragging = dragState.draggingItem
            if (dragging != null) {
                val density = LocalDensity.current
                val ghostWidthPx = with(density) { 240.dp.toPx() }
                val ghostHeightPx = with(density) { 80.dp.toPx() }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (dragState.pointer.x - ghostWidthPx / 2).toInt(),
                                y = (dragState.pointer.y - ghostHeightPx / 2).toInt()
                            )
                        }
                        .width(240.dp)
                        .graphicsLayer {
                            alpha = 0.85f
                            scaleX = 1.02f
                            scaleY = 1.02f
                        }
                ) {
                    KanbanItemCard(item = dragging)
                }
            }
        }
    }
}

@Composable
private fun KanbanColumnView(
    column: KanbanColumn,
    items: List<KanbanItem>,
    isHoverTarget: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onEditColumn: () -> Unit,
    onDeleteColumn: () -> Unit,
    onStartDrag: (KanbanItem, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val columnColor = column.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) }
        catch (_: Exception) { MaterialTheme.colorScheme.primaryContainer }
    } ?: MaterialTheme.colorScheme.primaryContainer

    val containerColor = if (isHoverTarget) {
        columnColor.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .onGloballyPositioned { layoutCoords ->
                onBoundsChanged(layoutCoords.boundsInRoot())
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${items.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Acciones de columna",
                        modifier = Modifier.padding(0.dp)
                    )
                }
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
                    text = if (isHoverTarget) "Soltar aquí" else "Sin tareas",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isHoverTarget) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    var itemOrigin by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { itemOrigin = it.positionInRoot() }
                            .pointerInput(item.id) {
                                // detectDragGesturesAfterLongPress entrega `change`
                                // con position LOCAL al pointerInput. Lo convertimos
                                // a coord root sumando el origen del item.
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        if (item is KanbanItem.TaskItem) {
                                            onStartDrag(item, itemOrigin + offset)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        onDragMove(itemOrigin + change.position)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() }
                                )
                            }
                    ) {
                        KanbanItemCard(item = item)
                    }
                }
            }
        }
    }
}
