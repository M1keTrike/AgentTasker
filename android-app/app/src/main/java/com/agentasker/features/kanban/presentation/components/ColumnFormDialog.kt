package com.agentasker.features.kanban.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Paleta preset para seleccionar color de columna sin requerir un color
 * picker complejo. Son 12 tonos Material balanceados que contrastan bien
 * sobre `surfaceVariant`. El usuario elige uno tocando su círculo.
 */
private val PRESET_COLORS = listOf(
    "#EF5350", // rojo
    "#EC407A", // rosa
    "#AB47BC", // púrpura
    "#7E57C2", // violeta
    "#5C6BC0", // indigo
    "#42A5F5", // azul
    "#26C6DA", // cian
    "#26A69A", // teal
    "#66BB6A", // verde
    "#FFCA28", // ámbar
    "#FFA726", // naranja
    "#8D6E63"  // marrón
)

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
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                ColorSwatchPicker(
                    selectedColor = color,
                    onColorSelected = onColorChange
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

@Composable
private fun ColorSwatchPicker(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit
) {
    // 2 filas × 6 columnas = 12 swatches. LazyGrid sería overkill
    // para 12 elementos, así que usamos Row + chunked.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PRESET_COLORS.chunked(6).forEach { rowColors ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowColors.forEach { hex ->
                    ColorSwatch(
                        hex = hex,
                        selected = selectedColor.equals(hex, ignoreCase = true),
                        onClick = {
                            // Toggle: tocar el mismo color lo deselecciona,
                            // volviendo al color default del tema.
                            if (selectedColor.equals(hex, ignoreCase = true)) {
                                onColorSelected(null)
                            } else {
                                onColorSelected(hex)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                border = if (selected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Seleccionado",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
