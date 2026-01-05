package com.agentx.service

import com.agentx.model.Conversation
import com.agentx.model.ConversationTitle
import com.agentx.repository.ConversationRepository
import com.agentx.repository.ConversationTitleRepository
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Service responsible for conversation persistence operations.
 * This service only handles data access and does NOT call other services.
 *
 * Responsibilities:
 * - Save and retrieve conversation messages
 * - Manage conversation titles
 * - Delete conversations
 *
 * Note: Orchestration logic (calling multiple services) belongs in the Use Case layer.
 */
@Singleton
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val conversationTitleRepository: ConversationTitleRepository
) {
    private val logger = LoggerFactory.getLogger(ConversationService::class.java)

    /**
     * Save a conversation message with embedding
     */
    fun saveMessage(
        conversationTitleId: String,
        role: String,
        content: String,
        embedding: FloatArray? = null
    ): Conversation {
        val conversation = Conversation(
            conversationTitleId = conversationTitleId,
            role = role,
            content = content,
            embedding = embedding
        )
        return conversationRepository.save(conversation)
    }

    /**
     * Get recent messages from a conversation
     */
    fun getRecentMessages(conversationTitleId: String, limit: Int): List<Conversation> {
        return conversationRepository
            .findRecentMessages(conversationTitleId, limit)
            .reversed() // Oldest first
    }

    /**
     * Ensure conversation title exists, create if not present
     */
    fun ensureConversationExists(conversationTitleId: String) {
        if (!conversationTitleRepository.existsById(conversationTitleId)) {
            conversationTitleRepository.save(
                ConversationTitle(
                    id = conversationTitleId,
                    userId = null,
                    title = null
                )
            )
            logger.info("Created new conversation: $conversationTitleId")
        }
    }

    /**
     * Update conversation title's timestamp
     */
    fun updateConversationTimestamp(conversationTitleId: String) {
        conversationTitleRepository.findById(conversationTitleId).ifPresent { title ->
            conversationTitleRepository.update(
                title.copy(updatedAt = Instant.now())
            )
        }
    }

    /**
     * Delete all messages and title for a conversation
     */
    fun deleteConversation(conversationTitleId: String) {
        conversationRepository.deleteByConversationTitleId(conversationTitleId)
        conversationTitleRepository.deleteById(conversationTitleId)
        logger.info("Deleted conversation: $conversationTitleId")
    }

    /**
     * Check if a conversation exists
     */
    fun conversationExists(conversationTitleId: String): Boolean {
        return conversationTitleRepository.existsById(conversationTitleId)
    }
}
