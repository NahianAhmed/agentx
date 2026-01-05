package com.agentx.service

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class EmbeddingService(
    @Named("embeddingModel")
    private val embeddingModel: EmbeddingModel
) {
    private val logger = LoggerFactory.getLogger(EmbeddingService::class.java)

    /**
     * Generate embedding for a text using OpenAI text-embedding-3-small
     */
    fun generateEmbedding(text: String): FloatArray {
        logger.debug("Generating embedding for text: ${text.take(50)}...")

        val response = embeddingModel.embed(text)
        val embedding = response.content().vector()

        logger.debug("Generated embedding with ${embedding.size} dimensions")
        return embedding
    }

    /**
     * Batch generate embeddings for multiple texts
     */
    fun generateEmbeddings(texts: List<String>): List<FloatArray> {
        logger.debug("Generating embeddings for ${texts.size} texts")

        val segments = texts.map { TextSegment.from(it) }
        val response = embeddingModel.embedAll(segments)
        return response.content().map { it.vector() }
    }
}
