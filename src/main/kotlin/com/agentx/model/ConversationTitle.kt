package com.agentx.model

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant

@MappedEntity("conversation_titles")
data class ConversationTitle(
    @field:Id
    val id: String,

    val userId: String? = null,

    val title: String? = null,

    @field:DateCreated
    val createdAt: Instant? = null,

    @field:DateUpdated
    val updatedAt: Instant? = null,

    @field:TypeDef(type = DataType.JSON)
    val metadata: Map<String, Any>? = null
)
