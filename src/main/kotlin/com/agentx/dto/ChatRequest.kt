package com.agentx.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ChatRequest(
    val message: String,
    val conversationId: String? = null
)
