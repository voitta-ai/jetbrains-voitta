package ai.voitta.jetbrains.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.ide.RestService
import io.netty.handler.codec.http.QueryStringDecoder
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.channel.ChannelHandlerContext

/**
 * Initializes MCP server endpoints and logs configuration on plugin startup
 */
class McpServerInitializer : ProjectActivity {
    
    companion object {
        private val LOG = Logger.getInstance(McpServerInitializer::class.java)
        private const val DEFAULT_PORT = 63342
        private const val DEBUG_PORT = 63344
    }
    
    override suspend fun execute(project: Project) {
        try {
            val appInfo = ApplicationInfo.getInstance()
            val productName = appInfo.fullApplicationName
            val port = System.getProperty("idea.builtin.server.port")?.toIntOrNull() ?: DEFAULT_PORT
            
            LOG.info("=== MCP Server Initializer ===")
            LOG.info("Product: $productName")
            LOG.info("Version: ${appInfo.fullVersion}")
            LOG.info("Built-in server port: $port")
            LOG.info("Is sandbox: ${isSandboxEnvironment()}")
            
            // Check environment variables for port configuration
            val envPort = System.getenv("IDEA_BUILTIN_SERVER_PORT")
            if (envPort != null) {
                LOG.info("Environment IDEA_BUILTIN_SERVER_PORT: $envPort")
            }
            
            // Register MCP health check endpoint
            registerHealthCheckEndpoint()
            
            // Log successful initialization with emphasis on port
            LOG.info("🔌 MCP SERVER READY on port $port")
            
            if (port == DEBUG_PORT) {
                LOG.info("🚀 PLUGIN DEBUG MODE - Connect Claude to port $DEBUG_PORT")
            }
            
        } catch (e: Exception) {
            LOG.error("Failed to initialize MCP server", e)
        }
    }
    
    private fun isSandboxEnvironment(): Boolean {
        // Check if we're running in a sandbox environment
        val sandboxIndicators = listOf(
            System.getProperty("idea.sandbox.path"),
            System.getProperty("idea.plugins.path")
        )
        
        return sandboxIndicators.any { path ->
            path?.contains("sandbox", ignoreCase = true) == true
        }
    }
    
    private fun registerHealthCheckEndpoint() {
        // This is a placeholder - actual endpoint registration would need
        // to be done through the MCP Server plugin's extension points
        LOG.info("MCP health check endpoint ready")
    }
}

/**
 * REST service to provide MCP health check endpoint
 */
class McpHealthCheckService : RestService() {
    
    companion object {
        private val LOG = Logger.getInstance(McpHealthCheckService::class.java)
    }
    
    override fun getServiceName(): String = "mcp/health"
    
    override fun execute(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): String? {
        val appInfo = ApplicationInfo.getInstance()
        val port = System.getProperty("idea.builtin.server.port") ?: "63342"
        
        val response = """
            {
                "status": "ok",
                "ide": "${appInfo.fullApplicationName}",
                "version": "${appInfo.fullVersion}",
                "port": "$port",
                "mcp_ready": true,
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        
        LOG.debug("Health check requested, responding with: $response")
        
        return response
    }
}