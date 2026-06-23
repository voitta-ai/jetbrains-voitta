# JetBrains-Voitta - Enhanced AST and Debug Tools

This is JetBrains-Voitta, an enhanced JetBrains IDE plugin that extends the MCP (Model Context Protocol) server with advanced AST analysis and debugging tools for both **Java and PHP**. This plugin leverages IntelliJ IDEA's and PhpStorm's powerful PSI (Program Structure Interface) and debugging APIs to provide deep code analysis and runtime inspection capabilities across multiple languages.

This project was inspired by the [original demo plugin](https://github.com/MaXal/mcpExtensionPlugin) as reference. 

## See also

 * [JetBrains MCP Server](https://plugins.jetbrains.com/plugin/26071-mcp-server) ([GitHub](https://github.com/JetBrains/mcp-jetbrains))
 * [JetBrains Claude Code Beta](https://plugins.jetbrains.com/plugin/27310-claude-code-beta-)

## Sample debug sessions

 * [Debug session 1](https://claude.ai/share/78195a43-8510-4a2b-929f-9fa8b30bc630) - some limitations found.
 * [Debug session 2](https://claude.ai/share/80a8faeb-74e3-48ed-8502-2f0c28c35e25) - much better.

## Key Features

### 🌐 **Universal Multi-Language Support**
- **Single Plugin** for both IntelliJ IDEA and PhpStorm
- **Auto-Detection** of Java and PHP files with appropriate analyzer routing
- **Universal Tools** that work regardless of IDE or language
- **Language-Specific Tools** for fine-grained control when needed

### 🔍 **Advanced AST Analysis**
- Complete class structure analysis for Java and PHP
- Method complexity metrics and code pattern detection
- Smart breakpoint suggestions with first executable line detection
- Class hierarchy analysis with inheritance relationships

### 🐛 **Enhanced Debugging**
- Rich variable inspection with object expansion
- Complete stack trace analysis during breakpoints
- Debug session snapshots and evaluation contexts
- Runtime expression evaluation

### 🎯 **Intelligent Breakpoint Suggestions**
- First executable line detection (not just method signatures)
- Decision points (if statements, loops, switches)
- Exception handling entry/exit points
- Method return statements

## Development

### **Available Gradle Tasks**

The plugin supports **multi-platform builds** with clean, explicit tasks for each IDE:

#### **🚀 Run Tasks (Development)**
All sandbox IDEs **automatically use port 63344** to avoid conflicts with production IDEs.

| Task | Description | Features Available | Port |
|------|-------------|-------------------|------|
| `./gradlew runIntelliJ` | Run IntelliJ Ultimate sandbox | ✅ Java tools, Debug tools, Universal tools | 63344 (auto) |
| `./gradlew runPhpStorm` | Run PhpStorm sandbox | ✅ PHP tools, Universal tools | 63344 (auto) |
| `./gradlew runCommunity` | Run Community Edition sandbox | ✅ Java tools, Universal tools | 63344 (auto) |

> **🔧 Port Configuration**: The built-in server port is **automatically configured** - no manual setup required!

#### **📦 Build Plugin Tasks**
| Task | Description | Output Location |
|------|-------------|-----------------|
| `./gradlew buildPluginIntelliJ` | Build plugin for Ultimate Edition | `build/distributions/JetbrainsVoitta-Ultimate-*.zip` |
| `./gradlew buildPluginPhpStorm` | Build plugin for PhpStorm | `build/distributions/JetbrainsVoitta-PhpStorm-*.zip` |
| `./gradlew buildPluginCommunity` | Build plugin for Community Edition | `build/distributions/JetbrainsVoitta-Community-*.zip` |

## **Installation**
```bash
# Build for all platforms
./gradlew buildPluginIntelliJ buildPluginPhpStorm buildPluginCommunity

# Or build specific platforms
./gradlew buildPluginPhpStorm  # Just PhpStorm distribution
```

### **Prerequisites**

Before installing JetBrains-Voitta, ensure you have:

1. **IntelliJ IDEA 2024.3+** (Ultimate Edition recommended), **PhpStorm 2024.3+**, or **IntelliJ Community 2024.3+**
2. **Java JDK 21+** configured in your IDE  
3. **MCP Server Plugin** installed and enabled
   - Go to **Settings** → **Plugins** → **Marketplace**
   - Search for "MCP Server" and install it
   - Restart your IDE after installation

### 🔌 MCP Configuration for Claude

#### **Complete MCP Setup**

To use this plugin with Claude, add the following to your Claude MCP configuration file:

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

#### **🔧 Port Configuration Strategy**

| Configuration | Port | Use Case | Available Tools |
|---------------|------|----------|-----------------|
| `jetbrains-intellij` | 63342 | Production IntelliJ IDEA | Java, Debug, Universal, Navigation |
| `jetbrains-phpstorm` | 63343 | Production PhpStorm | PHP, Universal |
| `jetbrains-plugin-debug` | 63344 | All sandbox IDEs | Depends on IDE (see table above) |

