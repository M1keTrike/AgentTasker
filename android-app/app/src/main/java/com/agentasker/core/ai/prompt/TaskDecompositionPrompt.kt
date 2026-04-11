package com.agentasker.core.ai.prompt

/**
 * Prompts reutilizables para los flujos de IA que descomponen tasks.
 * Se centralizan aquí para poder iterar sin tocar workers o services.
 */
object TaskDecompositionPrompt {

    const val SYSTEM =
        "Eres un asistente experto en productividad que descompone tareas en pasos concretos y accionables. " +
            "Respondes ÚNICAMENTE con JSON válido sin texto extra, sin markdown, sin explicaciones. " +
            "Las subtareas deben ser concisas (máximo 80 caracteres), específicas y en imperativo (ej: 'Comprar billetes')."

    /**
     * Prompt para dividir una task existente en subtasks a partir de su
     * título y descripción. Espera respuesta:
     *   { "subtasks": ["Paso 1", "Paso 2", ...] }
     */
    fun buildSubtaskSplit(title: String, description: String): String {
        val safeDescription = description.ifBlank { "(sin descripción)" }
        return "Dada la siguiente tarea, descomponla en entre 3 y 8 subtareas ordenadas.\n\n" +
            "Título: $title\n" +
            "Descripción: $safeDescription\n\n" +
            "Responde exactamente con este formato JSON:\n" +
            "{\"subtasks\": [\"...\", \"...\", \"...\"]}"
    }

    /**
     * Prompt para analizar texto OCR extraído de una foto y sugerir una
     * task completa. Espera respuesta:
     *   {
     *     "title": "...",
     *     "description": "...",
     *     "priority": "high|medium|low",
     *     "subtasks": ["...", "..."]
     *   }
     */
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
