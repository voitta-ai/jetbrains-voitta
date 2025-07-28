package ai.voitta.jetbrains.debug

import ai.voitta.jetbrains.ast.DebugSessionInfo
import ai.voitta.jetbrains.ast.StackFrameNode
import ai.voitta.jetbrains.ast.VariableNode
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import kotlinx.serialization.Serializable
import org.jetbrains.ide.mcp.Response
import org.jetbrains.mcpserverplugin.AbstractMcpTool
import ai.voitta.jetbrains.utils.JsonUtils
import java.time.Instant
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueContainer
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.ui.impl.watch.LocalVariableDescriptorImpl
import com.intellij.debugger.ui.impl.watch.FieldDescriptorImpl
import com.intellij.debugger.ui.impl.watch.ThisDescriptorImpl
import com.intellij.debugger.ui.impl.watch.ArgumentValueDescriptorImpl
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.XSourcePosition
import com.intellij.util.concurrency.annotations.RequiresReadLock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.swing.Icon
import com.sun.jdi.*
import com.intellij.debugger.engine.StackFrameContext

/**
 * Enhanced debug session analysis tools for runtime inspection with rich variable and stack trace support
 */

@Serializable
class NoArgs

class GetCurrentStackTraceTool : AbstractMcpTool<NoArgs>(NoArgs.serializer()) {
    override val name = "get_current_stack_trace"
    override val description = """
        Retrieves the current stack trace when execution is suspended at a breakpoint.
        Shows method call chain with file names and line numbers.
        
        Returns: Array of StackFrameNode with complete call stack
        Error: "No active debug session" if not debugging or not suspended
    """
    
    override fun handle(project: Project, args: NoArgs): Response {
        return try {
            val session = getCurrentDebugSession(project)
                ?: return Response(error = "No active debug session or execution not suspended")
            
            val frames = getStackFrames(session)
            val framesJson = frames.joinToString(",\n", prefix = "[", postfix = "]") { frame ->
                """
                {
                    "methodName": "${JsonUtils.escapeJson(frame.methodName)}",
                    "className": "${JsonUtils.escapeJson(frame.className)}",
                    "fileName": "${JsonUtils.escapeJson(frame.fileName ?: "")}",
                    "lineNumber": ${frame.lineNumber},
                    "isInUserCode": ${frame.isInUserCode},
                    "frameIndex": ${frame.frameIndex}
                }
                """.trimIndent()
            }
            Response(framesJson)
            
        } catch (e: Exception) {
            Response(error = "Error retrieving stack trace: ${e.message}")
        }
    }
    
    private fun getCurrentDebugSession(project: Project): XDebugSession? {
        val debuggerManager = XDebuggerManager.getInstance(project)
        return debuggerManager.currentSession?.takeIf { it.isSuspended }
    }
    
    private fun getStackFrames(session: XDebugSession): List<StackFrameNode> {
        val executionStack = session.suspendContext?.activeExecutionStack
        if (executionStack == null) return emptyList()
        
        val frames = mutableListOf<StackFrameNode>()
        
        try {
            // Get the top frame first
            val topFrame = executionStack.topFrame
            if (topFrame != null) {
                val topFrameNode = createStackFrameNode(topFrame, 0)
                frames.add(topFrameNode)
                
                // Try to get additional frames using IntelliJ's debugging APIs
                try {
                    if (topFrame is JavaStackFrame) {
                        val stackFrameProxy = topFrame.stackFrameProxy
                        val threadProxy = stackFrameProxy.threadProxy()
                        
                        // Get all frames from the thread
                        val allFrames = threadProxy.frames()
                        for (i in 1 until minOf(allFrames.size, 20)) { // Limit to 20 frames max
                            try {
                                val frameProxy = allFrames[i]
                                val frameNode = createStackFrameNodeFromProxy(frameProxy as com.sun.jdi.StackFrame, i)
                                frames.add(frameNode)
                            } catch (e: Exception) {
                                // Skip this frame and continue
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Additional frames not available, just use top frame
                }
            }
        } catch (e: Exception) {
            // Even top frame access failed, return empty list
        }
        
        return frames
    }
    
    private fun createStackFrameNode(frame: XStackFrame, index: Int): StackFrameNode {
        return StackFrameNode(
            methodName = extractMethodName(frame),
            className = extractClassName(frame),
            fileName = frame.sourcePosition?.file?.name,
            lineNumber = (frame.sourcePosition?.line ?: -1) + 1,
            isInUserCode = isUserCode(frame),
            frameIndex = index
        )
    }
    
    private fun createStackFrameNodeFromProxy(frameProxy: com.sun.jdi.StackFrame, index: Int): StackFrameNode {
        val location = frameProxy.location()
        val method = location.method()
        val declaringType = method.declaringType()
        
        return StackFrameNode(
            methodName = method.name(),
            className = declaringType.name().substringAfterLast('.'),
            fileName = try { location.sourceName() } catch (e: Exception) { null },
            lineNumber = try { location.lineNumber() } catch (e: Exception) { -1 },
            isInUserCode = isUserCodeFromType(declaringType.name()),
            frameIndex = index
        )
    }
    
    private fun extractMethodName(frame: XStackFrame): String {
        return try {
            if (frame is JavaStackFrame) {
                val method = frame.stackFrameProxy.location().method()
                method.name()
            } else {
                val frameString = frame.toString()
                val methodRegex = Regex("\\.(\\w+)\\(")
                methodRegex.find(frameString)?.groupValues?.get(1) ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun extractClassName(frame: XStackFrame): String {
        return try {
            if (frame is JavaStackFrame) {
                val declaringType = frame.stackFrameProxy.location().method().declaringType()
                declaringType.name().substringAfterLast('.')
            } else {
                val frameString = frame.toString()
                val classRegex = Regex("([\\w.]+)\\.\\w+\\(")
                val fullClassName = classRegex.find(frameString)?.groupValues?.get(1) ?: "Unknown"
                fullClassName.substringAfterLast('.')
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun isUserCode(frame: XStackFrame): Boolean {
        return try {
            if (frame is JavaStackFrame) {
                val declaringType = frame.stackFrameProxy.location().method().declaringType()
                isUserCodeFromType(declaringType.name())
            } else {
                val frameString = frame.toString()
                !frameString.contains("java.") && 
                !frameString.contains("javax.") && 
                !frameString.contains("sun.") &&
                !frameString.contains("com.sun.")
            }
        } catch (e: Exception) {
            true
        }
    }
    
    private fun isUserCodeFromType(typeName: String): Boolean {
        return !typeName.startsWith("java.") && 
               !typeName.startsWith("javax.") && 
               !typeName.startsWith("sun.") &&
               !typeName.startsWith("com.sun.")
    }
}

class GetDebugSessionInfoTool : AbstractMcpTool<NoArgs>(NoArgs.serializer()) {
    override val name = "get_debug_session_info"
    override val description = """
        Retrieves information about the current debug session state.
        
        Returns: DebugSessionInfo with session status and context
    """
    
    override fun handle(project: Project, args: NoArgs): Response {
        return try {
            val sessionInfo = getDebugSessionInfo(project)
            val sessionJson = """
            {
                "isActive": ${sessionInfo.isActive},
                "isSuspended": ${sessionInfo.isSuspended},
                "suspendedThreadName": "${JsonUtils.escapeJson(sessionInfo.suspendedThreadName ?: "")}",
                "currentBreakpoint": "${JsonUtils.escapeJson(sessionInfo.currentBreakpoint ?: "")}",
                "totalThreads": ${sessionInfo.totalThreads},
                "suspendReason": "${JsonUtils.escapeJson(sessionInfo.suspendReason ?: "")}"
            }
            """.trimIndent()
            Response(sessionJson)
            
        } catch (e: Exception) {
            Response(error = "Error getting debug session info: ${e.message}")
        }
    }
    
    private fun getDebugSessionInfo(project: Project): DebugSessionInfo {
        val debuggerManager = XDebuggerManager.getInstance(project)
        val session = debuggerManager.currentSession
        
        return if (session != null) {
            DebugSessionInfo(
                isActive = !session.isStopped,
                isSuspended = session.isSuspended,
                suspendedThreadName = getCurrentThreadName(session),
                currentBreakpoint = getCurrentBreakpointInfo(session),
                totalThreads = getTotalThreadCount(session),
                suspendReason = determineSuspendReason(session)
            )
        } else {
            DebugSessionInfo(
                isActive = false,
                isSuspended = false,
                suspendedThreadName = null,
                currentBreakpoint = null,
                totalThreads = 0,
                suspendReason = null
            )
        }
    }
    
    private fun getCurrentThreadName(session: XDebugSession): String? {
        return try {
            val executionStack = session.suspendContext?.activeExecutionStack
            executionStack?.displayName
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getCurrentBreakpointInfo(session: XDebugSession): String? {
        return try {
            val topFrame = session.suspendContext?.activeExecutionStack?.topFrame
            val sourcePosition = topFrame?.sourcePosition
            if (sourcePosition != null) {
                "${sourcePosition.file.name}:${sourcePosition.line + 1}"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getTotalThreadCount(session: XDebugSession): Int {
        return try {
            val suspendContext = session.suspendContext
            if (suspendContext != null) {
                suspendContext.executionStacks.size
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun determineSuspendReason(session: XDebugSession): String? {
        return try {
            when {
                session.isSuspended -> {
                    val topFrame = session.suspendContext?.activeExecutionStack?.topFrame
                    when {
                        topFrame?.sourcePosition != null -> "BREAKPOINT"
                        else -> "STEP"
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
}

@Serializable
data class DebugSnapshotArgs(
    val includeVariables: Boolean = true,
    val includeStackTrace: Boolean = true,
    val expandObjects: Boolean = false,
    val maxStackFrames: Int = 10
)

@Serializable
data class DebugSnapshot(
    val sessionInfo: DebugSessionInfo,
    val stackTrace: List<StackFrameNode>? = null,
    val currentFrameVariables: List<VariableNode>? = null,
    val timestamp: String = Instant.now().toString()
)

class GetDebugSnapshotTool : AbstractMcpTool<DebugSnapshotArgs>(DebugSnapshotArgs.serializer()) {
    override val name = "get_debug_snapshot"
    override val description = """
        Gets a complete snapshot of the current debug state including stack trace and variables.
        Convenient tool for breakpoint analysis that combines multiple debug tools.
        
        Parameters:
        - includeVariables: Include current frame variables (default: true)
        - includeStackTrace: Include full stack trace (default: true)
        - expandObjects: Expand object fields in variables (default: false)
        - maxStackFrames: Maximum stack frames to include (default: 10)
        
        Returns: DebugSnapshot with complete debug state
        Error: "No active debug session" if not debugging or not suspended
    """
    
    override fun handle(project: Project, args: DebugSnapshotArgs): Response {
        return try {
            val session = getCurrentDebugSession(project)
                ?: return Response(error = "No active debug session or execution not suspended")
            
            val sessionInfo = getDebugSessionInfo(project)
            
            val stackTrace = if (args.includeStackTrace) {
                getStackFrames(session).take(args.maxStackFrames)
            } else null
            
            val variables = if (args.includeVariables) {
                getVariablesForFrame(session, 0, args.expandObjects)
            } else null
            
            val snapshot = DebugSnapshot(
                sessionInfo = sessionInfo,
                stackTrace = stackTrace,
                currentFrameVariables = variables
            )
            
            val snapshotJson = """
            {
                "sessionInfo": {
                    "isActive": ${snapshot.sessionInfo.isActive},
                    "isSuspended": ${snapshot.sessionInfo.isSuspended},
                    "suspendedThreadName": "${JsonUtils.escapeJson(snapshot.sessionInfo.suspendedThreadName ?: "")}",
                    "currentBreakpoint": "${JsonUtils.escapeJson(snapshot.sessionInfo.currentBreakpoint ?: "")}",
                    "totalThreads": ${snapshot.sessionInfo.totalThreads},
                    "suspendReason": "${JsonUtils.escapeJson(snapshot.sessionInfo.suspendReason ?: "")}"
                },
                "stackTrace": [${snapshot.stackTrace?.joinToString(",") { frame ->
                    """
                    {
                        "methodName": "${JsonUtils.escapeJson(frame.methodName)}",
                        "className": "${JsonUtils.escapeJson(frame.className)}",
                        "fileName": "${JsonUtils.escapeJson(frame.fileName ?: "")}",
                        "lineNumber": ${frame.lineNumber},
                        "isInUserCode": ${frame.isInUserCode},
                        "frameIndex": ${frame.frameIndex}
                    }
                    """.trimIndent()
                } ?: ""}],
                "currentFrameVariables": [${snapshot.currentFrameVariables?.joinToString(",") { variable ->
                    """
                    {
                        "name": "${JsonUtils.escapeJson(variable.name)}",
                        "value": "${JsonUtils.escapeJson(variable.value ?: "")}",
                        "type": "${JsonUtils.escapeJson(variable.type)}",
                        "scope": "${variable.scope}",
                        "isExpandable": ${variable.isExpandable},
                        "isPrimitive": ${variable.isPrimitive}
                    }
                    """.trimIndent()
                } ?: ""}],
                "timestamp": "${snapshot.timestamp}"
            }
            """.trimIndent()
            Response(snapshotJson)
            
        } catch (e: Exception) {
            Response(error = "Error creating debug snapshot: ${e.message}")
        }
    }
    
    private fun getCurrentDebugSession(project: Project): XDebugSession? {
        val debuggerManager = XDebuggerManager.getInstance(project)
        return debuggerManager.currentSession?.takeIf { it.isSuspended }
    }
    
    private fun getDebugSessionInfo(project: Project): DebugSessionInfo {
        val debuggerManager = XDebuggerManager.getInstance(project)
        val session = debuggerManager.currentSession
        
        return if (session != null) {
            DebugSessionInfo(
                isActive = !session.isStopped,
                isSuspended = session.isSuspended,
                suspendedThreadName = getCurrentThreadName(session),
                currentBreakpoint = getCurrentBreakpointInfo(session),
                totalThreads = getTotalThreadCount(session),
                suspendReason = determineSuspendReason(session),
                sessionStartTime = null
            )
        } else {
            DebugSessionInfo(
                isActive = false,
                isSuspended = false,
                suspendedThreadName = null,
                currentBreakpoint = null,
                totalThreads = 0,
                suspendReason = null,
                sessionStartTime = null
            )
        }
    }
    
    private fun getStackFrames(session: XDebugSession): List<StackFrameNode> {
        val executionStack = session.suspendContext?.activeExecutionStack
        if (executionStack == null) return emptyList()
        
        val frames = mutableListOf<StackFrameNode>()
        
        try {
            // Get the top frame first
            val topFrame = executionStack.topFrame
            if (topFrame != null) {
                val topFrameNode = createStackFrameNode(topFrame, 0)
                frames.add(topFrameNode)
                
                // Try to get additional frames using enhanced IntelliJ debugging APIs
                try {
                    if (topFrame is JavaStackFrame) {
                        val stackFrameProxy = topFrame.stackFrameProxy
                        val threadProxy = stackFrameProxy.threadProxy()
                        
                        // Get all frames from the thread
                        val allFrames = threadProxy.frames()
                        for (i in 1 until minOf(allFrames.size, 20)) { // Limit to 20 frames max
                            try {
                                val frameProxy = allFrames[i]
                                val frameNode = createStackFrameNodeFromProxy(frameProxy as com.sun.jdi.StackFrame, i)
                                frames.add(frameNode)
                            } catch (e: Exception) {
                                // Skip this frame and continue
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Additional frames not available, just use top frame
                }
            }
        } catch (e: Exception) {
            // Even top frame access failed, return empty list
        }
        
        return frames
    }
    
    private fun getVariablesForFrame(session: XDebugSession, frameIndex: Int, expandObjects: Boolean): List<VariableNode> {
        try {
            val executionStack = session.suspendContext?.activeExecutionStack ?: return emptyList()
            
            // For now, we'll only work with the top frame (frameIndex 0)
            if (frameIndex != 0) {
                return listOf(VariableNode(
                    name = "Variables",
                    value = "Only top frame variables are currently supported",
                    type = "Info",
                    scope = "LOCAL",
                    isExpandable = false,
                    isPrimitive = false
                ))
            }
            
            val frame = executionStack.topFrame ?: return emptyList()
            
            // Enhanced variable extraction for Java debugging
            if (frame is JavaStackFrame) {
                return extractJavaVariables(frame, expandObjects)
            }
            
            // Fallback for other frame types
            return extractGenericVariables(frame, expandObjects)
            
        } catch (e: Exception) {
            // Return a helpful message instead of empty list
            return listOf(VariableNode(
                name = "Variables",
                value = "Error extracting variables: ${e.message}. Use evaluate expression functionality for detailed inspection.",
                type = "Error",
                scope = "LOCAL",
                isExpandable = false,
                isPrimitive = false
            ))
        }
    }
    
    private fun extractJavaVariables(frame: JavaStackFrame, expandObjects: Boolean): List<VariableNode> {
        val variables = mutableListOf<VariableNode>()
        
        try {
            val stackFrameProxy = frame.stackFrameProxy
            
            // Get local variables
            try {
                val localVariables = stackFrameProxy.visibleVariables()
                for (localVar in localVariables) {
                    try {
                        val value = stackFrameProxy.getValue(localVar)
                        val variableNode = createVariableNode(
                            localVar.name(),
                            value,
                            localVar.typeName(),
                            "LOCAL",
                            expandObjects
                        )
                        variables.add(variableNode)
                    } catch (e: Exception) {
                        // Skip this variable if we can't access it
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
            
            // Get method parameters
            try {
                val method = stackFrameProxy.location().method()
                val argumentTypes = method.argumentTypes()
                val argumentValues = stackFrameProxy.getArgumentValues()
                
                for (i in argumentValues.indices) {
                    try {
                        val value = argumentValues[i]
                        val typeName = if (i < argumentTypes.size) {
                            argumentTypes[i].name()
                        } else {
                            "Unknown"
                        }
                        
                        val variableNode = createVariableNode(
                            "arg$i",
                            value,
                            typeName,
                            "PARAMETER",
                            expandObjects
                        )
                        variables.add(variableNode)
                    } catch (e: Exception) {
                        // Skip this parameter
                        continue
                    }
                }
            } catch (e: Exception) {
                // Parameters not accessible
            }
            
            // Get 'this' object if available (non-static method)
            try {
                val thisObject = stackFrameProxy.thisObject()
                if (thisObject != null) {
                    val variableNode = createVariableNode(
                        "this",
                        thisObject,
                        thisObject.referenceType().name(),
                        "FIELD",
                        expandObjects
                    )
                    variables.add(variableNode)
                }
            } catch (e: Exception) {
                // 'this' not available (static method or error)
            }
            
        } catch (e: Exception) {
            // Fallback to a helpful error message
            variables.add(VariableNode(
                name = "Variables",
                value = "Enhanced variable extraction failed: ${e.message}",
                type = "Error",
                scope = "LOCAL",
                isExpandable = false,
                isPrimitive = false
            ))
        }
        
        // If no variables were extracted, provide helpful guidance
        if (variables.isEmpty()) {
            variables.add(VariableNode(
                name = "Variables",
                value = "No variables visible in current scope. Use 'Evaluate Expression' (Ctrl+Alt+F8) for custom inspection.",
                type = "Info",
                scope = "LOCAL",
                isExpandable = false,
                isPrimitive = false
            ))
        }
        
        return variables
    }
    
    private fun extractGenericVariables(frame: XStackFrame, expandObjects: Boolean): List<VariableNode> {
        // Generic variable extraction using XValueContainer
        val variables = mutableListOf<VariableNode>()
        
        try {
            // This is a simplified implementation
            // XStackFrame doesn't directly expose variables, so we provide guidance
            variables.add(VariableNode(
                name = "Variables",
                value = "Generic frame type detected. Use 'Evaluate Expression' during debugging to inspect specific variables (Ctrl+Alt+F8)",
                type = "Info",
                scope = "LOCAL",
                isExpandable = false,
                isPrimitive = false
            ))
        } catch (e: Exception) {
            variables.add(VariableNode(
                name = "Variables",
                value = "Variable extraction not available for this frame type: ${e.message}",
                type = "Error",
                scope = "LOCAL",
                isExpandable = false,
                isPrimitive = false
            ))
        }
        
        return variables
    }
    
    private fun createVariableNode(
        name: String,
        value: Value?,
        typeName: String,
        scope: String,
        expandObjects: Boolean
    ): VariableNode {
        val valueString = try {
            when (value) {
                null -> "null"
                is StringReference -> "\"${value.value()}\""
                is PrimitiveValue -> value.toString()
                is ObjectReference -> {
                    if (expandObjects) {
                        expandObjectValue(value)
                    } else {
                        "${value.referenceType().name()}@${value.uniqueID()}"
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
        
        return VariableNode(
            name = name,
            value = valueString,
            type = typeName.substringAfterLast('.'),
            scope = scope,
            isExpandable = isExpandable,
            isPrimitive = isPrimitive
        )
    }
    
    private fun expandObjectValue(objectRef: ObjectReference): String {
        return try {
            val type = objectRef.referenceType()
            val fields = type.allFields().take(5) // Limit to first 5 fields
            val fieldValues = fields.map { field ->
                try {
                    val fieldValue = objectRef.getValue(field)
                    "${field.name()}=${fieldValue?.toString() ?: "null"}"
                } catch (e: Exception) {
                    "${field.name()}=<error>"
                }
            }
            "${type.name()}{${fieldValues.joinToString(", ")}}"
        } catch (e: Exception) {
            objectRef.toString()
        }
    }
    
    private fun createStackFrameNode(frame: XStackFrame, index: Int): StackFrameNode {
        return StackFrameNode(
            methodName = extractMethodName(frame),
            className = extractClassName(frame),
            fileName = frame.sourcePosition?.file?.name,
            lineNumber = (frame.sourcePosition?.line ?: -1) + 1,
            isInUserCode = isUserCode(frame),
            frameIndex = index
        )
    }
    
    private fun createStackFrameNodeFromProxy(frameProxy: com.sun.jdi.StackFrame, index: Int): StackFrameNode {
        val location = frameProxy.location()
        val method = location.method()
        val declaringType = method.declaringType()
        
        return StackFrameNode(
            methodName = method.name(),
            className = declaringType.name().substringAfterLast('.'),
            fileName = try { location.sourceName() } catch (e: Exception) { null },
            lineNumber = try { location.lineNumber() } catch (e: Exception) { -1 },
            isInUserCode = isUserCodeFromType(declaringType.name()),
            frameIndex = index
        )
    }
    
    private fun extractMethodName(frame: XStackFrame): String {
        return try {
            if (frame is JavaStackFrame) {
                val method = frame.stackFrameProxy.location().method()
                method.name()
            } else {
                val frameString = frame.toString()
                val methodRegex = Regex("\\.(\\w+)\\(")
                methodRegex.find(frameString)?.groupValues?.get(1) ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun extractClassName(frame: XStackFrame): String {
        return try {
            if (frame is JavaStackFrame) {
                val declaringType = frame.stackFrameProxy.location().method().declaringType()
                declaringType.name().substringAfterLast('.')
            } else {
                val frameString = frame.toString()
                val classRegex = Regex("([\\w.]+)\\.\\w+\\(")
                val fullClassName = classRegex.find(frameString)?.groupValues?.get(1) ?: "Unknown"
                fullClassName.substringAfterLast('.')
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun isUserCode(frame: XStackFrame): Boolean {
        return try {
            if (frame is JavaStackFrame) {
                val declaringType = frame.stackFrameProxy.location().method().declaringType()
                isUserCodeFromType(declaringType.name())
            } else {
                val frameString = frame.toString()
                !frameString.contains("java.") && 
                !frameString.contains("javax.") && 
                !frameString.contains("sun.") &&
                !frameString.contains("com.sun.")
            }
        } catch (e: Exception) {
            true
        }
    }
    
    private fun isUserCodeFromType(typeName: String): Boolean {
        return !typeName.startsWith("java.") && 
               !typeName.startsWith("javax.") && 
               !typeName.startsWith("sun.") &&
               !typeName.startsWith("com.sun.")
    }
    
    private fun getCurrentThreadName(session: XDebugSession): String? {
        return try {
            val executionStack = session.suspendContext?.activeExecutionStack
            executionStack?.displayName
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getCurrentBreakpointInfo(session: XDebugSession): String? {
        return try {
            val topFrame = session.suspendContext?.activeExecutionStack?.topFrame
            val sourcePosition = topFrame?.sourcePosition
            if (sourcePosition != null) {
                "${sourcePosition.file.name}:${sourcePosition.line + 1}"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getTotalThreadCount(session: XDebugSession): Int {
        return try {
            val suspendContext = session.suspendContext
            if (suspendContext != null) {
                suspendContext.executionStacks.size
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun determineSuspendReason(session: XDebugSession): String? {
        return try {
            when {
                session.isSuspended -> {
                    val topFrame = session.suspendContext?.activeExecutionStack?.topFrame
                    when {
                        topFrame?.sourcePosition != null -> "BREAKPOINT"
                        else -> "STEP"
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
}
