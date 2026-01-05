package com.agentx.facade

import com.agentx.model.Conversation
import com.agentx.model.ConversationTitle
import com.agentx.repository.ConversationRepository
import com.agentx.repository.ConversationRepositoryCustom
import com.agentx.repository.ConversationTitleRepository
import com.agentx.service.ChatAgentService
import com.agentx.service.EmbeddingService
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Use case that orchestrates the chat conversation flow.
 * Handles coordination between multiple services and repositories.
 *
 * This layer is responsible for:
 * - Business logic orchestration
 * - Coordinating multiple services
 * - Managing transaction boundaries
 * - Enriching context with semantic search
 */
@Singleton
class ChatConversationFacade(
    private val conversationRepository: ConversationRepository,
    private val conversationRepositoryCustom: ConversationRepositoryCustom,
    private val conversationTitleRepository: ConversationTitleRepository,
    private val embeddingService: EmbeddingService,
    private val chatAgentService: ChatAgentService,
    @Value("\${chat-memory.max-similar-messages:5}")
    private val maxSimilarMessages: Int,
    @Value("\${chat-memory.similarity-threshold:0.7}")
    private val similarityThreshold: Double
) {
    private val logger = LoggerFactory.getLogger(ChatConversationFacade::class.java)

    /**
     * Process a chat message with conversation memory and semantic search.
     *
     * Flow:
     * 1. Ensure conversation exists
     * 2. Generate embedding for user message
     * 3. Store user message with embedding
     * 4. Get semantically similar messages from past conversations
     * 5. Build enhanced message with semantic context (if available)
     * 6. Call AI service (LangChain4j handles recent chat memory automatically)
     * 7. Generate embedding for AI response
     * 8. Store AI response with embedding
     * 9. Update conversation timestamp
     *
     * @param conversationTitleId Unique identifier for the conversation
     * @param userMessage The user's message
     * @return AI assistant's response
     */
    fun chat(conversationTitleId: String, userMessage: String): String {
        logger.info("Processing chat for conversation: $conversationTitleId")

        // 1. Ensure conversation title exists
        ensureConversationTitleExists(conversationTitleId)

        // 2. Generate embedding for user message
        val userEmbedding = embeddingService.generateEmbedding(userMessage)
        logger.debug("Generated embedding for user message")

        // 3. Store user message with embedding BEFORE calling AI
        // This ensures it's available in the chat memory for context
        val userMsg = Conversation(
            conversationTitleId = conversationTitleId,
            role = "USER",
            content = userMessage,
            embedding = userEmbedding
        )
        conversationRepository.save(userMsg)
        logger.debug("Stored user message in database")

        // 4. Get semantically similar messages from past conversations
        val similarMessages = getSemanticallySimilarMessages(conversationTitleId, userEmbedding)

        // 5. Build message with semantic context if available
        // Note: Recent conversation history is handled automatically by LangChain4j ChatMemory
        val messageToSend = if (similarMessages.isNotEmpty()) {
            buildMessageWithSemanticContext(userMessage, similarMessages)
        } else {
            userMessage
        }

        logger.debug("Calling AI service with conversation ID: $conversationTitleId")

        // 6. Call AI service with @MemoryId
        // LangChain4j will automatically load and inject recent chat history
        val response = chatAgentService.chat(conversationTitleId, messageToSend)

        logger.debug("Received response from AI service")

        // 7. Generate embedding for assistant response
        val assistantEmbedding = embeddingService.generateEmbedding(response)

        // 8. Store assistant message with embedding
        val assistantMsg = Conversation(
            conversationTitleId = conversationTitleId,
            role = "ASSISTANT",
            content = response,
            embedding = assistantEmbedding
        )
        conversationRepository.save(assistantMsg)
        logger.debug("Stored assistant response in database")

        // 9. Update conversation title timestamp
        updateConversationTimestamp(conversationTitleId)

        logger.info("Successfully processed chat for conversation: $conversationTitleId")

        return response
    }

    /**
     * Clear all messages in a conversation
     */
    fun clearConversation(conversationTitleId: String) {
        logger.info("Clearing conversation: $conversationTitleId")
        conversationRepository.deleteByConversationTitleId(conversationTitleId)
        conversationTitleRepository.deleteById(conversationTitleId)
        logger.info("Successfully cleared conversation: $conversationTitleId")
    }

    /**
     * Get semantically similar messages using vector similarity search.
     * This enriches the context beyond just recent messages.
     */
    private fun getSemanticallySimilarMessages(
        conversationTitleId: String,
        currentEmbedding: FloatArray
    ): List<Conversation> {
        return try {
            val recentMessages = conversationRepository
                .findRecentMessages(conversationTitleId, 20) // Get recent to filter out

            conversationRepositoryCustom
                .findSimilarMessagesCustom(conversationTitleId, currentEmbedding, maxSimilarMessages)
                .filter { it !in recentMessages } // Avoid duplicates with recent history
        } catch (e: Exception) {
            logger.warn("Could not retrieve similar messages, pgvector may not be installed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Build message enriched with semantic context from similar past conversations.
     * This is added as additional context to the user's message.
     */
    private fun buildMessageWithSemanticContext(
        userMessage: String,
        similarMessages: List<Conversation>
    ): String {
        val sb = StringBuilder()

        // Add relevant context header
        sb.append("## Relevant Context from Past Conversations:\n")
        similarMessages.forEach { msg ->
            sb.append("${msg.role}: ${msg.content}\n")
        }
        sb.append("\n")

        // Add current message
        sb.append("## Current Question:\n")
        sb.append(userMessage)

        return sb.toString()
    }

    /**
     * Ensure conversation title exists, create if not present
     */
    private fun ensureConversationTitleExists(conversationTitleId: String) {
        if (!conversationTitleRepository.existsById(conversationTitleId)) {
            conversationTitleRepository.save(
                ConversationTitle(
                    id = conversationTitleId,
                    userId = null,
                    title = null
                )
            )
            logger.debug("Created new conversation title: $conversationTitleId")
        }
    }

    /**
     * Update conversation title's updated_at timestamp
     */
    private fun updateConversationTimestamp(conversationTitleId: String) {
        conversationTitleRepository.findById(conversationTitleId).ifPresentOrElse(
            { title ->
                conversationTitleRepository.update(
                    title.copy(updatedAt = Instant.now())
                )
                logger.debug("Updated conversation timestamp: $conversationTitleId")
            },
            {
                // Create if doesn't exist (shouldn't happen, but defensive)
                conversationTitleRepository.save(
                    ConversationTitle(
                        id = conversationTitleId,
                        userId = null,
                        title = null
                    )
                )
                logger.warn("Conversation title was missing, created: $conversationTitleId")
            }
        )
    }
}
