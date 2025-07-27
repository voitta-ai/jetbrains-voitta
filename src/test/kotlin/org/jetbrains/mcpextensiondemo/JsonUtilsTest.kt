package org.jetbrains.mcpextensiondemo

import org.junit.jupiter.api.Test
import org.jetbrains.mcpextensiondemo.utils.JsonUtils
import kotlin.test.assertEquals

class JsonUtilsTest {
    
    @Test
    fun testEscapeJson() {
        // Test basic escaping
        assertEquals("Hello World", JsonUtils.escapeJson("Hello World"))
        
        // Test quote escaping
        assertEquals("Hello \\\"World\\\"", JsonUtils.escapeJson("Hello \"World\""))
        
        // Test newline escaping
        assertEquals("Hello\\nWorld", JsonUtils.escapeJson("Hello\nWorld"))
        
        // Test backslash escaping
        assertEquals("Hello\\\\World", JsonUtils.escapeJson("Hello\\World"))
        
        // Test complex string
        val input = "Class \"MyClass\" {\n\tpublic void method() {\n\t\t// comment\n\t}\n}"
        val expected = "Class \\\"MyClass\\\" {\\n\\tpublic void method() {\\n\\t\\t// comment\\n\\t}\\n}"
        assertEquals(expected, JsonUtils.escapeJson(input))
    }
}
