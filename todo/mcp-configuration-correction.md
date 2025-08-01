# MCP Configuration Correction

## Issue Found
The README.md was using the incorrect environment variable `JETBRAINS_PORT` instead of the official `IDE_PORT` as specified in the [JetBrains MCP repository](https://github.com/JetBrains/mcp-jetbrains).

## ✅ Corrected Configuration

### **Correct Claude MCP Configuration:**
```json
{
    "mcpServers": {
        "jetbrains-intellij": {
            "command": "npx",
            "args": ["-y", "@jetbrains/mcp-proxy"],
            "env": {
                "LOG_ENABLED": "true",
                "IDE_PORT": "63342"
            }
        },
        "jetbrains-phpstorm": {
            "command": "npx",
            "args": ["-y", "@jetbrains/mcp-proxy"],
            "env": {
                "LOG_ENABLED": "true",
                "IDE_PORT": "63343"
            }
        },
        "jetbrains-plugin-debug": {
            "command": "npx",
            "args": ["-y", "@jetbrains/mcp-proxy"],
            "env": {
                "LOG_ENABLED": "true",
                "IDE_PORT": "63344"
            }
        }
    }
}
```

## Changes Made

### ❌ **Before (Incorrect):**
- `"JETBRAINS_PORT": "63342"`
- `"JETBRAINS_PORT": "63343"`
- `"JETBRAINS_PORT": "63344"`

### ✅ **After (Correct):**
- `"IDE_PORT": "63342"`
- `"IDE_PORT": "63343"`
- `"IDE_PORT": "63344"`

## Files Updated
- ✅ README.md: Complete MCP Setup section
- ✅ README.md: Development MCP configuration
- ✅ README.md: Troubleshooting section

## Verification
Users should update their Claude MCP configuration to use `IDE_PORT` instead of `JETBRAINS_PORT` for proper connection to the JetBrains MCP proxy.

## Reference
- [Official JetBrains MCP Repository](https://github.com/JetBrains/mcp-jetbrains)
- [MCP Proxy Documentation](https://github.com/JetBrains/mcp-jetbrains#usage)