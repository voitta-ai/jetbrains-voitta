package ai.voitta.jetbrains.ast.common

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
 * Universal language-agnostic MCP tools that route to appropriate language analyzers
 */

@Serializable
data class UniversalFileAstArgs(
    val filePath: String, 
    val includeMethodBodies: Boolean = false,
    val includePrivateMembers: Boolean = true
)

class GetUniversalFileAstTool : AbstractMcpTool<UniversalFileAstArgs>(UniversalFileAstArgs.serializer()) {
    override val name = "get_file_ast"
    override val description = """
        Retrieves the AST (Abstract Syntax Tree) for a source file (supports Java, PHP).
        Automatically detects the language and uses the appropriate analyzer.
        Returns complete class structure including methods, fields, and metadata.
        
        Parameters:
        - filePath: Path to the source file (relative to project root)
        - includeMethodBodies: Whether to include detailed method body statements (default: false)
        - includePrivateMembers: Whether to include private methods and fields (default: true)
        
        Returns: List of ClassNode with complete file structure
        Error: File not found, unsupported file type, or parsing errors
    """
    
    override fun handle(project: Project, args: UniversalFileAstArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}. Supported languages: ${LanguageAnalyzerFactory.getSupportedLanguages().joinToString(", ")}")
            
            val classes = analyzer.analyzeFile(project, psiFile, args.includeMethodBodies, args.includePrivateMembers)
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
                    "language": "${analyzer.languageName}",
                    "methods": $methodsJson
                }
                """.trimIndent()
            }
            Response(classesJson)
            
        } catch (e: Exception) {
            Response(error = "Error analyzing file: ${e.message}")
        }
    }
}

@Serializable
data class UniversalMethodDetailsArgs(
    val filePath: String,
    val methodName: String? = null
)

class GetUniversalMethodDetailsTool : AbstractMcpTool<UniversalMethodDetailsArgs>(UniversalMethodDetailsArgs.serializer()) {
    override val name = "get_method_details"
    override val description = """
        Get detailed information about all methods/functions in a source file (supports Java, PHP).
        Automatically detects the language and uses the appropriate analyzer.
        
        Returns:
        - Method signature line number
        - First executable line number  
        - Method body line range
        - Complexity metrics
        - Parameter details
        - Breakpoint suggestions
        
        Parameters:
        - filePath: Path to the source file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of MethodDetails with comprehensive method information
        Error: File not found, unsupported file type, or analysis errors
    """
    
    override fun handle(project: Project, args: UniversalMethodDetailsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}. Supported languages: ${LanguageAnalyzerFactory.getSupportedLanguages().joinToString(", ")}")
            
            val methodDetails = analyzer.getMethodDetails(project, psiFile, args.methodName)
            Response(JsonUtils.toJson(methodDetails))
            
        } catch (e: Exception) {
            Response(error = "Error analyzing methods: ${e.message}")
        }
    }
}

@Serializable
data class UniversalBreakpointSuggestionsArgs(
    val filePath: String,
    val methodName: String? = null
)

class SuggestUniversalBreakpointLinesTool : AbstractMcpTool<UniversalBreakpointSuggestionsArgs>(UniversalBreakpointSuggestionsArgs.serializer()) {
    override val name = "suggest_breakpoint_lines"
    override val description = """
        Suggests optimal line numbers for setting breakpoints in methods/functions (supports Java, PHP).
        Automatically detects the language and uses the appropriate analyzer.
        Returns first executable line, key decision points, and method entry/exit points.
        
        Parameters:
        - filePath: Path to the source file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of BreakpointSuggestion with recommended breakpoint locations
        Error: File not found, unsupported file type, or analysis errors
    """
    
    override fun handle(project: Project, args: UniversalBreakpointSuggestionsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}. Supported languages: ${LanguageAnalyzerFactory.getSupportedLanguages().joinToString(", ")}")
            
            val suggestions = analyzer.suggestBreakpointLines(project, psiFile, args.methodName)
            Response(JsonUtils.toJson(suggestions))
            
        } catch (e: Exception) {
            Response(error = "Error generating breakpoint suggestions: ${e.message}")
        }
    }
}

@Serializable
data class UniversalComplexityArgs(
    val filePath: String,
    val methodName: String? = null
)

class GetUniversalMethodComplexityTool : AbstractMcpTool<UniversalComplexityArgs>(UniversalComplexityArgs.serializer()) {
    override val name = "get_method_complexity"
    override val description = """
        Calculates cyclomatic complexity for methods/functions in a source file (supports Java, PHP).
        Automatically detects the language and uses the appropriate analyzer.
        
        Parameters:
        - filePath: Path to the source file (relative to project root)
        - methodName: Specific method name (optional, if null analyzes all methods)
        
        Returns: List of ComplexityMetric for methods
        Error: File not found, unsupported file type, or analysis errors
    """
    
    override fun handle(project: Project, args: UniversalComplexityArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}. Supported languages: ${LanguageAnalyzerFactory.getSupportedLanguages().joinToString(", ")}")
            
            val complexityMetrics = analyzer.getMethodComplexity(project, psiFile, args.methodName)
            Response(JsonUtils.toJson(complexityMetrics))
            
        } catch (e: Exception) {
            Response(error = "Error calculating method complexity: ${e.message}")
        }
    }
}

@Serializable
data class UniversalCodePatternsArgs(
    val filePath: String,
    val patterns: List<String>
)

class DetectUniversalCodePatternsTool : AbstractMcpTool<UniversalCodePatternsArgs>(UniversalCodePatternsArgs.serializer()) {
    override val name = "detect_code_patterns"
    override val description = """
        Detects common code patterns and potential issues in a source file (supports Java, PHP).
        Automatically detects the language and uses the appropriate analyzer.
        
        Parameters:
        - filePath: Path to the source file (relative to project root)
        - patterns: List of patterns to detect ("null_checks", "exception_handling", "loops", "all")
        
        Returns: List of CodePattern with detected patterns and suggestions
        Error: File not found, unsupported file type, or analysis errors
    """
    
    override fun handle(project: Project, args: UniversalCodePatternsArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val analyzer = LanguageAnalyzerFactory.getAnalyzer(psiFile)
                ?: return Response(error = "No analyzer available for file type: ${args.filePath}. Supported languages: ${LanguageAnalyzerFactory.getSupportedLanguages().joinToString(", ")}")
            
            val patterns = analyzer.detectCodePatterns(project, psiFile, args.patterns)
            Response(JsonUtils.toJson(patterns))
            
        } catch (e: Exception) {
            Response(error = "Error detecting code patterns: ${e.message}")
        }
    }
}

@Serializable
data class UniversalClassHierarchyArgs(
    val className: String,
    val direction: String = "both",
    val language: String? = null // Optional language hint
)

class GetUniversalClassHierarchyTool : AbstractMcpTool<UniversalClassHierarchyArgs>(UniversalClassHierarchyArgs.serializer()) {
    override val name = "get_class_hierarchy"
    override val description = """
        Retrieves class hierarchy information for a given class (supports Java, PHP).
        Shows inheritance relationships, implemented interfaces, and subclasses.
        
        Parameters:
        - className: Fully qualified class name or simple name
        - direction: "up" (superclasses), "down" (subclasses), "both" (default: "both")
        - language: Optional language hint ("Java" or "PHP"). If not provided, searches all supported languages.
        
        Returns: HierarchyNode with inheritance relationships
        Error: Class not found or analysis errors
    """
    
    override fun handle(project: Project, args: UniversalClassHierarchyArgs): Response {
        return try {
            val analyzers = if (args.language != null) {
                val analyzer = LanguageAnalyzerFactory.getAnalyzerByLanguage(args.language)
                if (analyzer != null) listOf(analyzer) else emptyList()
            } else {
                // Try all available analyzers
                LanguageAnalyzerFactory.getSupportedLanguages().mapNotNull { 
                    LanguageAnalyzerFactory.getAnalyzerByLanguage(it) 
                }
            }
            
            if (analyzers.isEmpty()) {
                return Response(error = "No analyzers available for language: ${args.language}")
            }
            
            // Try each analyzer until we find the class
            for (analyzer in analyzers) {
                val hierarchy = analyzer.getClassHierarchy(project, args.className, args.direction)
                if (hierarchy != null) {
                    return Response(JsonUtils.toJson(hierarchy))
                }
            }
            
            return Response(error = "Class not found: ${args.className}")
            
        } catch (e: Exception) {
            Response(error = "Error analyzing class hierarchy: ${e.message}")
        }
    }
}
