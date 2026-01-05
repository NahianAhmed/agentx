package com.agentx.repository

import com.agentx.model.Conversation
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ConversationRepository : CrudRepository<Conversation, Long> {

    fun findByConversationTitleIdOrderByCreatedAtAsc(conversationTitleId: String): List<Conversation>

    @Query("""
        SELECT * FROM agentx.conversations
        WHERE conversation_title_id = :conversationTitleId
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun findRecentMessages(conversationTitleId: String, limit: Int): List<Conversation>

    @Query("""
        SELECT * FROM agentx.conversations
        WHERE conversation_title_id = :conversationTitleId
          AND embedding IS NOT NULL
        ORDER BY embedding OPERATOR(public.<=>) CAST(:embedding AS public.vector)
        LIMIT :limit
    """)
    fun findSimilarMessages(
        conversationTitleId: String,
        embedding: String,
        limit: Int
    ): List<Conversation>

    @Query("""
        SELECT * FROM agentx.conversations
        WHERE embedding IS NOT NULL
          AND (embedding OPERATOR(public.<=>) CAST(:embedding AS public.vector)) < :threshold
        ORDER BY embedding OPERATOR(public.<=>) CAST(:embedding AS public.vector)
        LIMIT :limit
    """)
    fun findSimilarMessagesWithThreshold(
        embedding: String,
        threshold: Double,
        limit: Int
    ): List<Conversation>

    fun deleteByConversationTitleId(conversationTitleId: String): Int
}
