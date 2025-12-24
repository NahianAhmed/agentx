package com.agentx.tools

import dev.langchain4j.agent.tool.Tool
import jakarta.inject.Singleton

@Singleton
class CalculatorTools {

    @Tool("Calculates the sum of two numbers")
    fun add(a: Double, b: Double): Double {
        return a + b
    }

    @Tool("Calculates the difference between two numbers")
    fun subtract(a: Double, b: Double): Double {
        return a - b
    }

    @Tool("Calculates the product of two numbers")
    fun multiply(a: Double, b: Double): Double {
        return a * b
    }

    @Tool("Calculates the division of two numbers")
    fun divide(a: Double, b: Double): Double {
        if (b == 0.0) {
            throw IllegalArgumentException("Cannot divide by zero")
        }
        return a / b
    }

    @Tool("Calculates the square root of a number")
    fun sqrt(a: Double): Double {
        if (a < 0) {
            throw IllegalArgumentException("Cannot calculate square root of negative number")
        }
        return kotlin.math.sqrt(a)
    }

    @Tool("Calculates the power of a number (base^exponent)")
    fun power(base: Double, exponent: Double): Double {
        return Math.pow(base, exponent)
    }
}
