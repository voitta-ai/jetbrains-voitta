# MCP Configuration for Multiple JetBrains IDEs

## Issue Summary
When running both PhpStorm and IntelliJ IDEA simultaneously, the MCP (Model Context Protocol) connection fails with timeout errors. This happens because:
- Both IDEs compete for the same default port (63342)
- PhpStorm's built-in server doesn't respond to MCP API calls properly
- The MCP proxy times out trying to connect to PhpStorm, then falls back to IntelliJ on port 63343

## Current Port Configuration
- **PhpStorm**: Port 63342 (default, but not responding to MCP)
- **IntelliJ IDEA**: Port 63343 (working correctly with MCP)

## Solution: Separate MCP Configurations

Update Claude's MCP configuration to have separate entries for each IDE:

```json
{
    "jetbrains-intellij": {
        "command": "npx",
        "args": [
            "-y",
            "@jetbrains/mcp-proxy"
        ],
        "env": {
            "LOG_ENABLED": "true",
            "JETBRAINS_PORT": "63343"
        }
    },
    "jetbrains-phpstorm": {
        "command": "npx",
        "args": [
            "-y",
            "@jetbrains/mcp-proxy"
        ],
        "env": {
            "LOG_ENABLED": "true",
            "JETBRAINS_PORT": "63342"
        }
    }
}
```

## Prerequisites Completed
- ✅ Switched from Node.js v24.2.0 to v20.19.4 (stable LTS)
- ✅ Cleared npm/npx cache
- ✅ Verified both IDEs have MCP plugin installed
- ✅ Confirmed IntelliJ is listening on port 63343

## Next Steps for Plugin Development
1. Ensure MCP endpoints are properly configured in PhpStorm plugin
2. Test that PhpStorm responds correctly to `/api/mcp/list_tools` requests
3. Consider implementing port auto-detection in the plugin to avoid conflicts