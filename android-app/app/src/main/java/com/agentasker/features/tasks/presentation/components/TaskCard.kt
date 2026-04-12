package com.agentasker.features.tasks.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.agentasker.core.ui.components.PriorityBadge
import com.agentasker.features.tasks.domain.entities.Subtask
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.entities.TaskSource

@Composable
fun TaskCard(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSplitWithAi: (() -> Unit)? = null,
    onToggleSubtask: ((Subtask) -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isClassroom = task.source == TaskSource.CLASSROOM

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // El badge va en su PROPIA línea arriba del título.
                    // Antes estaba inline con el título, pero entre el
                    // badge + los 3 IconButtons de la Row padre, al Text
                    // le quedaba un ancho tan chico que Compose wrapeaba
                    // a 1 carácter por línea. Separarlos garantiza que
                    // el título tenga todo el ancho de la Column.
                    if (isClassroom) {
                        ClassroomBadge()
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PriorityBadge(priority = task.priority)
                        if (isClassroom && task.courseName != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.courseName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row {
                    if (onSplitWithAi != null) {
                        IconButton(onClick = onSplitWithAi) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = "Dividir con IA",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    // Aunque la task venga de Classroom, como ahora vive
                    // en la tabla local el usuario puede editarla,
                    // archivarla y borrarla a voluntad. Si re-sincroniza
                    // Classroom, el repo preserva los flags locales
                    // (isArchived, status) por idempotencia.
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar tarea",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar tarea",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    task.subtasks.forEach { subtask ->
                        SubtaskRow(
                            subtask = subtask,
                            onToggle = { onToggleSubtask?.invoke(subtask) }
                        )
                    }
                }
            }

            // Cuando TODAS las subtasks están tachadas, aparece el botón
            // verde "Completar y archivar". Se muestra para cualquier
            // task (local o Classroom) porque el archivado vive en la
            // DB local.
            if (task.allSubtasksCompleted && onComplete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Completar y archivar")
                }
            }

            task.createdAt?.let { createdAt ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Creada: $createdAt",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun ClassroomBadge() {
    Row(
        modifier = Modifier
            .background(
                color = Color(0xFF388E3C).copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.School,
            contentDescription = "Tarea de Google Classroom",
            tint = Color(0xFF388E3C),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Classroom",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF388E3C),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SubtaskRow(
    subtask: Subtask,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (subtask.isCompleted) Icons.Default.CheckBox
                else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (subtask.isCompleted) "Desmarcar" else "Completar",
                tint = if (subtask.isCompleted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodySmall,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
