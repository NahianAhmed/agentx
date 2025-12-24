package com.agentx.tools

import dev.langchain4j.agent.tool.Tool
import jakarta.inject.Singleton

@Singleton
class StringTools {

    @Tool("Converts a string to uppercase")
    fun toUpperCase(text: String): String {
        return text.uppercase()
    }

    @Tool("Converts a string to lowercase")
    fun toLowerCase(text: String): String {
        return text.lowercase()
    }

    @Tool("Reverses a string")
    fun reverse(text: String): String {
        return text.reversed()
    }

    @Tool("Gets the length of a string")
    fun getLength(text: String): Int {
        return text.length
    }

    @Tool("Counts the occurrences of a substring in a string")
    fun countOccurrences(text: String, substring: String): Int {
        return text.split(substring).size - 1
    }
}
