package com.agentasker.core.ai.model

import com.google.gson.annotations.SerializedName

data class DeepSeekChatRequest(
    @SerializedName("model")
    val model: String = "deepseek-chat",
    @SerializedName("messages")
    val messages: List<DeepSeekMessage>,
    @SerializedName("temperature")
    val temperature: Double = 0.3,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    @SerializedName("response_format")
    val responseFormat: DeepSeekResponseFormat? = DeepSeekResponseFormat(type = "json_object"),
    @SerializedName("stream")
    val stream: Boolean = false
)

data class DeepSeekResponseFormat(
    @SerializedName("type")
    val type: String
)

data class DeepSeekMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

data class DeepSeekChatResponse(
    @SerializedName("id")
    val id: String?,
    @SerializedName("model")
    val model: String?,
    @SerializedName("choices")
    val choices: List<DeepSeekChoice>
)

data class DeepSeekChoice(
    @SerializedName("index")
    val index: Int,
    @SerializedName("message")
    val message: DeepSeekMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)
