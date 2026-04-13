package com.agentasker.core.ai.prompt

object TaskDecompositionPrompt {

    const val SYSTEM =
        "Eres un asistente experto en productividad que descompone tareas en pasos concretos y accionables. " +
            "Respondes ÚNICAMENTE con JSON válido sin texto extra, sin markdown, sin explicaciones. " +
            "Las subtareas deben ser concisas (máximo 80 caracteres), específicas y en imperativo (ej: 'Comprar billetes')."

    fun buildSubtaskSplit(title: String, description: String): String {
        val safeDescription = description.ifBlank { "(sin descripción)" }
        return "Dada la siguiente tarea, descomponla en entre 3 y 8 subtareas ordenadas.\n\n" +
            "Título: $title\n" +
            "Descripción: $safeDescription\n\n" +
            "Responde exactamente con este formato JSON:\n" +
            "{\"subtasks\": [\"...\", \"...\", \"...\"]}"
    }

    fun buildImageAnalysis(ocrText: String): String {
        val safeOcr = if (ocrText.isBlank()) "(no se detectó texto en la imagen)" else ocrText
        return "Se extrajo el siguiente texto de una foto tomada por el usuario. Analízalo y " +
            "genera una tarea con sus subtareas.\n\n" +
            "Texto detectado:\n" +
            "<<<\n$safeOcr\n>>>\n\n" +
            "Responde exactamente con este formato JSON:\n" +
            "{\n" +
            "  \"title\": \"Título breve\",\n" +
            "  \"description\": \"Descripción con contexto útil\",\n" +
            "  \"priority\": \"medium\",\n" +
            "  \"subtasks\": [\"...\", \"...\", \"...\"]\n" +
            "}"
    }
}
