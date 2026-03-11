package com.agentasker.features.classroom.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentasker.features.classroom.domain.entities.ClassroomTask
import com.agentasker.features.classroom.domain.entities.SubmissionState
import java.time.format.DateTimeFormatter

@Composable
fun ClassroomTaskCard(
    task: ClassroomTask,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = task.courseName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (task.description != null) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.dueDate != null) {
                    Text(
                        text = task.dueDate.format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                SubmissionStateBadge(state = task.submissionState)
            }

            if (task.maxPoints != null) {
                Text(
                    text = "${task.maxPoints.toInt()} puntos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubmissionStateBadge(state: SubmissionState) {
    val (text, color) = when (state) {
        SubmissionState.TURNED_IN -> "Entregado" to MaterialTheme.colorScheme.primary
        SubmissionState.RETURNED -> "Devuelto" to MaterialTheme.colorScheme.tertiary
        SubmissionState.CREATED -> "Creado" to MaterialTheme.colorScheme.secondary
        SubmissionState.RECLAIMED_BY_STUDENT -> "Recuperado" to MaterialTheme.colorScheme.secondary
        SubmissionState.NEW -> "Pendiente" to MaterialTheme.colorScheme.error
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Medium
    )
}
