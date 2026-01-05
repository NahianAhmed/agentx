package com.agentx.config

import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Named
import jakarta.inject.Singleton

@Factory
class EmbeddingModelConfig {

    @Singleton
    @Named("embeddingModel")
    fun embeddingModel(
        @Value("\${langchain4j.open-ai.api-key}") apiKey: String
    ): EmbeddingModel {
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName("text-embedding-3-small")
            .build()
    }
}
