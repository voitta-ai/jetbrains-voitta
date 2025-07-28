# JetbrainsVoitta - Enhanced AST and Debug Tools

This is JetbrainsVoitta, an enhanced JetBrains IntelliJ IDEA plugin that extends the MCP (Model Context Protocol) server with advanced AST analysis and debugging tools. This plugin leverages IntelliJ IDEA's powerful PSI (Program Structure Interface) and debugging APIs to provide deep code analysis and runtime inspection capabilities.

This project was inspired by the [original demo plugin](https://github.com/MaXal/mcpExtensionPlugin) as reference. For Claude integration,

## See also

 * [JetBrains MCP Server](https://plugins.jetbrains.com/plugin/26071-mcp-server) ([GitHub](https://github.com/JetBrains/mcp-jetbrains))
 * [JetBrains Claude Code Beta](https://plugins.jetbrains.com/plugin/27310-claude-code-beta-)
 * 

## Sample debug sessions

 * [Debug session 1](https://claude.ai/share/78195a43-8510-4a2b-929f-9fa8b30bc630) - some limitations found.
 * [Debug session 2]()

## Development

## Installation

JetbrainsVoitta can be installed in several ways depending on your setup and requirements.

### **Prerequisites**

Before installing JetbrainsVoitta, ensure you have:

1. **IntelliJ IDEA 2024.3+** (Community or Ultimate Edition)
2. **Java JDK 21+** configured in your IDE
3. **MCP Server Plugin** installed and enabled
   - Go to **Settings** → **Plugins** → **Marketplace**
   - Search for "MCP Server" and install it
   - Restart IntelliJ IDEA after installation

### **Method 1: Install from Built Plugin (Recommended)**

#### **Step 1: Build the Plugin**
```bash
# Clone the repository
git clone <repository-url>
cd jetbrains-voitta

# Build the plugin
./gradlew build

# The built plugin will be in build/distributions/
```

#### **Step 2: Install the Plugin**
1. Open **IntelliJ IDEA**
2. Go to **Settings** → **Plugins**
3. Click the **⚙️** (gear) icon → **Install Plugin from Disk...**
4. Navigate to `build/distributions/JetbrainsVoitta-1.0-SNAPSHOT.zip`
5. Click **OK** and restart IntelliJ IDEA

### **Method 2: Development Installation**

For development and testing purposes:

#### **Step 1: Clone and Setup**
```bash
git clone <repository-url>
cd jetbrains-voitta

# Ensure you have JDK 21+ configured
./gradlew build
```

#### **Step 2: Run in Development Mode**
```bash
# This will start a new IntelliJ IDEA instance with the plugin loaded
./gradlew runIde
```

This method is ideal for:
- Plugin development and debugging
- Testing new features
- Contributing to the project

### **Plugin Development**
The plugin uses IntelliJ's plugin development framework with MCP integration:

1. **Tool Registration**: Tools are registered in `plugin.xml`
2. **PSI Integration**: Direct access to IntelliJ's PSI APIs
3. **JSON Serialization**: Kotlinx.serialization for data structures
4. **Error Handling**: Comprehensive error handling for PSI operations

## Migration and Compatibility

This plugin maintains compatibility with the original MCP server implementation while providing enhanced capabilities through IntelliJ's native APIs. The PSI-based approach ensures robust and accurate code analysis that leverages the full power of IntelliJ IDEA's language understanding capabilities.

## Available Tools

### 🔍 **Enhanced AST Analysis Tools**

#### `get_java_file_ast`
Retrieves comprehensive AST structure for Java files with enhanced method analysis.
```json
{
  "filePath": "src/main/java/Example.java",
  "includeMethodBodies": true,
  "includePrivateMembers": true
}
```
**Returns**: Complete class structure with method details including first executable line numbers, line ranges, and complexity metrics.

#### `get_method_details`
**NEW**: Provides detailed method analysis including breakpoint suggestions and line ranges.
```json
{
  "filePath": "src/main/java/Example.java",
  "methodName": "myMethod"  // optional
}
```
**Returns**: Comprehensive method information including:
- Method signature line number
- First executable line number
- Method body line range
- Complexity metrics
- Smart breakpoint suggestions
- Parameter details

#### `suggest_breakpoint_lines`
**NEW**: Suggests optimal breakpoint locations in methods.
```json
{
  "filePath": "src/main/java/Example.java",
  "methodName": "myMethod"  // optional
}
```
**Returns**: Smart breakpoint suggestions with reasons (FIRST_EXECUTABLE, DECISION_POINT, METHOD_EXIT, etc.)

#### `get_method_complexity`
Calculates cyclomatic complexity and other metrics for methods.
```json
{
  "filePath": "src/main/java/Example.java",
  "methodName": "myMethod"  // optional
}
```
**Returns**: Complexity metrics including cyclomatic complexity, lines of code, and parameter count.

#### `detect_code_patterns`
Detects common code patterns and potential issues.
```json
{
  "filePath": "src/main/java/Example.java",
  "patterns": ["null_checks", "exception_handling", "loops", "all"]
}
```
**Returns**: Code pattern analysis with suggestions for improvement.

#### `get_class_hierarchy`
Analyzes class inheritance relationships and interfaces.
```json
{
  "className": "com.example.MyClass",
  "direction": "both"  // "up", "down", "both"
}
```
**Returns**: Complete class hierarchy information.

### 🐛 **Debug Tools**

#### `get_debug_snapshot`
Creates a complete snapshot of the current debug state.
```json
{
  "includeVariables": true,
  "includeStackTrace": true,
  "expandObjects": false,
  "maxStackFrames": 10
}
```
**Returns**: Complete debug state including stack trace, variables, and execution context.

#### `get_current_stack_trace`
Retrieves the current stack trace when suspended at a breakpoint.
**Returns**: Array of stack frames with method names, class names, file names, and line numbers.

#### `get_debug_session_info`
Gets information about the current debug session state.
**Returns**: Debug session status, suspended state, and breakpoint information.

### 🧭 **Navigation Tools**

#### `get_symbol_at_position`
Gets detailed symbol information at a specific position in a file.
```json
{
  "filePath": "src/main/java/Example.java",
  "line": 10,
  "column": 15
}
```
**Returns**: Symbol details, declaration location, and documentation.

#### `find_all_references`
Finds all references to a symbol at the specified position.
```json
{
  "filePath": "src/main/java/Example.java",
  "line": 10,
  "column": 15
}
```
**Returns**: List of all references with file locations and usage context.

## IntelliJ IDEA Functionalities Used

This plugin extensively leverages IntelliJ IDEA's rich APIs and frameworks:

### 🏗️ **PSI (Program Structure Interface)**
The foundation of all AST analysis in this plugin.

**Core PSI Classes Used:**
- `PsiFile` & `PsiJavaFile` - File-level AST representation
- `PsiClass` - Class declarations and structures
- `PsiMethod` - Method declarations and bodies
- `PsiStatement` - Individual statements within methods
- `PsiElement` - Base interface for all PSI elements
- `PsiModifierList` - Access modifiers and annotations
- `PsiParameterList` - Method parameters

**PSI Utilities:**
- `PsiTreeUtil` - Navigation and traversal of PSI trees
- `PsiManager` - Central manager for PSI operations
- `JavaPsiFacade` - Java-specific PSI operations
- `PsiDocumentManager` - Integration between PSI and Document APIs

### 📄 **Document API**
For precise line number calculations and text operations.

**Key Classes:**
- `Document` - Text document representation
- `FileDocumentManager` - File-to-document mapping
- `TextRange` - Text position and range handling

**Functionality:**
- Line number calculations from text offsets
- Column position determination
- Range-based text analysis

### 🔍 **Search and Navigation APIs**
For symbol resolution and reference finding.

**Key Components:**
- `GlobalSearchScope` - Search scope definition
- `ReferencesSearch` - Finding symbol references
- `ClassInheritorsSearch` - Class hierarchy analysis
- Symbol resolution and navigation

### 🐛 **Debug API**
For runtime inspection and debugging support.

**Debug Components:**
- Debug session management
- Stack frame inspection
- Variable evaluation
- Breakpoint management
- Thread state monitoring

### 📊 **Code Analysis Framework**
For complexity calculation and pattern detection.

**Analysis Features:**
- Control flow analysis
- Cyclomatic complexity calculation
- Code pattern recognition
- Statement classification
- Method signature analysis

### 🗂️ **Virtual File System (VFS)**
For file system operations and project navigation.

**VFS Components:**
- `VirtualFile` - File system abstraction
- `LocalFileSystem` - Local file access
- Project path resolution
- File type detection

## Technical Implementation Details

### **Enhanced Method Analysis**
The plugin provides sophisticated method analysis capabilities:

```kotlin
// Example: Getting first executable line
fun getFirstExecutableLineNumber(method: PsiMethod): Int? {
    val body = method.body ?: return null
    val firstStatement = body.statements.firstOrNull { statement ->
        !isNonExecutableStatement(statement)
    } ?: return null
    return getLineNumber(firstStatement)
}
```

### **Smart Breakpoint Suggestions**
The tool analyzes method bodies to suggest optimal breakpoint locations:

- **First Executable Line**: The actual first line of code execution
- **Decision Points**: Conditional statements and branches
- **Loop Entry Points**: For iteration debugging
- **Method Exit Points**: Return statements and method endings

### **Comprehensive Line Range Analysis**
Each method analysis includes:

```kotlin
data class MethodLineRange(
    val signatureLineNumber: Int,      // Method declaration line
    val firstExecutableLineNumber: Int?, // First executable statement
    val lastLineNumber: Int,           // Method closing brace
    val bodyStartLine: Int?,          // Opening brace
    val bodyEndLine: Int?             // Closing brace
)
```

### **Statement Classification**
The plugin categorizes PSI statements for better analysis:

- **DECLARATION** - Variable declarations
- **ASSIGNMENT** - Assignment operations
- **METHOD_CALL** - Method invocations
- **IF_STATEMENT** - Conditional logic
- **LOOP** variants - For, while, foreach loops
- **RETURN** - Method exit points
- **EXCEPTION** handling - Try-catch blocks

## Architecture

### **Plugin Structure**
```
src/main/kotlin/ai/voitta/jetbrains/
├── MyCustomTool.kt                    # Original demo tool
├── ast/
│   ├── AstDataTypes.kt               # Enhanced data structures
│   ├── AstUtils.kt                   # PSI utility functions
│   ├── debug/
│   │   └── DebugTools.kt             # Debug API integration
│   ├── java/
│   │   └── JavaAstTools.kt           # Java PSI analysis tools
│   └── common/
│       └── NavigationTools.kt        # Symbol navigation tools
└── utils/
    └── JsonUtils.kt                  # JSON serialization utilities
```

### **Key Design Principles**

1. **PSI-First Approach**: All analysis is built on IntelliJ's PSI framework
2. **Language-Specific Tools**: Java tools leverage Java PSI elements
3. **Comprehensive Analysis**: Multiple perspectives on the same code elements
4. **Precise Line Mapping**: Accurate line number calculation for debugging
5. **Smart Suggestions**: AI-friendly breakpoint and analysis suggestions

## Dependencies

- **Main MCP Server Plugin** (`com.intellij.mcpServer`)
- **IntelliJ Java Plugin** (`com.intellij.modules.java`) - For Java PSI support
- **IntelliJ Platform** (`com.intellij.modules.platform`) - Core APIs
- **Kotlin Serialization** - For data structure serialization

## Usage Examples

### **Setting Breakpoints on First Executable Lines**
```javascript
// Get method details with line information
const methodDetails = await jetbrains.get_method_details({
  filePath: "src/main/java/Example.java"
});

// Extract first executable lines for breakpoint setting
methodDetails.forEach(method => {
  if (method.lineRange.firstExecutableLineNumber > 0) {
    // Set breakpoint on first executable line, not method signature
    jetbrains.toggle_debugger_breakpoint({
      filePathInProject: method.file,
      line: method.lineRange.firstExecutableLineNumber
    });
  }
});
```

### **Smart Breakpoint Suggestions**
```javascript
// Get smart breakpoint suggestions
const suggestions = await jetbrains.suggest_breakpoint_lines({
  filePath: "src/main/java/Example.java",
  methodName: "complexMethod"
});

// Set breakpoints based on priority
suggestions
  .filter(s => s.priority === "HIGH")
  .forEach(suggestion => {
    console.log(`Setting ${suggestion.reason} breakpoint at line ${suggestion.lineNumber}: ${suggestion.description}`);
  });
```

