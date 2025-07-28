package ai.voitta.jetbrains

import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import org.jetbrains.ide.mcp.Response
import org.jetbrains.mcpserverplugin.AbstractMcpTool

class MyCustomTool : AbstractMcpTool<MyArgs>(MyArgs.serializer()) {
    override val name: String = "my_custom_tool"
    override val description: String = "Custom tool for the demonstration"

    override fun handle(project: Project, args: MyArgs): Response {
        return Response("Everything is fine, passed args: ${args.param1}, args: ${args.param2}")
    }
}

// Define your arguments data class
@Serializable
data class MyArgs(
    val param1: String,
    val param2: Int
)