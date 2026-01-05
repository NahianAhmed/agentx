package com.agentx.config

import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.model.runtime.convert.AttributeConverter
import jakarta.inject.Singleton

@Singleton
class VectorAttributeConverter : AttributeConverter<FloatArray?, Any?> {

    override fun convertToPersistedValue(floatArray: FloatArray?, context: ConversionContext?): Any? {
        if (floatArray == null) return null

        return try {
            // Create PGobject dynamically to avoid compile-time dependency
            val pgObjectClass = Class.forName("org.postgresql.util.PGobject")
            val pgObject = pgObjectClass.getDeclaredConstructor().newInstance()
            val setTypeMethod = pgObjectClass.getMethod("setType", String::class.java)
            val setValueMethod = pgObjectClass.getMethod("setValue", String::class.java)

            setTypeMethod.invoke(pgObject, "vector")
            setValueMethod.invoke(pgObject, "[${floatArray.joinToString(",")}]")

            pgObject
        } catch (e: Exception) {
            null
        }
    }

    override fun convertToEntityValue(obj: Any?, context: ConversionContext?): FloatArray? {
        if (obj == null) return null

        return try {
            // Handle PGobject from PostgreSQL
            val vectorString = when {
                obj.javaClass.name == "org.postgresql.util.PGobject" -> {
                    val typeMethod = obj.javaClass.getMethod("getType")
                    val valueMethod = obj.javaClass.getMethod("getValue")
                    val type = typeMethod.invoke(obj) as? String
                    if (type == "vector") {
                        valueMethod.invoke(obj) as? String
                    } else null
                }
                obj is String -> obj
                else -> null
            } ?: return null

            // Parse the vector string format: [0.1,0.2,0.3,...]
            vectorString
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().toFloat() }
                .toFloatArray()
        } catch (e: Exception) {
            null
        }
    }
}
