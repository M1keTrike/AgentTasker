package com.agentasker.features.tasks.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agentasker.R
import com.agentasker.core.ui.components.PrioritySelector
import com.agentasker.features.tasks.domain.entities.Task

@Composable
fun TaskFormDialog(
    task: Task? = null,
    title: String,
    description: String,
    priority: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, priority: String) -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (task == null) stringResource(R.string.task_new_title) else stringResource(R.string.task_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.task_field_title)) },
                    placeholder = { Text(stringResource(R.string.task_field_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = title.isBlank()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.task_field_description)) },
                    placeholder = { Text(stringResource(R.string.task_field_description_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                var isPrioritySelectorExpanded by remember { mutableStateOf(false) }

                PrioritySelector(
                    selectedPriority = priority,
                    onPriorityChange = onPriorityChange,
                    expanded = isPrioritySelectorExpanded,
                    onExpandedChange = { isPrioritySelectorExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, description, priority)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (task == null) stringResource(R.string.task_button_create) else stringResource(R.string.task_button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.task_button_cancel))
            }
        }
    )
}

