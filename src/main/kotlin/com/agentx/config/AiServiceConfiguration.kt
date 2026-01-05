package com.agentx.config

import com.agentx.service.ChatAgentService
import com.agentx.tools.CalculatorTools
import com.agentx.tools.DateTimeTools
import com.agentx.tools.StringTools
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.service.AiServices
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Configuration for LangChain4j AI Services.
 * Manually builds AI service beans with proper dependency injection.
 */
@Factory
class AiServiceConfiguration {

    private val logger = LoggerFactory.getLogger(AiServiceConfiguration::class.java)

    /**
     * Manually build the ChatAgentService with all required dependencies.
     *
     * This approach is used instead of @AiService annotation because Micronaut's
     * @AiService doesn't support the chatMemoryProvider parameter.
     *
     * @param chatModel The LangChain4j chat model (injected by Micronaut)
     * @param chatMemoryProvider The custom PostgreSQL-backed memory provider
     * @param calculatorTools Tool for math operations
     * @param dateTimeTools Tool for date/time operations
     * @param stringTools Tool for string manipulations
     * @return Configured ChatAgentService instance
     */
    @Singleton
    fun chatAgentService(
        chatModel: ChatModel,
        chatMemoryProvider: ChatMemoryProvider,
        calculatorTools: CalculatorTools,
        dateTimeTools: DateTimeTools,
        stringTools: StringTools
    ): ChatAgentService {
        logger.info("Building ChatAgentService with ChatMemoryProvider and tools")

        return AiServices.builder(ChatAgentService::class.java)
            .chatModel(chatModel)
            .chatMemoryProvider(chatMemoryProvider)
            .tools(calculatorTools, dateTimeTools, stringTools)
            .build()
    }
}
