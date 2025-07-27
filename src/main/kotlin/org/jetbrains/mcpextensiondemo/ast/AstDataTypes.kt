package org.jetbrains.mcpextensiondemo.ast

import kotlinx.serialization.Serializable

/**
 * Core data structures for AST representation in MCP tools
 */

@Serializable
data class ClassNode(
    val name: String?,
    val fullyQualifiedName: String?,
    val superClass: String?,
    val interfaces: List<String> = emptyList(),
    val modifiers: List<String> = emptyList(),
    val methods: List<MethodNode> = emptyList(),
    val fields: List<FieldNode> = emptyList(),
    val innerClasses: List<ClassNode> = emptyList(),
    val annotations: List<AnnotationNode> = emptyList(),
    val javadoc: String? = null,
    val packageName: String? = null,
    val isInterface: Boolean = false,
    val isEnum: Boolean = false,
    val isAbstract: Boolean = false
)

@Serializable
data class MethodNode(
    val name: String?,
    val returnType: String?,
    val parameters: List<ParameterNode> = emptyList(),
    val modifiers: List<String> = emptyList(),
    val annotations: List<AnnotationNode> = emptyList(),
    val javadoc: String? = null,
    val bodyStatements: List<StatementNode>? = null,
    val complexity: Int? = null,
    val isConstructor: Boolean = false,
    val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    val throwsExceptions: List<String> = emptyList(),
    val lineNumber: Int? = null
)

@Serializable
data class FieldNode(
    val name: String?,
    val type: String?,
    val modifiers: List<String> = emptyList(),
    val annotations: List<AnnotationNode> = emptyList(),
    val initializer: String? = null,
    val isStatic: Boolean = false,
    val isFinal: Boolean = false,
    val isVolatile: Boolean = false,
    val lineNumber: Int? = null
)

@Serializable
data class StatementNode(
    val type: String,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val lineNumber: Int? = null,
    val children: List<StatementNode> = emptyList()
)

@Serializable
data class AnnotationNode(
    val name: String,
    val fullyQualifiedName: String? = null,
    val parameters: Map<String, String> = emptyMap()
)

@Serializable
data class ParameterNode(
    val name: String?,
    val type: String?,
    val annotations: List<AnnotationNode> = emptyList(),
    val isVarArgs: Boolean = false,
    val defaultValue: String? = null
)

@Serializable
data class UsageNode(
    val file: String,
    val line: Int,
    val column: Int,
    val context: String,
    val usageType: String, // "READ", "WRITE", "METHOD_CALL", "DECLARATION", "IMPORT"
    val elementType: String // "CLASS", "METHOD", "FIELD", "VARIABLE"
)

@Serializable
data class SymbolInfo(
    val name: String,
    val type: String, // "CLASS", "METHOD", "FIELD", "VARIABLE", "PARAMETER", "LOCAL_VARIABLE"
    val declarationFile: String?,
    val declarationLine: Int?,
    val declarationColumn: Int? = null,
    val signature: String?,
    val documentation: String?,
    val modifiers: List<String> = emptyList(),
    val fullyQualifiedName: String? = null,
    val containingClass: String? = null,
    val returnType: String? = null,
    val parameterTypes: List<String> = emptyList()
)

@Serializable
data class HierarchyNode(
    val className: String,
    val fullyQualifiedName: String,
    val type: String, // "CLASS", "INTERFACE", "ENUM", "ANNOTATION"
    val file: String?,
    val superClasses: List<HierarchyNode> = emptyList(),
    val subClasses: List<HierarchyNode> = emptyList(),
    val interfaces: List<HierarchyNode> = emptyList(),
    val implementingClasses: List<HierarchyNode> = emptyList()
)

@Serializable
data class CallHierarchyNode(
    val methodName: String,
    val className: String,
    val signature: String,
    val file: String?,
    val line: Int?,
    val callers: List<CallHierarchyNode> = emptyList(),
    val callees: List<CallHierarchyNode> = emptyList()
)

@Serializable
data class ComplexityMetric(
    val methodName: String,
    val className: String,
    val cyclomaticComplexity: Int,
    val cognitiveComplexity: Int? = null,
    val linesOfCode: Int,
    val parameterCount: Int,
    val file: String,
    val lineNumber: Int
)

@Serializable
data class CodePattern(
    val patternType: String, // "NULL_CHECK", "EXCEPTION_HANDLING", "LOOP", "SINGLETON", etc.
    val description: String,
    val file: String,
    val startLine: Int,
    val endLine: Int,
    val codeSnippet: String,
    val severity: String = "INFO", // "INFO", "WARNING", "ERROR"
    val suggestion: String? = null
)

// Debug-related data structures
@Serializable
data class StackFrameNode(
    val methodName: String,
    val className: String,
    val fileName: String?,
    val lineNumber: Int,
    val isInUserCode: Boolean,
    val variables: List<VariableNode> = emptyList(),
    val frameIndex: Int = 0
)

@Serializable
data class VariableNode(
    val name: String,
    val value: String?,
    val type: String,
    val scope: String, // "LOCAL", "PARAMETER", "FIELD", "STATIC"
    val isExpandable: Boolean = false,
    val children: List<VariableNode> = emptyList(),
    val isPrimitive: Boolean = true
)

@Serializable
data class DebugSessionInfo(
    val isActive: Boolean,
    val isSuspended: Boolean,
    val suspendedThreadName: String?,
    val currentBreakpoint: String?,
    val totalThreads: Int,
    val suspendReason: String?, // "BREAKPOINT", "STEP", "EXCEPTION", "MANUAL"
    val sessionStartTime: String? = null
)

@Serializable
data class EvaluationResult(
    val value: String?,
    val type: String?,
    val success: Boolean,
    val error: String? = null,
    val executionTimeMs: Long? = null
)

@Serializable
data class ExceptionInfo(
    val type: String,
    val message: String?,
    val stackTrace: List<StackFrameNode>,
    val cause: ExceptionInfo? = null,
    val suppressedExceptions: List<ExceptionInfo> = emptyList()
)

// Error handling
@Serializable
data class AstError(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val file: String? = null,
    val line: Int? = null
)
