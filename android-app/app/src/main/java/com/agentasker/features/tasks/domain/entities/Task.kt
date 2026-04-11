package com.agentasker.features.tasks.domain.entities

/**
 * Origen de una task:
 *  - "local":     creada por el usuario en la app (flujo habitual).
 *  - "classroom": importada desde Google Classroom vía el sync del Dashboard.
 *
 * Se mantiene como String plano para no acoplar el DTO y permitir
 * que llegue cualquier valor del backend sin romper el parser.
 */
object TaskSource {
    const val LOCAL = "local"
    const val CLASSROOM = "classroom"
}

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String = "pending",
    val dueDate: String? = null,
    val source: String = TaskSource.LOCAL,
    val externalId: String? = null,
    val courseName: String? = null,
    val externalLink: String? = null,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)
