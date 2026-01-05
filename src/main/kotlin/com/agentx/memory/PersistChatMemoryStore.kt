package com.agentx.memory

import com.agentx.repository.ConversationRepository
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.store.memory.chat.ChatMemoryStore
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Custom ChatMemoryStore implementation that persists chat messages to PostgreSQL.
 * This is used by LangChain4j's MessageWindowChatMemory to store and retrieve messages.
 */
@Singleton
class PersistChatMemoryStore(
    private val conversationRepository: ConversationRepository,
    @Value("\${chat-memory.max-messages:20}")
    private val maxMessages: Int
) : ChatMemoryStore {

    private val logger = LoggerFactory.getLogger(PersistChatMemoryStore::class.java)

    /**
     * Retrieve messages for a specific memory ID (conversation ID)
     */
    override fun getMessages(memoryId: Any): List<ChatMessage> {
        val conversationId = memoryId.toString()
        logger.debug("Getting messages for conversation: $conversationId")

        // Load recent messages from database
        val conversations = conversationRepository
            .findRecentMessages(conversationId, maxMessages)
            .reversed() // Oldest first for chronological order

        logger.debug("Loaded ${conversations.size} messages from database")

        // Convert to LangChain4j ChatMessage objects
        return conversations.map { conversation ->
            when (conversation.role.uppercase()) {
                "USER" -> UserMessage.from(conversation.content)
                "ASSISTANT", "AI" -> AiMessage.from(conversation.content)
                else -> {
                    logger.warn("Unknown role: ${conversation.role}, treating as user message")
                    UserMessage.from(conversation.content)
                }
            }
        }
    }

    /**
     * Update (replace) all messages for a specific memory ID.
     * Called by LangChain4j when messages are added to ChatMemory.
     *
     * Note: This is called AFTER messages are already added by our use case,
     * so we don't want to duplicate. We'll use this as a no-op since we
     * handle persistence in the use case layer.
     */
    override fun updateMessages(memoryId: Any, messages: List<ChatMessage>) {
        val conversationId = memoryId.toString()
        logger.debug("ChatMemoryStore.updateMessages called for conversation: $conversationId with ${messages.size} messages")

        // We handle message persistence in ChatConversationUseCase with embeddings,
        // so this is intentionally a no-op to avoid duplicate storage.
        // The getMessages() method will still retrieve messages correctly.
    }

    /**
     * Delete all messages for a specific memory ID
     */
    override fun deleteMessages(memoryId: Any) {
        val conversationId = memoryId.toString()
        logger.info("Deleting all messages for conversation: $conversationId")

        conversationRepository.deleteByConversationTitleId(conversationId)

        logger.info("Deleted all messages for conversation: $conversationId")
    }
}
