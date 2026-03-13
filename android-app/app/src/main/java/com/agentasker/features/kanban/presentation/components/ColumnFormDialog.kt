package com.agentasker.features.kanban.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ColumnFormDialog(
    isEditing: Boolean,
    title: String,
    statusKey: String,
    color: String?,
    onTitleChange: (String) -> Unit,
    onStatusKeyChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Editar Columna" else "Nueva Columna")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = statusKey,
                    onValueChange = onStatusKeyChange,
                    label = { Text("Clave de estado") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Ej: pending, in_progress, review") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = color ?: "",
                    onValueChange = { onColorChange(it.ifBlank { null }) },
                    label = { Text("Color (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Ej: #FF5722") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank() && statusKey.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
