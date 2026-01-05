package com.agentx.repository

import com.agentx.config.VectorAttributeConverter
import com.agentx.model.Conversation
import io.micronaut.data.model.DataType
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.sql.ResultSet
import java.time.Instant
import javax.sql.DataSource

@Singleton
open class ConversationRepositoryCustomImpl(
    private val dataSource: DataSource,
    private val vectorConverter: VectorAttributeConverter
) : ConversationRepositoryCustom {

    @Transactional(readOnly = true)
    override fun findSimilarMessagesCustom(
        conversationTitleId: String,
        embedding: FloatArray,
        limit: Int
    ): List<Conversation> {
        val embeddingString = "[${embedding.joinToString(",")}]"

        val sql = """
            SELECT * FROM agentx.conversations
            WHERE conversation_title_id = ?
              AND embedding IS NOT NULL
            ORDER BY embedding OPERATOR(public.<=>) CAST(? AS public.vector)
            LIMIT ?
        """.trimIndent()

        return dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, conversationTitleId)
                stmt.setString(2, embeddingString)
                stmt.setInt(3, limit)

                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<Conversation>()
                    while (rs.next()) {
                        results.add(mapResultSetToConversation(rs))
                    }
                    results
                }
            }
        }
    }

    private fun mapResultSetToConversation(rs: ResultSet): Conversation {
        val embeddingObj = rs.getObject("embedding")
        val embedding = vectorConverter.convertToEntityValue(embeddingObj, null)

        val metadataJson = rs.getString("metadata")
        val metadata = if (metadataJson != null) {
            // Simple JSON parsing - you might want to use a proper JSON library
            emptyMap<String, Any>()
        } else {
            null
        }

        return Conversation(
            id = rs.getLong("id"),
            conversationTitleId = rs.getString("conversation_title_id"),
            role = rs.getString("role"),
            content = rs.getString("content"),
            toolCallId = rs.getString("tool_call_id"),
            toolName = rs.getString("tool_name"),
            metadata = metadata,
            embedding = embedding,
            createdAt = rs.getTimestamp("created_at")?.toInstant()
        )
    }
}
