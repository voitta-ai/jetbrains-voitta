# MCP Extension Demo - Enhanced AST and Debug Tools

This is an enhanced JetBrains IntelliJ IDEA plugin that extends the MCP (Model Context Protocol) server with advanced AST analysis and debugging tools. This plugin is designed to work alongside the main MCP server plugin to provide additional functionality for code analysis and runtime debugging.

## Features

### Debug Tools
- **get_current_stack_trace** - Retrieves the current stack trace when execution is suspended at a breakpoint
- **get_debug_session_info** - Gets information about the current debug session state
- **get_debug_snapshot** - Creates a complete snapshot of debug state including stack trace and variables

### Java AST Analysis Tools
- **get_java_file_ast** - Retrieves the complete AST structure for Java files
- **get_class_hierarchy** - Analyzes class inheritance relationships
- **get_method_complexity** - Calculates cyclomatic complexity for methods
- **detect_code_patterns** - Detects common code patterns and potential issues

### Navigation Tools
- **get_symbol_at_position** - Gets symbol information at a specific position in a file
- **find_all_references** - Finds all references to a symbol

## Architecture

This plugin is structured as a separate MCP extension that depends on the main MCP server plugin. The key benefits of this approach:

1. **Separation of Concerns** - Enhanced features are isolated from the core MCP functionality
2. **Independent Development** - Can be developed, tested, and debugged separately
3. **Modular Design** - Easy to enable/disable advanced features
4. **Future Integration** - Can potentially be merged into the main plugin later

## Directory Structure

```
src/main/kotlin/org/jetbrains/mcpextensiondemo/
├── MyCustomTool.kt                    # Original demo tool
├── ast/
│   ├── AstDataTypes.kt               # Core AST data structures
│   ├── AstUtils.kt                   # AST utility functions
│   ├── debug/
│   │   └── DebugTools.kt             # Debug inspection tools
│   ├── java/
│   │   └── JavaAstTools.kt           # Java-specific AST analysis
│   └── common/
│       └── NavigationTools.kt        # Language-agnostic navigation
└── utils/
    └── JsonUtils.kt                  # JSON utility functions
```

## Dependencies

- Main MCP Server Plugin (com.intellij.mcpServer)
- IntelliJ Java plugin (for Java AST analysis)
- Kotlin serialization for data structures

## Usage

1. Install the main MCP server plugin
2. Install this extension plugin
3. The new tools will be automatically available through the MCP interface

## Development

To build and test:

```bash
./gradlew build
./gradlew runIde
```

The plugin will be available in the test IDE instance with all the enhanced MCP tools.

## Future Enhancements

- Variable inspection and evaluation in debug mode
- Support for additional languages (Kotlin, Python, etc.)
- Advanced code smell detection
- Integration with static analysis tools
- Performance profiling integration

## Migration Notes

This plugin was created by migrating enhanced AST and debug functionality from the main MCP server plugin repository to create a cleaner separation of concerns. The core functionality remains compatible with the original implementation while providing better maintainability and debugging capabilities.
