package com.agentx.controller

import com.agentx.dto.ChatRequest
import com.agentx.dto.ChatResponse
import com.agentx.service.ChatAgentService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.util.UUID

@Controller("/api/chat")
@Suppress("MnInjectionPoints")
class ChatController(
    private val chatAgentService: ChatAgentService
) {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    @Post
    fun chat(@Body request: ChatRequest): HttpResponse<*> {
        return try {
            // Validate message is not blank
            if (request.message.isBlank()) {
                logger.warn("Received empty message in chat request")
                return HttpResponse.badRequest(mapOf("error" to "Message cannot be empty"))
            }

            // Validate and get conversation ID
            val conversationId = validateAndGetConversationId(request.conversationId)

            logger.info("Processing chat request for conversation: $conversationId")

            // Process the chat request (without memory for now)
            val response = chatAgentService.chat(request.message)

            logger.info("Successfully processed chat request for conversation: $conversationId")

            HttpResponse.ok(
                ChatResponse(
                    response = response,
                    conversationId = conversationId
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid request: ${e.message}")
            HttpResponse.badRequest(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error processing chat request", e)
            HttpResponse.status<Map<String, String>>(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "An error occurred while processing your request"))
        }
    }

    /**
     * Validates the conversation ID format and returns a valid ID
     * If no ID is provided, generates a new UUID
     */
    private fun validateAndGetConversationId(conversationId: String?): String {
        return if (conversationId.isNullOrBlank()) {
            val newId = UUID.randomUUID().toString()
            logger.info("Generated new conversation ID: $newId")
            newId
        } else {
            // Validate UUID format
            try {
                UUID.fromString(conversationId)
                logger.debug("Using existing conversation ID: $conversationId")
                conversationId
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid conversation ID format: $conversationId")
                throw IllegalArgumentException("Invalid conversation ID format. Must be a valid UUID")
            }
        }
    }
}
