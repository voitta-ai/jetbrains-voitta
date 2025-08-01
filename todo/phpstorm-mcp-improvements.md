# PhpStorm MCP Integration Improvements

## Current Issues
1. PhpStorm's built-in server (port 63342) doesn't respond to MCP API calls
2. The plugin has PHP support but it's commented out in the main plugin.xml
3. Multiple IDEs conflict when running simultaneously

## Suggested Modifications

### 1. **Add MCP Server Configuration**
Create a configuration service that allows the plugin to:
- Auto-detect which port the IDE is using
- Configure custom MCP endpoints per IDE type
- Handle port conflicts gracefully

### 2. **Enable PHP Support**
The PHP tools are already implemented but disabled. To enable them:
- Uncomment lines 49-56 in `plugin.xml`
- Ensure the PHP AST analyzers in `src/main/kotlin-excluded-temp/` are moved back to active source
- Test PHP-specific functionality in PhpStorm

### 3. **Add IDE-Specific Port Configuration**
Implement a settings page that allows users to:
- Configure which port each IDE should use for MCP
- Set custom environment variables per IDE
- Enable/disable specific tool sets based on IDE type

### 4. **Implement MCP Endpoint Registration**
Add proper MCP endpoint registration in PhpStorm:
```kotlin
// Example: Register MCP endpoints on plugin startup
class McpEndpointRegistrar : StartupActivity {
    override fun runActivity(project: Project) {
        val httpService = HttpServerManager.getInstance()
        httpService.registerHandler("/api/mcp/list_tools", McpToolsHandler())
        httpService.registerHandler("/api/mcp/execute", McpExecuteHandler())
    }
}
```

### 5. **Add Health Check Endpoint**
Implement a simple health check that responds quickly:
```kotlin
class McpHealthCheckHandler : HttpRequestHandler {
    override fun handle(request: HttpRequest): HttpResponse {
        return HttpResponse.ok().withJson("""{"status": "ok", "ide": "PhpStorm"}""")
    }
}
```

### 6. **Universal Tool Improvements**
Since you have universal tools that work across languages:
- Ensure they properly detect PHP files and use appropriate analyzers
- Test the universal tools work correctly in PhpStorm context
- Add PHP-specific code pattern detection

### 7. **Debug Tools for PHP**
Extend the debug tools to work with PHP debugging sessions:
- Support Xdebug protocol specifics
- Handle PHP-specific variable types
- Add remote debugging support

## Implementation Priority
1. **High**: Enable existing PHP tools (uncomment and test)
2. **High**: Add MCP endpoint registration for PhpStorm
3. **Medium**: Implement port configuration UI
4. **Low**: Add PHP-specific debug enhancements

## Testing Strategy
1. Run PhpStorm and IntelliJ simultaneously with different ports
2. Verify all PHP AST tools work correctly
3. Test debug tools with Xdebug sessions
4. Ensure universal tools handle PHP files properly