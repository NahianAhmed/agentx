package com.agentx.model

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant

@MappedEntity("conversations")
data class Conversation(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    val conversationTitleId: String,

    val role: String,

    val content: String,

    val toolCallId: String? = null,

    val toolName: String? = null,

    @field:TypeDef(type = DataType.JSON)
    val metadata: Map<String, Any>? = null,

    @field:TypeDef(type = DataType.OBJECT)
    @field:io.micronaut.data.annotation.MappedProperty(converter = com.agentx.config.VectorAttributeConverter::class)
    val embedding: FloatArray? = null,

    @field:DateCreated
    val createdAt: Instant? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Conversation

        if (id != other.id) return false
        if (conversationTitleId != other.conversationTitleId) return false
        if (role != other.role) return false
        if (content != other.content) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + conversationTitleId.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}
