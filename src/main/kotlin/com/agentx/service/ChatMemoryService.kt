package com.agentx.service

import com.agentx.model.Conversation
import com.agentx.model.ConversationTitle
import com.agentx.repository.ConversationRepository
import com.agentx.repository.ConversationTitleRepository
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant

data class ConversationContext(
    val recentMessages: List<Conversation>,
    val similarMessages: List<Conversation>
)

@Singleton
class ChatMemoryService(
    private val conversationRepository: ConversationRepository,
    private val conversationRepositoryCustom: com.agentx.repository.ConversationRepositoryCustom,
    private val conversationTitleRepository: ConversationTitleRepository,
    private val embeddingService: EmbeddingService,
    private val chatAgentService: ChatAgentService,
    @Value("\${chat-memory.max-messages:20}")
    private val maxMessages: Int,
    @Value("\${chat-memory.max-similar-messages:5}")
    private val maxSimilarMessages: Int,
    @Value("\${chat-memory.similarity-threshold:0.7}")
    private val similarityThreshold: Double
) {
    private val logger = LoggerFactory.getLogger(ChatMemoryService::class.java)

    /**
     * Process chat with conversation memory and semantic search
     */
    fun chat(conversationTitleId: String, userMessage: String): String {
        // 1. Ensure conversation title exists
        ensureConversationTitleExists(conversationTitleId)

        // 2. Generate embedding for user message
        val userEmbedding = embeddingService.generateEmbedding(userMessage)

        // 3. Store user message with embedding
        val userMsg = Conversation(
            conversationTitleId = conversationTitleId,
            role = "USER",
            content = userMessage,
            embedding = userEmbedding
        )
        conversationRepository.save(userMsg)

        // 4. Get conversation context
        val context = buildConversationContext(conversationTitleId, userEmbedding)

        // 5. Build prompt with context
        val promptWithContext = buildPromptWithContext(userMessage, context)

        // 6. Get response from LLM
        val response = chatAgentService.chat(promptWithContext)

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

        // 9. Update conversation title timestamp
        updateConversationTimestamp(conversationTitleId)

        return response
    }

    /**
     * Build conversation context using recent messages + similar messages
     */
    private fun buildConversationContext(
        conversationTitleId: String,
        currentEmbedding: FloatArray
    ): ConversationContext {
        // Get recent messages from this conversation
        val recentMessages = conversationRepository
            .findRecentMessages(conversationTitleId, maxMessages)
            .reversed() // Oldest first

        // Get semantically similar messages
        val similarMessages = try {
            conversationRepositoryCustom
                .findSimilarMessagesCustom(conversationTitleId, currentEmbedding, maxSimilarMessages)
                .filter { it !in recentMessages } // Avoid duplicates
        } catch (e: Exception) {
            logger.warn("Could not retrieve similar messages, pgvector may not be installed: ${e.message}")
            emptyList()
        }

        return ConversationContext(
            recentMessages = recentMessages,
            similarMessages = similarMessages
        )
    }

    /**
     * Build prompt with conversation context
     */
    private fun buildPromptWithContext(
        userMessage: String,
        context: ConversationContext
    ): String {
        val sb = StringBuilder()

        // Add similar messages context if available
        if (context.similarMessages.isNotEmpty()) {
            sb.append("## Relevant Context from Past Conversations:\n")
            context.similarMessages.forEach { msg ->
                sb.append("${msg.role}: ${msg.content}\n")
            }
            sb.append("\n")
        }

        // Add recent conversation history
        if (context.recentMessages.isNotEmpty()) {
            sb.append("## Recent Conversation:\n")
            context.recentMessages.forEach { msg ->
                sb.append("${msg.role}: ${msg.content}\n")
            }
            sb.append("\n")
        }

        // Add current message
        sb.append("## Current Question:\n")
        sb.append(userMessage)

        return sb.toString()
    }

    /**
     * Ensure conversation title exists, create if not
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
            },
            {
                // Create conversation title if it doesn't exist
                conversationTitleRepository.save(
                    ConversationTitle(
                        id = conversationTitleId,
                        userId = null,
                        title = null
                    )
                )
            }
        )
    }

    /**
     * Clear conversation history
     */
    fun clearConversation(conversationTitleId: String) {
        conversationRepository.deleteByConversationTitleId(conversationTitleId)
        conversationTitleRepository.deleteById(conversationTitleId)
        logger.info("Cleared conversation: $conversationTitleId")
    }
}
