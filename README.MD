# JetBrains-Voitta - Enhanced AST and Debug Tools

This is JetBrains-Voitta, an enhanced JetBrains IDE plugin that extends the MCP (Model Context Protocol) server with advanced AST analysis and debugging tools for both **Java and PHP**. This plugin leverages IntelliJ IDEA's and PhpStorm's powerful PSI (Program Structure Interface) and debugging APIs to provide deep code analysis and runtime inspection capabilities across multiple languages.

This project was inspired by the [original demo plugin](https://github.com/MaXal/mcpExtensionPlugin) as reference. For Claude integration,

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

### **Quick Start**

1. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd jetbrains-voitta
   ./gradlew build
   ```

2. **Run the Plugin**
   ```bash
   # Run in IntelliJ IDEA (default)
   ./gradlew runIde
   
   # Run in PhpStorm
   ./gradlew runPhpStorm
   ```

### **Available Gradle Tasks**

The plugin provides three gradle tasks for testing in different environments:

| Task | Description | IDE |
|------|-------------|-----|
| `./gradlew runIde` | Default task - runs IntelliJ IDEA Ultimate Edition | IntelliJ Ultimate |
| `./gradlew runIntelliJ` | Explicit IntelliJ IDEA task (same as runIde) | IntelliJ Ultimate |
| `./gradlew runPhpStorm` | Runs with full PHP support | IntelliJ Ultimate with PHP |

All tasks use IntelliJ Ultimate Edition 2024.3 as the base platform with both Java and PHP plugins bundled, providing complete language support for both Java and PHP development.

### **Development Workflow**

#### Testing in Different IDEs
```bash
# Default task - runs IntelliJ IDEA Community Edition
./gradlew runIde

# Explicit IntelliJ IDEA task
./gradlew runIntelliJ

# Run with PHP support
./gradlew runPhpStorm
```

#### Build Configuration
The plugin is configured to work with both Java and PHP out of the box:
- Base Platform: IntelliJ Ultimate Edition 2024.3
- Bundled Plugins: Java, PHP (native Ultimate support)
- Java Version: JDK 21+
- Kotlin Version: 1.9.24
- License: Requires Ultimate Edition or PhpStorm license

### **Architecture Overview**

#### Single Plugin, Multi-IDE Support
```
JetBrains-Voitta Plugin
├── Universal Tools (Language-Agnostic)
│   ├── get_file_ast           # Auto-detects Java/PHP
│   ├── get_method_details     # Works with both languages
│   ├── suggest_breakpoint_lines
│   └── detect_code_patterns
├── Language-Specific Tools
│   ├── Java Tools (get_java_file_ast, etc.)
│   └── PHP Tools (get_php_file_ast, etc.)
└── Debug Tools (IDE-Agnostic)
    ├── get_debug_snapshot
    ├── evaluate_expression
    └── get_current_stack_trace
```

#### Language Analyzer Framework
```kotlin
LanguageAstAnalyzer (Abstract)
├── JavaAstAnalyzer    # Java PSI implementation
└── PhpAstAnalyzer     # PHP PSI implementation

LanguageAnalyzerFactory
└── Auto-detects file type and routes to appropriate analyzer
```

## Installation

JetbrainsVoitta can be installed in several ways depending on your setup and requirements.

### **Prerequisites**

Before installing JetBrains-Voitta, ensure you have:

1. **IntelliJ IDEA Ultimate 2024.3+ or PhpStorm 2024.3+** (Ultimate/Professional Edition required for PHP support)
2. **Java JDK 21+** configured in your IDE  
3. **MCP Server Plugin** installed and enabled
   - Go to **Settings** → **Plugins** → **Marketplace**
   - Search for "MCP Server" and install it
   - Restart your IDE after installation

> **Note**: This plugin requires **Ultimate Edition** or **PhpStorm** for full PHP support. Community Edition is not currently supported.

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

## Testing the Plugin

### **Running the Plugin**

The plugin can be tested with full Java and PHP support using simple gradle commands:

```bash
# Run in IntelliJ IDEA Ultimate (default)
./gradlew runIde

# Run in IntelliJ IDEA Ultimate (explicit)
./gradlew runIntelliJ

# Run with full PHP support
./gradlew runPhpStorm
```

All commands launch IntelliJ Ultimate Edition 2024.3 with the plugin loaded. Both Java and PHP languages are fully supported with native IDE integration.

### **What to Test**

#### Universal Tools (Work in Both IDEs)
- `get_file_ast` - Test with `.java` files in IntelliJ, `.php` files in PhpStorm
- `get_method_details` - Verify method analysis works for both languages
- `suggest_breakpoint_lines` - Check breakpoint suggestions
- `detect_code_patterns` - Test pattern detection

#### Language-Specific Tools
- **In IntelliJ IDEA**: Test Java-specific tools (`get_java_file_ast`, etc.)
- **In PhpStorm**: Test PHP-specific tools (`get_php_file_ast`, etc.)

#### Debug Tools (Work in Both IDEs)
- Set breakpoints and test `get_debug_snapshot`
- Test `evaluate_expression` during debugging
- Verify `get_current_stack_trace` functionality

### Development Installation

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

### 🌐 **Universal Language-Agnostic Tools** (Recommended)

These tools automatically detect the file language (Java or PHP) and use the appropriate analyzer:

#### `get_file_ast`
**NEW**: Universal AST analysis that works with both Java and PHP files.
```json
{
  "filePath": "src/main/java/Example.java",  // or "src/Example.php"
  "includeMethodBodies": true,
  "includePrivateMembers": true
}
```
**Returns**: Complete class structure with language detection and method details.

#### `get_method_details`
**NEW**: Universal method analysis for both Java and PHP.
```json
{
  "filePath": "src/Example.java",  // or "src/Example.php"
  "methodName": "myMethod"  // optional
}
```
**Returns**: Comprehensive method information including breakpoint suggestions.

#### `suggest_breakpoint_lines`
**NEW**: Universal breakpoint suggestions for both languages.
```json
{
  "filePath": "src/Example.java",  // or "src/Example.php"
  "methodName": "myMethod"  // optional
}
```
**Returns**: Smart breakpoint suggestions with language-appropriate analysis.

#### `get_method_complexity`
**NEW**: Universal complexity analysis.
```json
{
  "filePath": "src/Example.java",  // or "src/Example.php"
  "methodName": "myMethod"  // optional
}
```
**Returns**: Cyclomatic complexity and other metrics.

#### `detect_code_patterns`
**NEW**: Universal code pattern detection.
```json
{
  "filePath": "src/Example.java",  // or "src/Example.php"
  "patterns": ["null_checks", "exception_handling", "loops", "all"]
}
```
**Returns**: Language-appropriate code pattern analysis.

#### `get_class_hierarchy`
**NEW**: Universal class hierarchy analysis.
```json
{
  "className": "com.example.MyClass",  // or "MyPhpClass"
  "direction": "both",
  "language": "Java"  // optional hint: "Java" or "PHP"
}
```
**Returns**: Complete class hierarchy information.

### 🐘 **PHP-Specific AST Analysis Tools**

#### `get_php_file_ast`
Retrieves comprehensive AST structure for PHP files with enhanced method analysis.
```json
{
  "filePath": "src/Example.php",
  "includeMethodBodies": true,
  "includePrivateMembers": true
}
```
**Returns**: Complete class structure with PHP method details including first executable line numbers, line ranges, and complexity metrics.

#### `get_php_method_details`
**NEW**: Provides detailed PHP method analysis including breakpoint suggestions and line ranges.
```json
{
  "filePath": "src/Example.php",
  "methodName": "myMethod"  // optional
}
```
**Returns**: Comprehensive PHP method information including:
- Method signature line number
- First executable line number
- Method body line range
- Complexity metrics
- Smart breakpoint suggestions
- Parameter details

#### `suggest_php_breakpoint_lines`
**NEW**: Suggests optimal breakpoint locations in PHP methods.
```json
{
  "filePath": "src/Example.php",
  "methodName": "myMethod"  // optional
}
```
**Returns**: Smart breakpoint suggestions with reasons (FIRST_EXECUTABLE, DECISION_POINT, METHOD_EXIT, etc.)

#### `get_php_method_complexity`
Calculates cyclomatic complexity and other metrics for PHP methods.
```json
{
  "filePath": "src/Example.php",
  "methodName": "myMethod"  // optional
}
```
**Returns**: Complexity metrics including cyclomatic complexity, lines of code, and parameter count.

#### `detect_php_code_patterns`
Detects common PHP code patterns and potential issues.
```json
{
  "filePath": "src/Example.php",
  "patterns": ["null_checks", "exception_handling", "loops", "all"]
}
```
**Returns**: PHP-specific code pattern analysis with suggestions for improvement.

#### `get_php_class_hierarchy`
Analyzes PHP class inheritance relationships and interfaces.
```json
{
  "className": "MyPhpClass",
  "direction": "both"  // "up", "down", "both"
}
```

### 🔍 **Java-Specific AST Analysis Tools**

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

## Multi-IDE Development Benefits

### **Why Single Plugin Architecture?**

#### ✅ **Unified Development Experience**
- Same tool names work in both IntelliJ IDEA and PhpStorm
- Universal tools automatically detect language and route appropriately
- Debug tools work identically across IDEs
- Single installation, works everywhere

#### ✅ **Simplified Maintenance**
- One codebase for both Java and PHP support
- Single plugin.xml configuration
- Unified build and release process
- Shared debug infrastructure (90% of the codebase)

#### ✅ **Developer Productivity**
- Learn once, use everywhere approach
- Consistent API across languages
- Same debugging workflow for Java and PHP
- Easy testing with `./gradlew runIde` vs `./gradlew runPhpStorm`

### **Architecture Decisions**

#### Language Detection Strategy
```kotlin
// Auto-detection based on file extension and PSI type
val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
when (analyzer?.languageName) {
    "Java" -> // Use Java PSI APIs
    "PHP"  -> // Use PHP PSI APIs  
    else   -> // Unsupported language error
}
```

#### Universal vs Language-Specific Tools
- **Universal Tools**: Recommended for most use cases
  - `get_file_ast` (works with .java and .php)
  - `get_method_details` (language-agnostic)
  - `suggest_breakpoint_lines` (smart routing)
  
- **Language-Specific Tools**: For specialized scenarios
  - `get_java_file_ast` (Java-only features)
  - `get_php_file_ast` (PHP-only features)
  - Fine-grained control when needed

### **Testing Strategy**

```bash
# Development workflow
git clone <repo>
cd jetbrains-voitta

# Test Java functionality
./gradlew runIde
# Open .java files, test Java-specific features

# Test PHP functionality  
./gradlew runPhpStorm
# Open .php files, test PHP-specific features

# Both IDEs test debug tools identically
```

### **Future Language Support**

Adding new languages (Python, JavaScript, etc.) is straightforward:

1. **Create Language Analyzer**
   ```kotlin
   class PythonAstAnalyzer : LanguageAstAnalyzer() {
       override val languageName = "Python"
       override fun canAnalyze(psiFile: PsiFile) = psiFile is PyFile
       // Implement abstract methods...
   }
   ```

2. **Register in Factory**
   ```kotlin
   LanguageAnalyzerFactory.registerAnalyzer(PythonAstAnalyzer())
   ```

3. **Universal tools automatically work** with the new language

4. **Add language-specific tools** if needed for specialized features

This architecture scales naturally to support the entire JetBrains IDE ecosystem while maintaining a single, maintainable codebase.
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

### **Multi-IDE Compatibility**
The plugin provides the same tools across both IntelliJ IDEA and PhpStorm:

```javascript
// Universal tool - works in both IDEs, auto-detects language
const fileAst = await jetbrains.get_file_ast({
  filePath: "src/Example.java"    // or "src/Example.php"
});

// Response includes language detection
console.log(`Analyzing ${fileAst[0].language} file`);
// Output: "Analyzing Java file" or "Analyzing PHP file"
```

### **Language-Agnostic Development Workflow**
```javascript
// Same workflow for Java and PHP development
const analyzeFile = async (filePath) => {
  // Get AST (works for .java and .php files)
  const ast = await jetbrains.get_file_ast({ filePath });
  
  // Get method details with breakpoint suggestions
  const methods = await jetbrains.get_method_details({ filePath });
  
  // Set breakpoints on first executable lines
  for (const method of methods) {
    if (method.lineRange.firstExecutableLineNumber > 0) {
      await jetbrains.toggle_debugger_breakpoint({
        filePathInProject: filePath,
        line: method.lineRange.firstExecutableLineNumber
      });
    }
  }
};

// Works the same way for both languages
await analyzeFile("src/main/java/MyClass.java");
await analyzeFile("src/MyClass.php");
```

### **IDE-Specific Testing Scenarios**

#### IntelliJ IDEA Testing (Java Focus)
```javascript
// Test Java-specific features
const javaComplexity = await jetbrains.get_java_method_complexity({
  filePath: "src/main/java/Example.java"
});

// Test universal tools with Java files
const breakpoints = await jetbrains.suggest_breakpoint_lines({
  filePath: "src/main/java/Example.java",
  methodName: "calculateTotal"
});
```

#### PhpStorm Testing (PHP Focus)
```javascript
// Test PHP-specific features
const phpPatterns = await jetbrains.detect_php_code_patterns({
  filePath: "src/Example.php",
  patterns: ["null_checks", "exception_handling"]
});

// Test universal tools with PHP files
const hierarchy = await jetbrains.get_class_hierarchy({
  className: "MyPhpClass",
  direction: "both"
});
```

### **Cross-IDE Debug Workflow**
```javascript
// Debug workflow works identically in both IDEs
const debugWorkflow = async () => {
  // Set breakpoint (works in both IntelliJ IDEA and PhpStorm)
  await jetbrains.toggle_debugger_breakpoint({
    filePathInProject: "src/Example.java", // or "src/Example.php"
    line: 15
  });
  
  // When breakpoint hits, analyze debug state
  const debugInfo = await jetbrains.get_debug_snapshot({
    includeVariables: true,
    includeStackTrace: true,
    expandObjects: false
  });
  
  // Evaluate expressions in current context
  const result = await jetbrains.evaluate_expression({
    expression: "localVariable.toString()", // Java
    // expression: "$localVariable",        // PHP
    frameIndex: 0
  });
  
  console.log("Debug state:", debugInfo);
  console.log("Expression result:", result);
};
```

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

---

## 🚧 TODO: Community Edition Support

**Current Status**: This plugin currently requires **IntelliJ Ultimate Edition** or **PhpStorm** for full functionality.

### For Community Edition Users

If you're using IntelliJ IDEA Community Edition, you have these options:

#### Option 1: Java-Only Support (Recommended for Community)
- Modify `build.gradle.kts` to use `"IC"` instead of `"IU"`
- Remove PHP plugin dependency: `bundledPlugin("com.jetbrains.php")`
- Comment out PHP analyzer registration in `LanguageAstAnalyzer.kt`
- All Java features will work perfectly
- Universal tools will work with Java files

#### Option 2: External PHP Plugin Dependency
- Keep Community Edition base but add PHP as external dependency
- Add `plugin("com.jetbrains.php", "243.12818.47")` to dependencies
- Implement runtime detection for graceful fallback
- More complex but provides PHP support

#### Option 3: Upgrade to Ultimate
- Purchase IntelliJ Ultimate license
- Get full Java + PHP + more IDE features
- Simplest solution for professional development

### Implementation Tasks for Community Support

**High Priority:**
- [ ] Create Community Edition build variant
- [ ] Implement graceful PHP plugin detection
- [ ] Add conditional PHP tool registration
- [ ] Update documentation with Community Edition instructions

**Medium Priority:**
- [ ] Add build profiles for different IDE targets
- [ ] Create automated testing for Community Edition
- [ ] Implement feature detection UI
- [ ] Add Community Edition CI/CD pipeline

**Low Priority:**
- [ ] Create separate Community Edition distribution
- [ ] Add runtime IDE detection and feature flagging
- [ ] Implement plugin marketplace compatibility matrix

### Contributing

If you'd like to help add Community Edition support, please:
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/community-edition`
3. Implement one of the options above
4. Submit a pull request with tests

### Contact

For Community Edition support questions or contributions, please:
- Open an issue with the `community-edition` label
- Join our Discord/Slack channel for development discussions
- Submit feature requests for specific Community Edition needs

