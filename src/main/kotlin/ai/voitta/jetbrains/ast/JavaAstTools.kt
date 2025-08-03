package ai.voitta.jetbrains.ast

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.serialization.Serializable
import org.jetbrains.ide.mcp.Response
import org.jetbrains.mcpserverplugin.AbstractMcpTool
import ai.voitta.jetbrains.utils.JsonUtils
import com.intellij.psi.search.GlobalSearchScope
import java.nio.file.Paths

/**
 * Java-specific AST analysis tools
 */

@Serializable
data class JavaFileAstArgs(
    val filePath: String, 
    val includeMethodBodies: Boolean = false,
    val includePrivateMembers: Boolean = true
)

class GetJavaFileAstTool : AbstractMcpTool<JavaFileAstArgs>(JavaFileAstArgs.serializer()) {
    override val name = "get_java_file_ast"
    override val description = """
        Retrieves the AST (Abstract Syntax Tree) for a Java file.
        Returns complete class structure including methods, fields, and metadata.
        
        Parameters:
        - filePath: Path to the Java file (relative to project root)
        - includeMethodBodies: Whether to include detailed method body statements (default: false)
        - includePrivateMembers: Whether to include private methods and fields (default: true)
        
        Returns: List of ClassNode with complete file structure
        Error: File not found, not a Java file, or parsing errors
    """
    
    override fun handle(project: Project, args: JavaFileAstArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            if (psiFile !is PsiJavaFile) {
                return Response(error = "Not a Java file: ${args.filePath}")
            }
            
            val classes = analyzeJavaFile(psiFile, args)
            val classesJson = classes.joinToString(",\n", prefix = "[", postfix = "]") { classNode ->
                val methodsJson = if (args.includeMethodBodies) {
                    classNode.methods.joinToString(",\n", prefix = "[", postfix = "]") { method ->
                        """
                        {
                            "name": "${JsonUtils.escapeJson(method.name ?: "")}",
                            "lineNumber": ${method.lineNumber ?: -1},
                            "firstExecutableLineNumber": ${method.firstExecutableLineNumber ?: -1},
                            "lastLineNumber": ${method.lastLineNumber ?: -1},
                            "returnType": "${JsonUtils.escapeJson(method.returnType ?: "")}",
                            "complexity": ${method.complexity ?: 1},
                            "parameterCount": ${method.parameters.size},
                            "isConstructor": ${method.isConstructor},
                            "modifiers": [${method.modifiers.joinToString(",") { "\"$it\"" }}]
                        }
                        """.trimIndent()
                    }
                } else "[]"
                
                """
                {
                    "name": "${JsonUtils.escapeJson(classNode.name ?: "")}",
                    "fullyQualifiedName": "${JsonUtils.escapeJson(classNode.fullyQualifiedName ?: "")}",
                    "superClass": "${classNode.superClass ?: ""}",
                    "interfaces": [${classNode.interfaces.joinToString(",") { "\"$it\"" }}],
                    "modifiers": [${classNode.modifiers.joinToString(",") { "\"$it\"" }}],
                    "isInterface": ${classNode.isInterface},
                    "isEnum": ${classNode.isEnum},
                    "isAbstract": ${classNode.isAbstract},
                    "packageName": "${classNode.packageName ?: ""}",
                    "methodCount": ${classNode.methods.size},
                    "fieldCount": ${classNode.fields.size},
                    "methods": $methodsJson
                }
                """.trimIndent()
            }
            Response(classesJson)
            
        } catch (e: Exception) {
            Response(error = "Error analyzing Java file: ${e.message}")
        }
    }
    
    private fun analyzeJavaFile(javaFile: PsiJavaFile, args: JavaFileAstArgs): List<ClassNode> {
        return javaFile.classes.map { psiClass ->
            analyzeClass(psiClass, args)
        }
    }
    
    private fun analyzeClass(psiClass: PsiClass, args: JavaFileAstArgs): ClassNode {
        val methods = psiClass.methods.filter { method ->
            args.includePrivateMembers || !method.hasModifierProperty(PsiModifier.PRIVATE)
        }.map { method ->
            analyzeMethod(method, args.includeMethodBodies)
        }
        
        val fields = psiClass.fields.filter { field ->
            args.includePrivateMembers || !field.hasModifierProperty(PsiModifier.PRIVATE)
        }.map { field ->
            analyzeField(field)
        }
        
        val innerClasses = psiClass.innerClasses.map { innerClass ->
            analyzeClass(innerClass, args)
        }
        
        return ClassNode(
            name = psiClass.name,
            fullyQualifiedName = AstUtils.getFullyQualifiedName(psiClass),
            superClass = psiClass.superClass?.qualifiedName,
            interfaces = psiClass.interfaces.mapNotNull { it.qualifiedName },
            modifiers = AstUtils.extractModifiers(psiClass.modifierList),
            methods = methods,
            fields = fields,
            innerClasses = innerClasses,
            annotations = AstUtils.extractAnnotations(psiClass.modifierList),
            javadoc = AstUtils.extractJavadoc(psiClass.docComment),
            packageName = (psiClass.containingFile as? PsiJavaFile)?.packageName,
            isInterface = psiClass.isInterface,
            isEnum = psiClass.isEnum,
            isAbstract = psiClass.hasModifierProperty(PsiModifier.ABSTRACT)
        )
    }
    
    private fun analyzeMethod(method: PsiMethod, includeBody: Boolean): MethodNode {
        val bodyStatements = if (includeBody && method.body != null) {
            analyzeMethodBody(method.body!!)
        } else null
        
        val lineRange = AstUtils.getMethodLineRange(method)
        
        return MethodNode(
            name = method.name,
            returnType = AstUtils.typeToString(method.returnType),
            parameters = AstUtils.getMethodParameters(method),
            modifiers = AstUtils.extractModifiers(method.modifierList),
            annotations = AstUtils.extractAnnotations(method.modifierList),
            javadoc = AstUtils.extractJavadoc(method.docComment),
            bodyStatements = bodyStatements,
            complexity = AstUtils.calculateComplexity(method),
            isConstructor = AstUtils.isConstructor(method),
            isStatic = method.hasModifierProperty(PsiModifier.STATIC),
            isAbstract = method.hasModifierProperty(PsiModifier.ABSTRACT),
            throwsExceptions = AstUtils.getThrownExceptions(method),
            lineNumber = AstUtils.getLineNumber(method),
            firstExecutableLineNumber = lineRange.firstExecutableLineNumber,
            lastLineNumber = lineRange.lastLineNumber,
            bodyLineRange = lineRange
        )
    }
    
    private fun analyzeField(field: PsiField): FieldNode {
        return FieldNode(
            name = field.name,
            type = AstUtils.typeToString(field.type),
            modifiers = AstUtils.extractModifiers(field.modifierList),
            annotations = AstUtils.extractAnnotations(field.modifierList),
            initializer = field.initializer?.text,
            isStatic = field.hasModifierProperty(PsiModifier.STATIC),
            isFinal = field.hasModifierProperty(PsiModifier.FINAL),
            isVolatile = field.hasModifierProperty(PsiModifier.VOLATILE),
            lineNumber = AstUtils.getLineNumber(field)
        )
    }
    
    private fun analyzeMethodBody(body: PsiCodeBlock): List<StatementNode> {
        return body.statements.map { statement ->
            analyzeStatement(statement)
        }
    }
    
    private fun analyzeStatement(statement: PsiStatement): StatementNode {
        val children = PsiTreeUtil.getChildrenOfType(statement, PsiStatement::class.java)
            ?.map { analyzeStatement(it) } ?: emptyList()
        
        val statementKind = AstUtils.getStatementKind(statement)
        val isExecutable = !AstUtils.isNonExecutableStatement(statement)
        
        return StatementNode(
            type = statement.javaClass.simpleName,
            text = AstUtils.createCodeSnippet(statement, 100),
            startOffset = statement.textRange.startOffset,
            endOffset = statement.textRange.endOffset,
            lineNumber = AstUtils.getLineNumber(statement),
            isExecutable = isExecutable,
            statementKind = statementKind,
            children = children
        )
    }
}

@Serializable
data class ClassHierarchyArgs(
    val className: String, 
    val direction: String = "both" // "up", "down", "both"
)

class GetClassHierarchyTool : AbstractMcpTool<ClassHierarchyArgs>(ClassHierarchyArgs.serializer()) {
    override val name = "java_get_class_hierarchy"
    override val description = """
        Retrieves class hierarchy information for a given class.
        Shows inheritance relationships, implemented interfaces, and subclasses.
        
        Parameters:
        - className: Fully qualified class name or simple name
        - direction: "up" (superclasses), "down" (subclasses), "both" (default: "both")
        
        Returns: HierarchyNode with inheritance relationships
        Error: Class not found or analysis errors
    """
    
    override fun handle(project: Project, args: ClassHierarchyArgs): Response {
        return try {
            val psiClass = findClass(project, args.className)
                ?: return Response(error = "Class not found: ${args.className}")
            
            val hierarchy = analyzeHierarchy(psiClass, args.direction)
            val hierarchyJson = """
            {
                "className": "${JsonUtils.escapeJson(hierarchy.className)}",
                "fullyQualifiedName": "${JsonUtils.escapeJson(hierarchy.fullyQualifiedName)}",
                "type": "${hierarchy.type}",
                "file": "${JsonUtils.escapeJson(hierarchy.file ?: "")}"
            }
            """.trimIndent()
            Response(hierarchyJson)
            
        } catch (e: Exception) {
            Response(error = "Error analyzing class hierarchy: ${e.message}")
        }
    }
    
    private fun findClass(project: Project, className: String): PsiClass? {
        val facade = JavaPsiFacade.getInstance(project)
        
        // Try as fully qualified name first
        facade.findClass(className, GlobalSearchScope.allScope(project))?.let { return it }
        
        // Search in all scopes for simple name
        val scope = GlobalSearchScope.allScope(project)
        return facade.findClasses(className, scope).firstOrNull()
    }
    
    private fun analyzeHierarchy(psiClass: PsiClass, direction: String): HierarchyNode {
        val node = HierarchyNode(
            className = psiClass.name ?: "Unknown",
            fullyQualifiedName = psiClass.qualifiedName ?: "",
            type = when {
                psiClass.isInterface -> "INTERFACE"
                psiClass.isEnum -> "ENUM"
                psiClass.isAnnotationType -> "ANNOTATION"
                else -> "CLASS"
            },
            file = psiClass.containingFile?.virtualFile?.let {
                AstUtils.getRelativePath(psiClass.project, it)
            }
        )
        
        when (direction) {
            "up", "both" -> {
                // Add superclasses and interfaces
                node.copy(
                    superClasses = getSuperClasses(psiClass),
                    interfaces = getInterfaces(psiClass)
                )
            }
            "down", "both" -> {
                // Add subclasses and implementing classes
                node.copy(
                    subClasses = getSubClasses(psiClass),
                    implementingClasses = getImplementingClasses(psiClass)
                )
            }
            else -> node
        }
        
        return node
    }
    
    private fun getSuperClasses(psiClass: PsiClass): List<HierarchyNode> {
        val superClass = psiClass.superClass ?: return emptyList()
        return listOf(createSimpleHierarchyNode(superClass))
    }
    
    private fun getInterfaces(psiClass: PsiClass): List<HierarchyNode> {
        return psiClass.interfaces.map { createSimpleHierarchyNode(it) }
    }
    
    private fun getSubClasses(psiClass: PsiClass): List<HierarchyNode> {
        // Note: Finding all subclasses requires searching the entire project
        // This is a simplified version - in a full implementation, you'd use
        // ClassInheritorsSearch.search(psiClass, scope, true)
        return emptyList() // Placeholder for now
    }
    
    private fun getImplementingClasses(psiClass: PsiClass): List<HierarchyNode> {
        // Similar to subclasses - requires project-wide search
        return emptyList() // Placeholder for now
    }
    
    private fun createSimpleHierarchyNode(psiClass: PsiClass): HierarchyNode {
        return HierarchyNode(
            className = psiClass.name ?: "Unknown",
            fullyQualifiedName = psiClass.qualifiedName ?: "",
            type = when {
                psiClass.isInterface -> "INTERFACE"
                psiClass.isEnum -> "ENUM"
                psiClass.isAnnotationType -> "ANNOTATION"
                else -> "CLASS"
            },
            file = psiClass.containingFile?.virtualFile?.let {
                AstUtils.getRelativePath(psiClass.project, it)
            }
        )
    }
}

@Serializable
data class MethodComplexityArgs(
    val filePath: String, 
    val methodName: String? = null
)

class GetMethodComplexityTool : AbstractMcpTool<MethodComplexityArgs>(MethodComplexityArgs.serializer()) {
    override val name = "java_get_method_complexity"
    override val description = """
        Calculates cyclomatic complexity for methods in a Java file.
        
        Parameters:
        - filePath: Path to the Java file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of ComplexityMetric for methods
        Error: File not found, not a Java file, or analysis errors
    """
    
    override fun handle(project: Project, args: MethodComplexityArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            if (psiFile !is PsiJavaFile) {
                return Response(error = "Not a Java file: ${args.filePath}")
            }
            
            val metrics = analyzeComplexity(psiFile, args.methodName)
            val metricsJson = metrics.joinToString(",\n", prefix = "[", postfix = "]") { metric ->
                """
                {
                    "methodName": "${JsonUtils.escapeJson(metric.methodName)}",
                    "className": "${JsonUtils.escapeJson(metric.className)}",
                    "cyclomaticComplexity": ${metric.cyclomaticComplexity},
                    "linesOfCode": ${metric.linesOfCode},
                    "parameterCount": ${metric.parameterCount},
                    "file": "${JsonUtils.escapeJson(metric.file)}",
                    "lineNumber": ${metric.lineNumber}
                }
                """.trimIndent()
            }
            Response(metricsJson)
            
        } catch (e: Exception) {
            Response(error = "Error analyzing method complexity: ${e.message}")
        }
    }
    
    private fun analyzeComplexity(javaFile: PsiJavaFile, methodName: String?): List<ComplexityMetric> {
        val metrics = mutableListOf<ComplexityMetric>()
        
        javaFile.classes.forEach { psiClass ->
            psiClass.methods.forEach { method ->
                if (methodName == null || method.name == methodName) {
                    val metric = ComplexityMetric(
                        methodName = method.name,
                        className = psiClass.name ?: "Unknown",
                        cyclomaticComplexity = AstUtils.calculateComplexity(method),
                        linesOfCode = AstUtils.countLinesOfCode(method),
                        parameterCount = method.parameterList.parametersCount,
                        file = AstUtils.getRelativePath(method.project, javaFile.virtualFile),
                        lineNumber = AstUtils.getLineNumber(method)
                    )
                    metrics.add(metric)
                }
            }
        }
        
        return metrics
    }
}

@Serializable
data class CodePatternsArgs(
    val filePath: String, 
    val patterns: List<String> = listOf("all")
)

class DetectCodePatternsTool : AbstractMcpTool<CodePatternsArgs>(CodePatternsArgs.serializer()) {
    override val name = "java_detect_code_patterns"
    override val description = """
        Detects common code patterns and potential issues in a Java file.
        
        Parameters:
        - filePath: Path to the Java file (relative to project root)
        - patterns: List of patterns to detect ("null_checks", "exception_handling", "loops", "all")
        
        Returns: List of CodePattern with detected patterns and suggestions
        Error: File not found, not a Java file, or analysis errors
    """
    
    override fun handle(project: Project, args: CodePatternsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            if (psiFile !is PsiJavaFile) {
                return Response(error = "Not a Java file: ${args.filePath}")
            }
            
            val patterns = detectPatterns(psiFile, args.patterns)
            val patternsJson = patterns.joinToString(",\n", prefix = "[", postfix = "]") { pattern ->
                """
                {
                    "patternType": "${pattern.patternType}",
                    "description": "${JsonUtils.escapeJson(pattern.description)}",
                    "file": "${JsonUtils.escapeJson(pattern.file)}",
                    "startLine": ${pattern.startLine},
                    "endLine": ${pattern.endLine},
                    "codeSnippet": "${JsonUtils.escapeJson(pattern.codeSnippet)}",
                    "severity": "${pattern.severity}",
                    "suggestion": "${JsonUtils.escapeJson(pattern.suggestion ?: "")}"
                }
                """.trimIndent()
            }
            Response(patternsJson)
            
        } catch (e: Exception) {
            Response(error = "Error detecting code patterns: ${e.message}")
        }
    }
    
    private fun detectPatterns(javaFile: PsiJavaFile, patternTypes: List<String>): List<CodePattern> {
        val patterns = mutableListOf<CodePattern>()
        val relativePath = AstUtils.getRelativePath(javaFile.project, javaFile.virtualFile)
        
        val shouldDetect = { pattern: String -> 
            patternTypes.contains("all") || patternTypes.contains(pattern)
        }
        
        val visitor = object : JavaRecursiveElementVisitor() {
            
            override fun visitIfStatement(statement: PsiIfStatement) {
                if (shouldDetect("null_checks")) {
                    detectNullChecks(statement, patterns, relativePath)
                }
                super.visitIfStatement(statement)
            }
            
            override fun visitTryStatement(statement: PsiTryStatement) {
                if (shouldDetect("exception_handling")) {
                    detectExceptionHandling(statement, patterns, relativePath)
                }
                super.visitTryStatement(statement)
            }
            
            override fun visitForStatement(statement: PsiForStatement) {
                if (shouldDetect("loops")) {
                    detectLoopPatterns(statement, patterns, relativePath)
                }
                super.visitForStatement(statement)
            }
            
            override fun visitMethod(method: PsiMethod) {
                if (shouldDetect("method_length")) {
                    detectLongMethods(method, patterns, relativePath)
                }
                if (shouldDetect("parameter_count")) {
                    detectTooManyParameters(method, patterns, relativePath)
                }
                super.visitMethod(method)
            }
        }
        
        javaFile.accept(visitor)
        return patterns
    }
    
    private fun detectNullChecks(statement: PsiIfStatement, patterns: MutableList<CodePattern>, file: String) {
        val condition = statement.condition?.text ?: return
        
        if (condition.contains("== null") || condition.contains("!= null")) {
            patterns.add(
                CodePattern(
                    patternType = "NULL_CHECK",
                    description = "Null check detected",
                    file = file,
                    startLine = AstUtils.getLineNumber(statement),
                    endLine = AstUtils.getLineNumber(statement),
                    codeSnippet = AstUtils.createCodeSnippet(statement),
                    severity = "INFO",
                    suggestion = "Consider using Optional or defensive programming techniques"
                )
            )
        }
    }
    
    private fun detectExceptionHandling(statement: PsiTryStatement, patterns: MutableList<CodePattern>, file: String) {
        val emptyCatchBlocks = statement.catchSections.filter { catchSection ->
            val codeBlock = catchSection.catchBlock
            codeBlock?.statements?.isEmpty() ?: true
        }
        
        if (emptyCatchBlocks.isNotEmpty()) {
            patterns.add(
                CodePattern(
                    patternType = "EMPTY_CATCH",
                    description = "Empty catch block detected",
                    file = file,
                    startLine = AstUtils.getLineNumber(statement),
                    endLine = AstUtils.getLineNumber(statement),
                    codeSnippet = AstUtils.createCodeSnippet(statement),
                    severity = "WARNING",
                    suggestion = "Consider logging the exception or handling it appropriately"
                )
            )
        }
    }
    
    private fun detectLoopPatterns(statement: PsiForStatement, patterns: MutableList<CodePattern>, file: String) {
        // Detect potential infinite loops or performance issues
        val body = statement.body
        if (body != null) {
            patterns.add(
                CodePattern(
                    patternType = "FOR_LOOP",
                    description = "For loop detected",
                    file = file,
                    startLine = AstUtils.getLineNumber(statement),
                    endLine = AstUtils.getLineNumber(statement),
                    codeSnippet = AstUtils.createCodeSnippet(statement),
                    severity = "INFO",
                    suggestion = "Consider using enhanced for-loops or streams where appropriate"
                )
            )
        }
    }
    
    private fun detectLongMethods(method: PsiMethod, patterns: MutableList<CodePattern>, file: String) {
        val linesOfCode = AstUtils.countLinesOfCode(method)
        
        if (linesOfCode > 50) {
            patterns.add(
                CodePattern(
                    patternType = "LONG_METHOD",
                    description = "Method is too long ($linesOfCode lines)",
                    file = file,
                    startLine = AstUtils.getLineNumber(method),
                    endLine = AstUtils.getLineNumber(method),
                    codeSnippet = AstUtils.createCodeSnippet(method),
                    severity = "WARNING",
                    suggestion = "Consider breaking this method into smaller, more focused methods"
                )
            )
        }
    }
    
    private fun detectTooManyParameters(method: PsiMethod, patterns: MutableList<CodePattern>, file: String) {
        val parameterCount = method.parameterList.parametersCount
        
        if (parameterCount > 5) {
            patterns.add(
                CodePattern(
                    patternType = "TOO_MANY_PARAMETERS",
                    description = "Method has too many parameters ($parameterCount)",
                    file = file,
                    startLine = AstUtils.getLineNumber(method),
                    endLine = AstUtils.getLineNumber(method),
                    codeSnippet = AstUtils.createCodeSnippet(method),
                    severity = "WARNING",
                    suggestion = "Consider using a parameter object or builder pattern"
                )
            )
        }
    }
}

@Serializable
data class BreakpointSuggestionArgs(
    val filePath: String,
    val methodName: String? = null
)

class SuggestBreakpointLinesTool : AbstractMcpTool<BreakpointSuggestionArgs>(BreakpointSuggestionArgs.serializer()) {
    override val name = "java_suggest_breakpoint_lines"
    override val description = """
        Suggests optimal line numbers for setting breakpoints in methods.
        Returns first executable line, key decision points, and method entry/exit points.
        
        Parameters:
        - filePath: Path to the Java file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of BreakpointSuggestion with recommended breakpoint locations
        Error: File not found, not a Java file, or analysis errors
    """
    
    override fun handle(project: Project, args: BreakpointSuggestionArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            if (psiFile !is PsiJavaFile) {
                return Response(error = "Not a Java file: ${args.filePath}")
            }
            
            val suggestions = analyzeBreakpointSuggestions(psiFile, args.methodName)
            val suggestionsJson = suggestions.joinToString(",\n", prefix = "[", postfix = "]") { suggestion ->
                """
                {
                    "lineNumber": ${suggestion.lineNumber},
                    "reason": "${suggestion.reason}",
                    "description": "${JsonUtils.escapeJson(suggestion.description)}",
                    "priority": "${suggestion.priority}"
                }
                """.trimIndent()
            }
            Response(suggestionsJson)
            
        } catch (e: Exception) {
            Response(error = "Error analyzing breakpoint suggestions: ${e.message}")
        }
    }
    
    private fun analyzeBreakpointSuggestions(javaFile: PsiJavaFile, methodName: String?): List<BreakpointSuggestion> {
        val allSuggestions = mutableListOf<BreakpointSuggestion>()
        
        javaFile.classes.forEach { psiClass ->
            psiClass.methods.forEach { method ->
                if (methodName == null || method.name == methodName) {
                    val methodSuggestions = AstUtils.suggestBreakpointLines(method)
                    allSuggestions.addAll(methodSuggestions)
                }
            }
        }
        
        return allSuggestions.sortedBy { it.lineNumber }
    }
}

@Serializable
data class MethodDetailsArgs(
    val filePath: String,
    val methodName: String? = null
)

class GetMethodDetailsTool : AbstractMcpTool<MethodDetailsArgs>(MethodDetailsArgs.serializer()) {
    override val name = "java_get_method_details"
    override val description = """
        Get detailed information about all methods in a Java file including:
        - Method signature line number
        - First executable line number  
        - Method body line range
        - Complexity metrics
        - Parameter details
        - Breakpoint suggestions
        
        Parameters:
        - filePath: Path to the Java file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of MethodDetails with comprehensive method information
        Error: File not found, not a Java file, or analysis errors
    """
    
    override fun handle(project: Project, args: MethodDetailsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            if (psiFile !is PsiJavaFile) {
                return Response(error = "Not a Java file: ${args.filePath}")
            }
            
            val methodDetails = analyzeMethodDetails(psiFile, args.methodName, args.filePath)
            val detailsJson = methodDetails.joinToString(",\n", prefix = "[", postfix = "]") { details ->
                val suggestionsJson = details.breakpointSuggestions.joinToString(",\n", prefix = "[", postfix = "]") { suggestion ->
                    """
                    {
                        "lineNumber": ${suggestion.lineNumber},
                        "reason": "${suggestion.reason}",
                        "description": "${JsonUtils.escapeJson(suggestion.description)}",
                        "priority": "${suggestion.priority}"
                    }
                    """.trimIndent()
                }
                
                """
                {
                    "name": "${JsonUtils.escapeJson(details.name)}",
                    "className": "${JsonUtils.escapeJson(details.className)}",
                    "methodName": "${JsonUtils.escapeJson(details.name)}",
                    "file": "${JsonUtils.escapeJson(details.file)}",
                    "lineRange": {
                        "signatureLineNumber": ${details.lineRange.signatureLineNumber},
                        "firstExecutableLineNumber": ${details.lineRange.firstExecutableLineNumber ?: -1},
                        "lastLineNumber": ${details.lineRange.lastLineNumber},
                        "lastExecutableLineNumber": ${details.lineRange.lastExecutableLineNumber ?: -1},
                        "bodyStartLine": ${details.lineRange.bodyStartLine ?: -1},
                        "bodyEndLine": ${details.lineRange.bodyEndLine ?: -1}
                    },
                    "complexity": {
                        "cyclomaticComplexity": ${details.complexity.cyclomaticComplexity},
                        "linesOfCode": ${details.complexity.linesOfCode},
                        "parameterCount": ${details.complexity.parameterCount}
                    },
                    "breakpointSuggestions": $suggestionsJson
                }
                """.trimIndent()
            }
            Response(detailsJson)
            
        } catch (e: Exception) {
            Response(error = "Error analyzing method details: ${e.message}")
        }
    }
    
    private fun analyzeMethodDetails(javaFile: PsiJavaFile, methodName: String?, filePath: String): List<MethodDetails> {
        val methodDetailsList = mutableListOf<MethodDetails>()
        
        javaFile.classes.forEach { psiClass ->
            psiClass.methods.forEach { method ->
                if (methodName == null || method.name == methodName) {
                    val lineRange = AstUtils.getMethodLineRange(method)
                    val breakpointSuggestions = AstUtils.suggestBreakpointLines(method)
                    val complexity = ComplexityMetric(
                        methodName = method.name,
                        className = psiClass.name ?: "Unknown",
                        cyclomaticComplexity = AstUtils.calculateComplexity(method),
                        linesOfCode = AstUtils.countLinesOfCode(method),
                        parameterCount = method.parameterList.parametersCount,
                        file = filePath,
                        lineNumber = AstUtils.getLineNumber(method)
                    )
                    
                    val details = MethodDetails(
                        name = method.name,
                        className = psiClass.name ?: "Unknown",
                        file = filePath,
                        lineRange = lineRange,
                        complexity = complexity,
                        parameters = AstUtils.getMethodParameters(method),
                        breakpointSuggestions = breakpointSuggestions
                    )
                    methodDetailsList.add(details)
                }
            }
        }
        
        return methodDetailsList
    }
}
