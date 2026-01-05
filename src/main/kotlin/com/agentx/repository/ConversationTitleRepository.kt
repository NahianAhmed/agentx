package com.agentx.repository

import com.agentx.model.ConversationTitle
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ConversationTitleRepository : CrudRepository<ConversationTitle, String> {

    fun findByUserId(userId: String): List<ConversationTitle>

    @Query("SELECT * FROM agentx.conversation_titles ORDER BY updated_at DESC LIMIT :limit")
    fun findRecentConversations(limit: Int): List<ConversationTitle>
}
