package ai.voitta.jetbrains.ast.php

import ai.voitta.jetbrains.ast.common.LanguageAnalyzerFactory
import ai.voitta.jetbrains.utils.JsonUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import kotlinx.serialization.Serializable
import org.jetbrains.ide.mcp.Response
import org.jetbrains.mcpserverplugin.AbstractMcpTool
import java.nio.file.Paths

/**
 * PHP-specific MCP tools using the common language analyzer framework
 */

@Serializable
data class PhpFileAstArgs(
    val filePath: String, 
    val includeMethodBodies: Boolean = false,
    val includePrivateMembers: Boolean = true
)

class GetPhpFileAstTool : AbstractMcpTool<PhpFileAstArgs>(PhpFileAstArgs.serializer()) {
    override val name = "php_get_file_ast"
    override val description = """
        Retrieves the AST (Abstract Syntax Tree) for a PHP file.
        Returns complete class structure including methods, fields, and metadata.
        
        Parameters:
        - filePath: Path to the PHP file (relative to project root)
        - includeMethodBodies: Whether to include detailed method body statements (default: false)
        - includePrivateMembers: Whether to include private methods and fields (default: true)
        
        Returns: List of ClassNode with complete file structure
        Error: File not found, not a PHP file, or parsing errors
    """
    
    override fun handle(project: Project, args: PhpFileAstArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}")
            
            if (analyzer.languageName != "PHP") {
                return Response(error = "Not a PHP file: ${args.filePath}")
            }
            
            val classes = analyzer.analyzeFile(psiFile, args.includeMethodBodies, args.includePrivateMembers)
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
            Response(error = "Error analyzing PHP file: ${e.message}")
        }
    }
}

@Serializable
data class PhpMethodDetailsArgs(
    val filePath: String,
    val methodName: String? = null
)

class GetPhpMethodDetailsTool : AbstractMcpTool<PhpMethodDetailsArgs>(PhpMethodDetailsArgs.serializer()) {
    override val name = "php_get_method_details"
    override val description = """
        Get detailed information about all methods in a PHP file including:
        - Method signature line number
        - First executable line number  
        - Method body line range
        - Complexity metrics
        - Parameter details
        - Breakpoint suggestions
        
        Parameters:
        - filePath: Path to the PHP file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of MethodDetails with comprehensive method information
        Error: File not found, not a PHP file, or analysis errors
    """
    
    override fun handle(project: Project, args: PhpMethodDetailsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}")
            
            if (analyzer.languageName != "PHP") {
                return Response(error = "Not a PHP file: ${args.filePath}")
            }
            
            val methodDetails = analyzer.getMethodDetails(project, psiFile, args.methodName)
            Response(JsonUtils.toJson(methodDetails))
            
        } catch (e: Exception) {
            Response(error = "Error analyzing PHP methods: ${e.message}")
        }
    }
}

@Serializable
data class PhpBreakpointSuggestionsArgs(
    val filePath: String,
    val methodName: String? = null
)

class SuggestPhpBreakpointLinesTool : AbstractMcpTool<PhpBreakpointSuggestionsArgs>(PhpBreakpointSuggestionsArgs.serializer()) {
    override val name = "php_suggest_breakpoint_lines"
    override val description = """
        Suggests optimal line numbers for setting breakpoints in PHP methods.
        Returns first executable line, key decision points, and method entry/exit points.
        
        Parameters:
        - filePath: Path to the PHP file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of BreakpointSuggestion with recommended breakpoint locations
        Error: File not found, not a PHP file, or analysis errors
    """
    
    override fun handle(project: Project, args: PhpBreakpointSuggestionsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}")
            
            if (analyzer.languageName != "PHP") {
                return Response(error = "Not a PHP file: ${args.filePath}")
            }
            
            val suggestions = analyzer.suggestBreakpointLines(project, psiFile, args.methodName)
            Response(JsonUtils.toJson(suggestions))
            
        } catch (e: Exception) {
            Response(error = "Error generating PHP breakpoint suggestions: ${e.message}")
        }
    }
}

@Serializable
data class PhpComplexityArgs(
    val filePath: String,
    val methodName: String? = null
)

class GetPhpMethodComplexityTool : AbstractMcpTool<PhpComplexityArgs>(PhpComplexityArgs.serializer()) {
    override val name = "php_get_method_complexity"
    override val description = """
        Calculates cyclomatic complexity for methods in a PHP file.
        
        Parameters:
        - filePath: Path to the PHP file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of ComplexityMetric for methods
        Error: File not found, not a PHP file, or analysis errors
    """
    
    override fun handle(project: Project, args: PhpComplexityArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}")
            
            if (analyzer.languageName != "PHP") {
                return Response(error = "Not a PHP file: ${args.filePath}")
            }
            
            val complexityMetrics = analyzer.getMethodComplexity(project, psiFile, args.methodName)
            Response(JsonUtils.toJson(complexityMetrics))
            
        } catch (e: Exception) {
            Response(error = "Error calculating PHP method complexity: ${e.message}")
        }
    }
}

@Serializable
data class PhpCodePatternsArgs(
    val filePath: String,
    val patterns: List<String>
)

class DetectPhpCodePatternsTool : AbstractMcpTool<PhpCodePatternsArgs>(PhpCodePatternsArgs.serializer()) {
    override val name = "php_detect_code_patterns"
    override val description = """
        Detects common code patterns and potential issues in a PHP file.
        
        Parameters:
        - filePath: Path to the PHP file (relative to project root)
        - patterns: List of patterns to detect ("null_checks", "exception_handling", "loops", "all")
        
        Returns: List of CodePattern with detected patterns and suggestions
        Error: File not found, not a PHP file, or analysis errors
    """
    
    override fun handle(project: Project, args: PhpCodePatternsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}")
            
            if (analyzer.languageName != "PHP") {
                return Response(error = "Not a PHP file: ${args.filePath}")
            }
            
            val patterns = analyzer.detectCodePatterns(project, psiFile, args.patterns)
            Response(JsonUtils.toJson(patterns))
            
        } catch (e: Exception) {
            Response(error = "Error detecting PHP code patterns: ${e.message}")
        }
    }
}

@Serializable
data class PhpClassHierarchyArgs(
    val className: String,
    val direction: String = "both"
)

class GetPhpClassHierarchyTool : AbstractMcpTool<PhpClassHierarchyArgs>(PhpClassHierarchyArgs.serializer()) {
    override val name = "php_get_class_hierarchy"
    override val description = """
        Retrieves class hierarchy information for a given PHP class.
        Shows inheritance relationships, implemented interfaces, and subclasses.
        
        Parameters:
        - className: Fully qualified class name or simple name
        - direction: "up" (superclasses), "down" (subclasses), "both" (default: "both")
        
        Returns: HierarchyNode with inheritance relationships
        Error: Class not found or analysis errors
    """
    
    override fun handle(project: Project, args: PhpClassHierarchyArgs): Response {
        return try {
            val analyzer = LanguageAnalyzerFactory.getAnalyzerByLanguage("PHP")
                ?: return Response(error = "PHP analyzer not available")
            
            val hierarchy = analyzer.getClassHierarchy(project, args.className, args.direction)
                ?: return Response(error = "Class not found: ${args.className}")
            
            Response(JsonUtils.toJson(hierarchy))
            
        } catch (e: Exception) {
            Response(error = "Error analyzing PHP class hierarchy: ${e.message}")
        }
    }
}
