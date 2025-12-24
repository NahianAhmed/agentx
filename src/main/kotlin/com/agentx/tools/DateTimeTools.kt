package com.agentx.tools

import dev.langchain4j.agent.tool.Tool
import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Singleton
class DateTimeTools {

    @Tool("Gets the current date and time")
    fun getCurrentDateTime(): String {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }

    @Tool("Gets the current date and time for a specific timezone (e.g., Asia/Dhaka, America/New_York)")
    fun getCurrentDateTimeInZone(zoneId: String): String {
        return ZonedDateTime.now(ZoneId.of(zoneId))
            .format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }

    @Tool("Gets the current date")
    fun getCurrentDate(): String {
        return LocalDateTime.now().toLocalDate().toString()
    }

    @Tool("Gets the current time")
    fun getCurrentTime(): String {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @Tool("Gets the current time in a specific timezone (e.g., Asia/Dhaka, America/New_York)")
    fun getCurrentTimeInZone(zoneId: String): String {
        return ZonedDateTime.now(ZoneId.of(zoneId))
            .format(DateTimeFormatter.ISO_LOCAL_TIME)
    }
}
