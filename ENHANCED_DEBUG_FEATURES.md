# Enhanced Debug Features

This document describes the enhanced debugging capabilities provided by the JetBrains-Voitta plugin with rich variable inspection and full stack trace support.

## New Debug Tools

### 1. Enhanced Stack Trace Tool (`get_current_stack_trace`)

**Improvements:**
- **Full stack trace extraction**: Now retrieves complete call stack (up to 20 frames) instead of just the top frame
- **Java-specific frame analysis**: Leverages JDI (Java Debug Interface) for detailed frame information
- **Enhanced frame metadata**: Includes accurate method names, class names, file names, and line numbers
- **User code detection**: Intelligently identifies user code vs. system/library code

**Usage:**
```json
{
  "tool": "get_current_stack_trace",
  "arguments": {}
}
```

**Enhanced Response:**
```json
[
  {
    "methodName": "validateAndStore",
    "className": "TestEnhancedDebugFeatures", 
    "fileName": "TestEnhancedDebugFeatures.java",
    "lineNumber": 45,
    "isInUserCode": true,
    "frameIndex": 0
  },
  {
    "methodName": "processData",
    "className": "TestEnhancedDebugFeatures",
    "fileName": "TestEnhancedDebugFeatures.java", 
    "lineNumber": 35,
    "isInUserCode": true,
    "frameIndex": 1
  }
]
```

### 2. Enhanced Debug Snapshot Tool (`get_debug_snapshot`)

**Improvements:**
- **Rich variable inspection**: Extracts actual variable values, not just placeholders
- **Object field expansion**: Can expand object contents to show field values
- **Complete debug context**: Combines session info, stack trace, and variables in one call
- **Configurable depth**: Control expansion depth to prevent overwhelming output

**Enhanced Parameters:**
- `includeVariables`: Include current frame variables (default: true)
- `includeStackTrace`: Include full stack trace (default: true) 
- `expandObjects`: Expand object fields in variables (default: false)
- `maxStackFrames`: Maximum stack frames to include (default: 10)

**Usage:**
```json
{
  "tool": "get_debug_snapshot",
  "arguments": {
    "includeVariables": true,
    "includeStackTrace": true,
    "expandObjects": true,
    "maxStackFrames": 5
  }
}
```

### 3. New Expression Evaluation Tool (`evaluate_expression`)

**Features:**
- **Dynamic expression evaluation**: Execute Java expressions in current debug context
- **Variable access**: Access local variables, parameters, and object fields
- **Method calls**: Call methods on objects during debugging
- **Type information**: Returns both value and type information
- **Performance metrics**: Tracks evaluation execution time

**Parameters:**
- `expression`: Java expression to evaluate (e.g., "user.getName()", "localVar + 5")
- `frameIndex`: Stack frame index (default: 0 for current frame)
- `timeout`: Evaluation timeout in milliseconds (default: 5000)

**Usage:**
```json
{
  "tool": "evaluate_expression", 
  "arguments": {
    "expression": "userData.getFormattedInfo()",
    "frameIndex": 0,
    "timeout": 5000
  }
}
```

**Response:**
```json
{
  "value": "John Doe (31) - 123 Main St, Anytown",
  "type": "String",
  "success": true,
  "error": null,
  "executionTimeMs": 45
}
```

### 4. New Frame Variables Tool (`get_frame_variables`)

**Features:**
- **Comprehensive variable extraction**: Gets all variables in specified frame
- **Categorized by scope**: Local variables, parameters, fields, static fields
- **Object field expansion**: Recursively expand object contents
- **Hierarchical structure**: Variables with children for complex objects

**Parameters:**
- `frameIndex`: Stack frame index (default: 0)
- `includeFields`: Include object fields (default: true)
- `includeParameters`: Include method parameters (default: true)
- `includeLocals`: Include local variables (default: true)
- `expandObjects`: Expand object contents (default: false)
- `maxDepth`: Maximum expansion depth for objects (default: 2)

**Usage:**
```json
{
  "tool": "get_frame_variables",
  "arguments": {
    "frameIndex": 0,
    "expandObjects": true,
    "maxDepth": 2
  }
}
```

**Enhanced Response:**
```json
[
  {
    "name": "data",
    "value": "\"HELLO WORLD_42\"",
    "type": "String", 
    "scope": "PARAMETER",
    "isExpandable": false,
    "isPrimitive": true,
    "children": []
  },
  {
    "name": "this",
    "value": "TestEnhancedDebugFeatures{instanceField=\"HELLO WORLD_42\", counter=1}",
    "type": "TestEnhancedDebugFeatures",
    "scope": "FIELD", 
    "isExpandable": true,
    "isPrimitive": false,
    "children": [
      {
        "name": "instanceField",
        "value": "\"HELLO WORLD_42\"",
        "type": "String",
        "scope": "FIELD",
        "isExpandable": false,
        "isPrimitive": true
      },
      {
        "name": "counter", 
        "value": "1",
        "type": "int",
        "scope": "FIELD",
        "isExpandable": false,
        "isPrimitive": true
      }
    ]
  }
]
```

## Technical Implementation Details

### Java Debug Interface (JDI) Integration

The enhanced debugging tools leverage IntelliJ's JDI integration to provide:

1. **Direct JVM Access**: Connect directly to the debugged JVM for accurate variable values
2. **Type-Safe Operations**: Use JDI's type system for proper value extraction
3. **Stack Frame Navigation**: Navigate through the complete call stack
4. **Object Introspection**: Examine object fields and method signatures

### Enhanced Stack Trace Extraction

```kotlin
private fun getStackFrames(session: XDebugSession): List<StackFrameNode> {
    if (topFrame is JavaStackFrame) {
        val stackFrameProxy = topFrame.stackFrameProxy
        val threadProxy = stackFrameProxy.threadProxy()
        
        // Get all frames from the thread
        val allFrames = threadProxy.frames()
        for (i in 1 until minOf(allFrames.size, 20)) {
            // Process each frame using JDI
        }
    }
}
```

### Variable Value Extraction

```kotlin
private fun extractJavaVariables(frame: JavaStackFrame, expandObjects: Boolean): List<VariableNode> {
    val stackFrameProxy = frame.stackFrameProxy
    
    // Get local variables using JDI
    val localVariables = stackFrameProxy.visibleVariables()
    
    // Get method parameters
    val argumentValues = stackFrameProxy.getArgumentValues()
    
    // Get 'this' object and fields
    val thisObject = stackFrameProxy.thisObject()
}
```

## Testing the Enhanced Features

Use the provided `TestEnhancedDebugFeatures.java` file:

1. **Set breakpoints** at the marked locations
2. **Run in debug mode**
3. **When stopped at breakpoint**, use the enhanced debug tools:

### Test Scenarios

**Test 1: Local Variables**
- Breakpoint in `demonstrateVariableInspection()`
- Use `get_frame_variables` to see local variables
- Use `evaluate_expression` with `"localVariable.length()"`

**Test 2: Method Parameters**  
- Breakpoint in `processData()`
- Inspect method parameters and local processing
- Evaluate `"input.toLowerCase()"`

**Test 3: Object Fields**
- Breakpoint in `validateAndStore()`
- Use `get_debug_snapshot` with `expandObjects: true`
- See instance field modifications

**Test 4: Object Hierarchy**
- Breakpoint in `demonstrateObjectHierarchy()`
- Evaluate `"userData.address.getFullAddress()"`
- Expand `userData` object to see nested structure

## Benefits of Enhanced Debugging

1. **Comprehensive Context**: Get complete debugging picture in single calls
2. **Rich Variable Inspection**: See actual values, not just placeholders
3. **Interactive Evaluation**: Test expressions dynamically during debugging
4. **Object Exploration**: Navigate complex object hierarchies
5. **Performance Aware**: Built-in timeouts and performance metrics
6. **IDE Integration**: Leverages IntelliJ's powerful debugging infrastructure

## Error Handling

The tools provide graceful error handling:
- **Timeout protection** for long-running evaluations
- **Graceful degradation** when variable access fails
- **Helpful error messages** with guidance for alternative approaches
- **Fallback mechanisms** for different JVM configurations

This enhanced debugging capability transforms the plugin from basic inspection to a powerful runtime analysis tool suitable for complex debugging scenarios.
