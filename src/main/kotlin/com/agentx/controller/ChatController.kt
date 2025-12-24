package com.agentx.controller

import com.agentx.dto.ChatRequest
import com.agentx.dto.ChatResponse
import com.agentx.service.ChatAgentService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import java.util.UUID

@Controller("/api/chat")
@Suppress("MnInjectionPoints")
class ChatController(
    private val chatAgentService: ChatAgentService
) {

    @Post
    fun chat(@Body request: ChatRequest): HttpResponse<ChatResponse> {
        val conversationId = request.conversationId ?: UUID.randomUUID().toString()
        val response = chatAgentService.chat(request.message)

        return HttpResponse.ok(
            ChatResponse(
                response = response,
                conversationId = conversationId
            )
        )
    }
}
