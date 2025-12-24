package com.agentx.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ChatResponse(
    val response: String,
    val conversationId: String
)
