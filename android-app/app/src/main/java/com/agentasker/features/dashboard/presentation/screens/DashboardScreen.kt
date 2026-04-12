package com.agentasker.features.dashboard.presentation.screens

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agentasker.core.ui.components.LoadingState
import com.agentasker.features.dashboard.presentation.components.DashboardCard
import com.agentasker.features.dashboard.presentation.viewmodel.ClassroomIntegrationUiState
import com.agentasker.features.dashboard.presentation.viewmodel.ClassroomIntegrationViewModel
import com.agentasker.features.dashboard.presentation.viewmodel.DashboardUiState
import com.agentasker.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.agentasker.features.dashboard.presentation.viewmodel.UpcomingItem
import com.agentasker.features.tasks.domain.entities.Task
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    classroomViewModel: ClassroomIntegrationViewModel = hiltViewModel(),
    onNavigateToTasks: () -> Unit = {},
    onNavigateToClassroom: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val classroomState by classroomViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // OAuth launcher: abre la pantalla de consentimiento de Google y
    // devuelve aquí el Intent con el authorization code. Con ese Intent
    // el ViewModel hace el handshake con el backend y deja Classroom
    // conectado.
    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            classroomViewModel.onAuthResult(result.data!!)
        }
    }

    LaunchedEffect(classroomState.error) {
        classroomState.error?.let {
            snackbarHostState.showSnackbar(it)
            classroomViewModel.clearMessages()
        }
    }
    LaunchedEffect(classroomState.infoMessage) {
        classroomState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            // Al sincronizar, recargar el dashboard para reflejar las nuevas
            // tasks importadas en los contadores.
            viewModel.refresh()
            classroomViewModel.clearMessages()
        }
    }

    // Picker de cursos — se abre cuando el usuario toca "Sincronizar Classroom"
    if (classroomState.showCoursePicker) {
        CoursePickerDialog(
            state = classroomState,
            onToggleCourse = classroomViewModel::toggleCourse,
            onSelectAll = classroomViewModel::selectAllCourses,
            onDeselectAll = classroomViewModel::deselectAllCourses,
            onConfirm = classroomViewModel::syncSelectedCourses,
            onDismiss = classroomViewModel::hideCoursePicker
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dashboard") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }
            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    state = state,
                    classroomState = classroomState,
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToClassroom = onNavigateToClassroom,
                    onConnectClassroom = {
                        authLauncher.launch(classroomViewModel.createAuthIntent())
                    },
                    onSyncClassroom = { classroomViewModel.showCoursePicker() },
                    onDeleteArchived = { taskId ->
                        viewModel.deleteArchivedPermanently(taskId)
                    },
                    onRestoreArchived = { taskId ->
                        viewModel.restoreArchived(taskId)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    classroomState: ClassroomIntegrationUiState,
    onNavigateToTasks: () -> Unit,
    onNavigateToClassroom: () -> Unit,
    onConnectClassroom: () -> Unit,
    onSyncClassroom: () -> Unit,
    onDeleteArchived: (String) -> Unit,
    onRestoreArchived: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            SummarySection(
                pendingCount = state.pendingCount,
                completedCount = state.completedCount,
                dueSoonCount = state.dueSoonCount,
                onNavigateToTasks = onNavigateToTasks
            )
        }

        item {
            DashboardCard(
                title = "Próximas entregas",
                icon = Icons.Outlined.CalendarToday,
                iconTint = Color(0xFF1976D2)
            ) {
                if (state.upcomingDeadlines.isEmpty()) {
                    Text(
                        text = "No hay entregas próximas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.upcomingDeadlines.forEach { item ->
                            UpcomingItemRow(item)
                        }
                    }
                }
            }
        }

        item {
            DashboardCard(
                title = "Tareas por prioridad",
                icon = Icons.Outlined.Flag,
                iconTint = Color(0xFFFF5252),
                onClick = onNavigateToTasks
            ) {
                PriorityDistribution(
                    high = state.highPriorityCount,
                    medium = state.mediumPriorityCount,
                    low = state.lowPriorityCount
                )
            }
        }

        item {
            ClassroomIntegrationCard(
                state = classroomState,
                onConnect = onConnectClassroom,
                onSync = onSyncClassroom
            )
        }

        if (state.archivedTasks.isNotEmpty()) {
            item {
                ArchivedTasksCard(
                    archivedTasks = state.archivedTasks,
                    onRestore = onRestoreArchived,
                    onDelete = onDeleteArchived
                )
            }
        }

        item {
            DashboardCard(
                title = "Estado de Classroom",
                icon = Icons.Outlined.School,
                iconTint = Color(0xFF388E3C),
                onClick = onNavigateToClassroom
            ) {
                if (state.activeCourses.isEmpty()) {
                    Text(
                        text = "No hay cursos activos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.activeCourses.forEach { course ->
                            CourseRow(course.name, course.pendingCount)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ClassroomIntegrationCard(
    state: ClassroomIntegrationUiState,
    onConnect: () -> Unit,
    onSync: () -> Unit
) {
    DashboardCard(
        title = "Integraciones",
        icon = Icons.Outlined.Link,
        iconTint = Color(0xFF1976D2)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = Color(0xFF388E3C),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Classroom",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (state.isConnected) "Conectado" else "No conectado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isConnected) Color(0xFF388E3C)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!state.isConnected) {
                Button(
                    onClick = onConnect,
                    enabled = !state.isConnecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isConnecting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Conectar con Classroom")
                }
            } else {
                // Botón abre el picker de cursos en vez de sync directo
                OutlinedButton(
                    onClick = onSync,
                    enabled = !state.isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (state.isSyncing) "Sincronizando…"
                        else "Sincronizar Classroom"
                    )
                }

                state.lastSyncCount?.let { count ->
                    Text(
                        text = "Última sincronización: $count tareas pendientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Dialog que muestra los cursos del usuario con checkboxes para elegir
 * cuáles sincronizar. Se abre desde el botón "Sincronizar Classroom"
 * de la ClassroomIntegrationCard.
 */
@Composable
private fun CoursePickerDialog(
    state: ClassroomIntegrationUiState,
    onToggleCourse: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedCount = state.selectedCourseIds.size
    val allSelected = state.courses.isNotEmpty() &&
        selectedCount == state.courses.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar cursos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isLoadingCourses) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else if (state.courses.isEmpty()) {
                    Text(
                        text = "No se encontraron cursos activos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Toggle all
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (allSelected) onDeselectAll() else onSelectAll() }
                        ) {
                            Text(
                                text = if (allSelected) "Deseleccionar todos"
                                else "Seleccionar todos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$selectedCount/${state.courses.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Lista de cursos
                    state.courses.forEach { course ->
                        val isSelected = course.id in state.selectedCourseIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleCourse(course.id) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                                course.section?.let { section ->
                                    Text(
                                        text = section,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedCount > 0 && !state.isLoadingCourses
            ) {
                Text(
                    if (selectedCount > 0) "Sincronizar ($selectedCount)"
                    else "Sincronizar"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ArchivedTasksCard(
    archivedTasks: List<Task>,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var taskPendingDelete by remember { mutableStateOf<Task?>(null) }

    DashboardCard(
        title = "Archivadas (${archivedTasks.size})",
        icon = Icons.Outlined.Archive,
        iconTint = Color(0xFF6D4C41)
    ) {
        if (archivedTasks.isEmpty()) {
            Text(
                text = "Aún no has archivado ninguna tarea.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                archivedTasks.take(10).forEach { task ->
                    ArchivedTaskRow(
                        task = task,
                        onRestore = { onRestore(task.id) },
                        onRequestDelete = { taskPendingDelete = task }
                    )
                }
                if (archivedTasks.size > 10) {
                    Text(
                        text = "+${archivedTasks.size - 10} más",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    taskPendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text("Eliminar permanentemente") },
            text = {
                Text(
                    "¿Eliminar \"${task.title}\" de forma definitiva? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(task.id)
                        taskPendingDelete = null
                    }
                ) {
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ArchivedTaskRow(
    task: Task,
    onRestore: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            if (task.subtasks.isNotEmpty()) {
                Text(
                    text = "${task.subtasks.size} subtareas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onRestore) {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = "Restaurar",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onRequestDelete) {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = "Eliminar permanentemente",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SummarySection(
    pendingCount: Int,
    completedCount: Int,
    dueSoonCount: Int,
    onNavigateToTasks: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCounter(
            count = pendingCount,
            label = "Pendientes",
            icon = Icons.Outlined.Pending,
            color = Color(0xFFFFA726),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToTasks
        )
        SummaryCounter(
            count = completedCount,
            label = "Completadas",
            icon = Icons.Outlined.CheckCircle,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToTasks
        )
        SummaryCounter(
            count = dueSoonCount,
            label = "Por vencer",
            icon = Icons.Outlined.Schedule,
            color = Color(0xFFFF5252),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToTasks
        )
    }
}

@Composable
private fun SummaryCounter(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    DashboardCard(
        title = "",
        icon = icon,
        iconTint = color,
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@SuppressLint("NewApi")
@Composable
private fun UpcomingItemRow(item: UpcomingItem) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isClassroom) Icons.Outlined.School else Icons.AutoMirrored.Outlined.Assignment,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            if (item.courseName != null) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (item.dueDate != null) {
            Text(
                text = item.dueDate.format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityDistribution(high: Int, medium: Int, low: Int) {
    val total = (high + medium + low).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PriorityRow("Alta", high, total, Color(0xFFFF5252))
        PriorityRow("Media", medium, total, Color(0xFFFFA726))
        PriorityRow("Baja", low, total, Color(0xFF4CAF50))
    }
}

@Composable
private fun PriorityRow(label: String, count: Int, total: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(48.dp)
        )
        LinearProgressIndicator(
            progress = { count.toFloat() / total },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CourseRow(name: String, pendingCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$pendingCount pendientes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
