package com.agentasker.core.ai

import com.agentasker.core.ai.model.DeepSeekChatRequest
import com.agentasker.core.ai.model.DeepSeekChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DeepSeekApi {

    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: DeepSeekChatRequest): DeepSeekChatResponse
}
