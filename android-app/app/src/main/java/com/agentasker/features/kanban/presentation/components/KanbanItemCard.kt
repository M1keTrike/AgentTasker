package com.agentasker.features.kanban.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentasker.features.kanban.domain.entities.KanbanItem
import com.agentasker.features.kanban.domain.entities.KanbanItemType

@Composable
fun KanbanItemCard(
    item: KanbanItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (item.type) {
                        KanbanItemType.TASK -> Icons.Outlined.TaskAlt
                        KanbanItemType.CLASSROOM -> Icons.Outlined.School
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (item.type) {
                        KanbanItemType.TASK -> MaterialTheme.colorScheme.primary
                        KanbanItemType.CLASSROOM -> MaterialTheme.colorScheme.tertiary
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (item) {
                is KanbanItem.TaskItem -> {
                    val task = item.task
                    if (task.dueDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.dueDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is KanbanItem.ClassroomItem -> {
                    val ct = item.classroomTask
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ct.courseName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
