package ai.voitta.jetbrains.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonUtils {
    val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    inline fun <reified T> toJson(obj: T): String {
        return json.encodeToString(obj)
    }
    
    fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
