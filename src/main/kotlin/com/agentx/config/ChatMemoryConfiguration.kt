package com.agentx.config

import com.agentx.memory.PersistChatMemoryStore
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Configuration for LangChain4j Chat Memory.
 * Provides memory-related beans for persistent conversation storage.
 */
@Factory
class ChatMemoryConfiguration {

    private val logger = LoggerFactory.getLogger(ChatMemoryConfiguration::class.java)

    /**
     * Provides a ChatMemoryProvider that uses PostgresChatMemoryStore for persistence.
     *
     * This provider creates MessageWindowChatMemory instances that:
     * - Store messages in PostgreSQL via PostgresChatMemoryStore
     * - Maintain a sliding window of recent messages
     * - Support per-conversation memory via memoryId
     *
     * @param persistChatMemoryStore The PostgreSQL-backed storage implementation
     * @param maxMessages Maximum number of messages to keep in memory window
     * @return ChatMemoryProvider instance for use with AI services
     */
    @Singleton
    fun chatMemoryProvider(
        persistChatMemoryStore: PersistChatMemoryStore,
        @Value("\${chat-memory.max-messages:20}")
        maxMessages: Int
    ): ChatMemoryProvider {
        logger.info("Creating ChatMemoryProvider with PostgresChatMemoryStore, maxMessages: $maxMessages")

        return ChatMemoryProvider { memoryId ->
            logger.debug("Creating ChatMemory for memory ID: $memoryId")

            MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(persistChatMemoryStore)
                .build()
        }
    }
}
