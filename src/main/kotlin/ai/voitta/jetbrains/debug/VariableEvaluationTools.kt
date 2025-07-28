package ai.voitta.jetbrains.debug

import ai.voitta.jetbrains.ast.VariableNode
import ai.voitta.jetbrains.ast.EvaluationResult
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import kotlinx.serialization.Serializable
import org.jetbrains.ide.mcp.Response
import org.jetbrains.mcpserverplugin.AbstractMcpTool
import ai.voitta.jetbrains.utils.JsonUtils
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator.XEvaluationCallback
import com.intellij.xdebugger.frame.XValue
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.sun.jdi.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import com.intellij.openapi.diagnostic.Logger

/**
 * Enhanced variable evaluation tools for detailed debugging inspection
 */

@Serializable
data class EvaluateExpressionArgs(
    val expression: String,
    val frameIndex: Int = 0,
    val timeout: Long = 5000 // milliseconds
)

@Serializable
data class GetFrameVariablesArgs(
    val frameIndex: Int = 0,
    val includeFields: Boolean = true,
    val includeParameters: Boolean = true,
    val includeLocals: Boolean = true,
    val expandObjects: Boolean = false,
    val maxDepth: Int = 2
)

class EvaluateExpressionTool : AbstractMcpTool<EvaluateExpressionArgs>(EvaluateExpressionArgs.serializer()) {
    override val name = "evaluate_expression"
    override val description = """
        Evaluates a Java expression in the current debug context.
        Supports accessing variables, calling methods, and complex expressions.
        
        Parameters:
        - expression: Java expression to evaluate (e.g., "user.getName()", "localVar + 5")
        - frameIndex: Stack frame index (default: 0 for current frame)
        - timeout: Evaluation timeout in milliseconds (default: 5000)
        
        Returns: EvaluationResult with value, type, and execution details
        Error: "No active debug session" if not debugging or evaluation fails
    """
    
    override fun handle(project: Project, args: EvaluateExpressionArgs): Response {
        return try {
            val session = getCurrentDebugSession(project)
                ?: return Response(error = "No active debug session or execution not suspended")
            
            val result = evaluateExpression(session, args.expression, args.frameIndex, args.timeout)
            
            val resultJson = """
            {
                "value": "${JsonUtils.escapeJson(result.value ?: "")}",
                "type": "${JsonUtils.escapeJson(result.type ?: "")}",
                "success": ${result.success},
                "error": "${JsonUtils.escapeJson(result.error ?: "")}",
                "executionTimeMs": ${result.executionTimeMs ?: 0}
            }
            """.trimIndent()
            
            Response(resultJson)
            
        } catch (e: Exception) {
            Response(error = "Error evaluating expression: ${e.message}")
        }
    }
    
    private fun getCurrentDebugSession(project: Project): XDebugSession? {
        val debuggerManager = XDebuggerManager.getInstance(project)
        return debuggerManager.currentSession?.takeIf { it.isSuspended }
    }
    
    private fun evaluateExpression(
        session: XDebugSession,
        expression: String,
        frameIndex: Int,
        timeout: Long
    ): EvaluationResult {
        val startTime = System.currentTimeMillis()
        
        try {
            val executionStack = session.suspendContext?.activeExecutionStack
                ?: return EvaluationResult(
                    value = null,
                    type = null,
                    success = false,
                    error = "No execution stack available"
                )
            
            val frame = if (frameIndex == 0) {
                executionStack.topFrame
            } else {
                // For non-top frames, we'd need to implement frame navigation
                // This is complex and depends on the debugger implementation
                executionStack.topFrame
            }
            
            frame ?: return EvaluationResult(
                value = null,
                type = null,
                success = false,
                error = "Frame not available at index $frameIndex"
            )
            
            val evaluator = frame.evaluator
                ?: return EvaluationResult(
                    value = null,
                    type = null,
                    success = false,
                    error = "No evaluator available for current frame"
                )
            
            // Use CompletableFuture to handle async evaluation
            val future = CompletableFuture<EvaluationResult>()
            
            evaluator.evaluate(expression, object : XDebuggerEvaluator.XEvaluationCallback {
                override fun evaluated(result: XValue) {
                    try {
                        val valueString = extractValueString(result)
                        val typeString = extractTypeString(result)
                        val executionTime = System.currentTimeMillis() - startTime
                        
                        future.complete(EvaluationResult(
                            value = valueString,
                            type = typeString,
                            success = true,
                            error = null,
                            executionTimeMs = executionTime
                        ))
                    } catch (e: Exception) {
                        future.complete(EvaluationResult(
                            value = null,
                            type = null,
                            success = false,
                            error = "Error extracting result: ${e.message}",
                            executionTimeMs = System.currentTimeMillis() - startTime
                        ))
                    }
                }
                
                override fun errorOccurred(errorMessage: String) {
                    future.complete(EvaluationResult(
                        value = null,
                        type = null,
                        success = false,
                        error = errorMessage,
                        executionTimeMs = System.currentTimeMillis() - startTime
                    ))
                }
            }, frame.sourcePosition)
            
            // Wait for evaluation with timeout
            return try {
                future.get(timeout, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                EvaluationResult(
                    value = null,
                    type = null,
                    success = false,
                    error = "Evaluation timed out or failed: ${e.message}",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
            
        } catch (e: Exception) {
            return EvaluationResult(
                value = null,
                type = null,
                success = false,
                error = "Evaluation error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    private fun extractValueString(result: XValue): String {
        return try {
            if (result is JavaValue) {
                val descriptor = result.descriptor
                descriptor.value?.toString() ?: "null"
            } else {
                // For non-Java values, use a generic approach
                result.toString()
            }
        } catch (e: Exception) {
            "<error: ${e.message}>"
        }
    }
    
    private fun extractTypeString(result: XValue): String {
        return try {
            if (result is JavaValue) {
                val descriptor = result.descriptor
                descriptor.type?.name() ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

class GetFrameVariablesTool : AbstractMcpTool<GetFrameVariablesArgs>(GetFrameVariablesArgs.serializer()) {
    companion object {
        private val LOG = Logger.getInstance(GetFrameVariablesTool::class.java)
    }
    
    override val name = "get_frame_variables"
    override val description = """
        Retrieves all variables available in a specific stack frame with detailed information.
        
        Parameters:
        - frameIndex: Stack frame index (default: 0 for current frame)
        - includeFields: Include object fields (default: true)
        - includeParameters: Include method parameters (default: true)
        - includeLocals: Include local variables (default: true)
        - expandObjects: Expand object contents (default: false)
        - maxDepth: Maximum expansion depth for objects (default: 2)
        
        Returns: Array of VariableNode with complete variable information
        Error: "No active debug session" if not debugging
    """
    
    override fun handle(project: Project, args: GetFrameVariablesArgs): Response {
        return try {
            val session = getCurrentDebugSession(project)
                ?: return Response(error = "No active debug session or execution not suspended")
            
            val variables = getFrameVariables(session, args)
            
            val variablesJson = variables.joinToString(",\n", prefix = "[", postfix = "]") { variable ->
                """
                {
                    "name": "${JsonUtils.escapeJson(variable.name)}",
                    "value": "${JsonUtils.escapeJson(variable.value ?: "")}",
                    "type": "${JsonUtils.escapeJson(variable.type)}",
                    "scope": "${variable.scope}",
                    "isExpandable": ${variable.isExpandable},
                    "isPrimitive": ${variable.isPrimitive},
                    "children": [${variable.children.joinToString(",") { child ->
                        """
                        {
                            "name": "${JsonUtils.escapeJson(child.name)}",
                            "value": "${JsonUtils.escapeJson(child.value ?: "")}",
                            "type": "${JsonUtils.escapeJson(child.type)}",
                            "scope": "${child.scope}",
                            "isExpandable": ${child.isExpandable},
                            "isPrimitive": ${child.isPrimitive}
                        }
                        """.trimIndent()
                    }}]
                }
                """.trimIndent()
            }
            
            Response(variablesJson)
            
        } catch (e: Exception) {
            Response(error = "Error retrieving frame variables: ${e.message}")
        }
    }
    
    private fun getCurrentDebugSession(project: Project): XDebugSession? {
        val debuggerManager = XDebuggerManager.getInstance(project)
        return debuggerManager.currentSession?.takeIf { it.isSuspended }
    }
    
    private fun getFrameVariables(session: XDebugSession, args: GetFrameVariablesArgs): List<VariableNode> {
        val variables = mutableListOf<VariableNode>()
        
        try {
            val executionStack = session.suspendContext?.activeExecutionStack ?: return variables
            
            LOG.info("Getting variables for frame index ${args.frameIndex}")
            
            val stackFrameProxy = if (args.frameIndex == 0) {
                val topFrame = executionStack.topFrame
                if (topFrame is JavaStackFrame) {
                    topFrame.stackFrameProxy
                } else {
                    LOG.warn("Top frame is not a JavaStackFrame: ${topFrame?.javaClass?.simpleName}")
                    return variables
                }
            } else {
                // Use JDI to access frames by index
                getStackFrameProxyByIndex(session, args.frameIndex)
                    ?: run {
                        LOG.warn("Could not access frame at index ${args.frameIndex}")
                        variables.add(VariableNode(
                            name = "Frame Access",
                            value = "Frame at index ${args.frameIndex} is not accessible. Only ${getAvailableFrameCount(session)} frames available.",
                            type = "Error",
                            scope = "LOCAL",
                            isExpandable = false,
                            isPrimitive = false
                        ))
                        return variables
                    }
            }
            
            LOG.info("Successfully obtained stack frame proxy for index ${args.frameIndex}")
            
            if (stackFrameProxy != null) {
                
                // Get local variables
                if (args.includeLocals) {
                    try {
                        val localVariables = stackFrameProxy.visibleVariables()
                        for (localVar in localVariables) {
                            try {
                                val value = stackFrameProxy.getValue(localVar)
                                val variableNode = createDetailedVariableNode(
                                    localVar.name(),
                                    value,
                                    localVar.typeName(),
                                    "LOCAL",
                                    args.expandObjects,
                                    args.maxDepth
                                )
                                variables.add(variableNode)
                            } catch (e: Exception) {
                                variables.add(VariableNode(
                                    name = localVar.name(),
                                    value = "<unavailable: ${e.message}>",
                                    type = localVar.typeName(),
                                    scope = "LOCAL",
                                    isExpandable = false,
                                    isPrimitive = false
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Local variables not accessible
                    }
                }
                
                // Get method parameters
                if (args.includeParameters) {
                    try {
                        val method = stackFrameProxy.location().method()
                        val argumentTypes = method.argumentTypes()
                        val argumentValues = stackFrameProxy.getArgumentValues()
                        val argumentNames = try {
                            method.argumentTypeNames().mapIndexed { i, _ -> 
                                try {
                                    method.variables().find { it.signature() == argumentTypes[i].signature() }?.name() ?: "arg$i"
                                } catch (e: Exception) {
                                    "arg$i"
                                }
                            }
                        } catch (e: Exception) {
                            argumentValues.indices.map { "arg$it" }
                        }
                        
                        for (i in argumentValues.indices) {
                            try {
                                val value = argumentValues[i]
                                val typeName = if (i < argumentTypes.size) {
                                    argumentTypes[i].name()
                                } else {
                                    "Unknown"
                                }
                                val paramName = if (i < argumentNames.size) {
                                    argumentNames[i]
                                } else {
                                    "arg$i"
                                }
                                
                                val variableNode = createDetailedVariableNode(
                                    paramName,
                                    value,
                                    typeName,
                                    "PARAMETER",
                                    args.expandObjects,
                                    args.maxDepth
                                )
                                variables.add(variableNode)
                            } catch (e: Exception) {
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        // Parameters not accessible
                    }
                }
                
                // Get 'this' object and its fields
                if (args.includeFields) {
                    try {
                        val thisObject = stackFrameProxy.thisObject()
                        if (thisObject != null) {
                            val variableNode = createDetailedVariableNode(
                                "this",
                                thisObject,
                                thisObject.referenceType().name(),
                                "FIELD",
                                args.expandObjects,
                                args.maxDepth
                            )
                            variables.add(variableNode)
                        }
                    } catch (e: Exception) {
                        // 'this' not available
                    }
                }
            }
            
        } catch (e: Exception) {
            variables.add(VariableNode(
                name = "Variables",
                value = "Enhanced variable extraction failed: ${e.message}",
                type = "Error",
                scope = "LOCAL",
                isExpandable = false,
                isPrimitive = false
            ))
        }
        
        if (variables.isEmpty()) {
            variables.add(VariableNode(
                name = "Variables",
                value = "No variables visible in current scope or frame not accessible",
                type = "Info",
                scope = "LOCAL",
                isExpandable = false,
                isPrimitive = false
            ))
        }
        
        return variables
    }
    
    private fun createDetailedVariableNode(
        name: String,
        value: Value?,
        typeName: String,
        scope: String,
        expandObjects: Boolean,
        maxDepth: Int
    ): VariableNode {
        val valueString = try {
            when (value) {
                null -> "null"
                is StringReference -> "\"${value.value()}\""
                is PrimitiveValue -> value.toString()
                is ObjectReference -> {
                    if (expandObjects && maxDepth > 0) {
                        expandObjectValue(value)
                    } else {
                        "${value.referenceType().name().substringAfterLast('.')}@${value.uniqueID()}"
                    }
                }
                else -> value.toString()
            }
        } catch (e: Exception) {
            "<error: ${e.message}>"
        }
        
        val isPrimitive = when (value) {
            is PrimitiveValue -> true
            is StringReference -> true
            null -> true
            else -> false
        }
        
        val isExpandable = value is ObjectReference && !isPrimitive
        
        val children = if (expandObjects && value is ObjectReference && maxDepth > 0) {
            try {
                extractObjectFields(value, maxDepth - 1)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        return VariableNode(
            name = name,
            value = valueString,
            type = typeName.substringAfterLast('.'),
            scope = scope,
            isExpandable = isExpandable,
            isPrimitive = isPrimitive,
            children = children
        )
    }
    
    private fun extractObjectFields(objectRef: ObjectReference, remainingDepth: Int): List<VariableNode> {
        if (remainingDepth <= 0) return emptyList()
        
        val fields = mutableListOf<VariableNode>()
        
        try {
            val type = objectRef.referenceType()
            val allFields = type.allFields().take(10) // Limit to prevent overwhelming output
            
            for (field in allFields) {
                try {
                    val fieldValue = objectRef.getValue(field)
                    val fieldNode = createDetailedVariableNode(
                        field.name(),
                        fieldValue,
                        field.typeName(),
                        if (field.isStatic()) "STATIC" else "FIELD",
                        remainingDepth > 1,
                        remainingDepth - 1
                    )
                    fields.add(fieldNode)
                } catch (e: Exception) {
                    fields.add(VariableNode(
                        name = field.name(),
                        value = "<inaccessible>",
                        type = field.typeName(),
                        scope = "FIELD",
                        isExpandable = false,
                        isPrimitive = false
                    ))
                }
            }
        } catch (e: Exception) {
            // Failed to get fields
        }
        
        return fields
    }
    
    private fun expandObjectValue(objectRef: ObjectReference): String {
        return try {
            val type = objectRef.referenceType()
            val fields = type.allFields().take(3) // Limit for string representation
            val fieldValues = fields.map { field ->
                try {
                    val fieldValue = objectRef.getValue(field)
                    "${field.name()}=${formatValueForDisplay(fieldValue)}"
                } catch (e: Exception) {
                    "${field.name()}=<e>"
                }
            }
            "${type.name().substringAfterLast('.')}{${fieldValues.joinToString(", ")}}"
        } catch (e: Exception) {
            objectRef.toString()
        }
    }
    
    private fun formatValueForDisplay(value: Value?): String {
        return try {
            when (value) {
                null -> "null"
                is StringReference -> "\"${value.value().take(20)}${if (value.value().length > 20) "..." else ""}\""
                is PrimitiveValue -> value.toString()
                is ObjectReference -> "${value.referenceType().name().substringAfterLast('.')}@${value.uniqueID()}"
                else -> value.toString()
            }
        } catch (e: Exception) {
            "<error>"
        }
    }
    
    /**
     * Get a StackFrameProxyImpl by index using direct JDI access
     */
    private fun getStackFrameProxyByIndex(session: XDebugSession, frameIndex: Int): StackFrameProxyImpl? {
        return try {
            val executionStack = session.suspendContext?.activeExecutionStack
            val topFrame = executionStack?.topFrame
            
            if (topFrame is JavaStackFrame) {
                val stackFrameProxy = topFrame.stackFrameProxy
                val threadProxy = stackFrameProxy.threadProxy()
                
                // Get all frames from the thread
                val allFrames = threadProxy.frames()
                
                if (frameIndex < allFrames.size) {
                    LOG.info("Accessing frame $frameIndex out of ${allFrames.size} available frames")
                    allFrames[frameIndex]
                } else {
                    LOG.warn("Frame index $frameIndex is out of bounds. Available frames: ${allFrames.size}")
                    null
                }
            } else {
                LOG.warn("Top frame is not a JavaStackFrame, cannot access frames by index")
                null
            }
        } catch (e: Exception) {
            LOG.error("Error accessing frame by index $frameIndex: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get the total number of available frames
     */
    private fun getAvailableFrameCount(session: XDebugSession): Int {
        return try {
            val executionStack = session.suspendContext?.activeExecutionStack
            val topFrame = executionStack?.topFrame
            
            if (topFrame is JavaStackFrame) {
                val stackFrameProxy = topFrame.stackFrameProxy
                val threadProxy = stackFrameProxy.threadProxy()
                val allFrames = threadProxy.frames()
                allFrames.size
            } else {
                1 // Only top frame available
            }
        } catch (e: Exception) {
            LOG.error("Error getting frame count: ${e.message}", e)
            0
        }
    }
}
