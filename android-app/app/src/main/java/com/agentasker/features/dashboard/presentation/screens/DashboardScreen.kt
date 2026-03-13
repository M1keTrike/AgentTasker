package com.agentasker.features.dashboard.presentation.screens

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.agentasker.features.dashboard.presentation.viewmodel.DashboardUiState
import com.agentasker.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.agentasker.features.dashboard.presentation.viewmodel.UpcomingItem
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTasks: () -> Unit = {},
    onNavigateToClassroom: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dashboard") }
            )
        }
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
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToClassroom = onNavigateToClassroom,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    onNavigateToTasks: () -> Unit,
    onNavigateToClassroom: () -> Unit,
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
