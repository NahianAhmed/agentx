package com.agentx.service

import com.agentx.tools.CalculatorTools
import com.agentx.tools.DateTimeTools
import com.agentx.tools.StringTools
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import io.micronaut.langchain4j.annotation.AiService

@AiService(
    tools = [
        CalculatorTools::class,
        DateTimeTools::class,
        StringTools::class
    ]
)
interface ChatAgentService {

    @SystemMessage("""
        You are a helpful AI assistant with access to various tools.

        IMPORTANT: You MUST use the available tools when users ask for:
        - Calculations (arithmetic, math operations) - use CalculatorTools
        - Current time, date, or timezone information - use DateTimeTools
        - String manipulations - use StringTools

        Available tools include:
        - getCurrentDateTimeInZone(zoneId): Get time in any timezone (e.g., "Asia/Dhaka", "America/New_York")
        - getCurrentTimeInZone(zoneId): Get current time in any timezone
        - Various calculator tools: add, subtract, multiply, divide, sqrt, power

        When users ask about time in specific cities/regions:
        1. Use the getCurrentTimeInZone or getCurrentDateTimeInZone tool
        2. Use IANA timezone identifiers like "Asia/Dhaka", "Europe/London", "America/New_York"

        Always use tools when possible instead of saying you cannot do something.
        Be concise and helpful in your responses.
        Explain which tool you're using and provide the results clearly.

        Remember the conversation context to provide better responses.
    """)
    fun chat(@UserMessage message: String): String
}
