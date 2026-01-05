package com.agentx.repository

import com.agentx.model.Conversation

interface ConversationRepositoryCustom {
    fun findSimilarMessagesCustom(
        conversationTitleId: String,
        embedding: FloatArray,
        limit: Int
    ): List<Conversation>
}
