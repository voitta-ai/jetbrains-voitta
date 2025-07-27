package org.jetbrains.mcpextensiondemo.ast.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import kotlinx.serialization.Serializable
import org.jetbrains.ide.mcp.Response
import org.jetbrains.mcpserverplugin.AbstractMcpTool
import org.jetbrains.mcpextensiondemo.utils.JsonUtils
import org.jetbrains.mcpextensiondemo.ast.*
import java.nio.file.Paths

/**
 * Language-agnostic navigation and symbol analysis tools
 */

@Serializable
data class SymbolAtPositionArgs(
    val filePath: String,
    val line: Int,
    val column: Int
)

class GetSymbolAtPositionTool : AbstractMcpTool<SymbolAtPositionArgs>(SymbolAtPositionArgs.serializer()) {
    override val name = "get_symbol_at_position"
    override val description = """
        Retrieves symbol information at a specific position in a file.
        
        Parameters:
        - filePath: Path to the file (relative to project root)
        - line: Line number (1-based)
        - column: Column number (1-based)
        
        Returns: SymbolInfo with symbol details, declaration location, and documentation
        Error: File not found, position invalid, or no symbol at position
    """
    
    override fun handle(project: Project, args: SymbolAtPositionArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val element = findElementAtPosition(psiFile, args.line, args.column)
                ?: return Response(error = "No symbol found at position ${args.line}:${args.column}")
            
            val symbolInfo = analyzeSymbol(element)
            val symbolJson = """
            {
                "name": "${JsonUtils.escapeJson(symbolInfo.name)}",
                "type": "${symbolInfo.type}",
                "declarationFile": "${JsonUtils.escapeJson(symbolInfo.declarationFile ?: "")}",
                "declarationLine": ${symbolInfo.declarationLine ?: -1},
                "declarationColumn": ${symbolInfo.declarationColumn ?: -1},
                "signature": "${JsonUtils.escapeJson(symbolInfo.signature ?: "")}",
                "documentation": "${JsonUtils.escapeJson(symbolInfo.documentation ?: "")}",
                "fullyQualifiedName": "${JsonUtils.escapeJson(symbolInfo.fullyQualifiedName ?: "")}"
            }
            """.trimIndent()
            Response(symbolJson)
            
        } catch (e: Exception) {
            Response(error = "Error analyzing symbol: ${e.message}")
        }
    }
    
    private fun findElementAtPosition(file: PsiFile, line: Int, column: Int): PsiElement? {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file.virtualFile)
            ?: return null
        
        val lineIndex = line - 1 // Convert to 0-based
        if (lineIndex < 0 || lineIndex >= document.lineCount) return null
        
        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val columnOffset = column - 1 // Convert to 0-based
        val offset = lineStartOffset + columnOffset
        
        if (offset >= document.textLength) return null
        
        return file.findElementAt(offset)
    }
    
    private fun analyzeSymbol(element: PsiElement): SymbolInfo {
        val reference = element.reference
        val resolved = reference?.resolve() ?: element
        
        return when (resolved) {
            is PsiClass -> analyzeClass(resolved)
            is PsiMethod -> analyzeMethod(resolved)
            is PsiField -> analyzeField(resolved)
            is PsiVariable -> analyzeVariable(resolved)
            else -> SymbolInfo(
                name = element.text,
                type = "UNKNOWN",
                declarationFile = null,
                declarationLine = null,
                signature = null,
                documentation = null
            )
        }
    }
    
    private fun analyzeClass(psiClass: PsiClass): SymbolInfo {
        return SymbolInfo(
            name = psiClass.name ?: "Unknown",
            type = "CLASS",
            declarationFile = psiClass.containingFile?.virtualFile?.let { 
                AstUtils.getRelativePath(psiClass.project, it) 
            },
            declarationLine = AstUtils.getLineNumber(psiClass),
            declarationColumn = AstUtils.getColumnNumber(psiClass),
            signature = psiClass.qualifiedName,
            documentation = AstUtils.extractJavadoc(psiClass.docComment),
            fullyQualifiedName = psiClass.qualifiedName
        )
    }
    
    private fun analyzeMethod(method: PsiMethod): SymbolInfo {
        return SymbolInfo(
            name = method.name,
            type = "METHOD",
            declarationFile = method.containingFile?.virtualFile?.let { 
                AstUtils.getRelativePath(method.project, it) 
            },
            declarationLine = AstUtils.getLineNumber(method),
            declarationColumn = AstUtils.getColumnNumber(method),
            signature = AstUtils.formatSignature(method),
            documentation = AstUtils.extractJavadoc(method.docComment),
            containingClass = method.containingClass?.name,
            returnType = method.returnType?.presentableText,
            parameterTypes = method.parameterList.parameters.map { it.type.presentableText }
        )
    }
    
    private fun analyzeField(field: PsiField): SymbolInfo {
        return SymbolInfo(
            name = field.name,
            type = "FIELD",
            declarationFile = field.containingFile?.virtualFile?.let { 
                AstUtils.getRelativePath(field.project, it) 
            },
            declarationLine = AstUtils.getLineNumber(field),
            declarationColumn = AstUtils.getColumnNumber(field),
            signature = "${field.type.presentableText} ${field.name}",
            documentation = null, // Fields don't typically have JavaDoc
            containingClass = field.containingClass?.name,
            returnType = field.type.presentableText
        )
    }
    
    private fun analyzeVariable(variable: PsiVariable): SymbolInfo {
        val type = when (variable) {
            is PsiParameter -> "PARAMETER"
            is PsiLocalVariable -> "LOCAL_VARIABLE"
            else -> "VARIABLE"
        }
        
        return SymbolInfo(
            name = variable.name ?: "Unknown",
            type = type,
            declarationFile = variable.containingFile?.virtualFile?.let { 
                AstUtils.getRelativePath(variable.project, it) 
            },
            declarationLine = AstUtils.getLineNumber(variable),
            declarationColumn = AstUtils.getColumnNumber(variable),
            signature = "${variable.type.presentableText} ${variable.name}",
            documentation = null,
            returnType = variable.type.presentableText
        )
    }
}

@Serializable
data class FindReferencesArgs(
    val filePath: String,
    val line: Int,
    val column: Int
)

class FindAllReferencesTool : AbstractMcpTool<FindReferencesArgs>(FindReferencesArgs.serializer()) {
    override val name = "find_all_references"
    override val description = """
        Finds all references to a symbol at the specified position.
        
        Parameters:
        - filePath: Path to the file (relative to project root)
        - line: Line number (1-based)
        - column: Column number (1-based)
        
        Returns: List of UsageNode with all references to the symbol
        Error: File not found, position invalid, or no symbol at position
    """
    
    override fun handle(project: Project, args: FindReferencesArgs): Response {
        return try {
            val projectPath = Paths.get(project.basePath ?: "")
            val filePath = projectPath.resolve(args.filePath.removePrefix("/"))
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString())
                ?: return Response(error = "File not found: ${args.filePath}")
            
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return Response(error = "Could not parse file: ${args.filePath}")
            
            val element = findElementAtPosition(psiFile, args.line, args.column)
                ?: return Response(error = "No symbol found at position ${args.line}:${args.column}")
            
            val targetElement = findElementByName(element)
                ?: return Response(error = "Could not resolve symbol")
            
            val references = findReferences(targetElement)
            val referencesJson = references.joinToString(",\n", prefix = "[", postfix = "]") { usage ->
                """
                {
                    "file": "${JsonUtils.escapeJson(usage.file)}",
                    "line": ${usage.line},
                    "column": ${usage.column},
                    "context": "${JsonUtils.escapeJson(usage.context)}",
                    "usageType": "${usage.usageType}",
                    "elementType": "${usage.elementType}"
                }
                """.trimIndent()
            }
            Response(referencesJson)
            
        } catch (e: Exception) {
            Response(error = "Error finding references: ${e.message}")
        }
    }
    
    private fun findElementAtPosition(file: PsiFile, line: Int, column: Int): PsiElement? {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file.virtualFile)
            ?: return null
        
        val lineIndex = line - 1
        if (lineIndex < 0 || lineIndex >= document.lineCount) return null
        
        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val columnOffset = column - 1
        val offset = lineStartOffset + columnOffset
        
        if (offset >= document.textLength) return null
        
        return file.findElementAt(offset)
    }
    
    private fun findElementByName(element: PsiElement): PsiElement? {
        val reference = element.reference
        return reference?.resolve() ?: element
    }
    
    private fun findReferences(element: PsiElement): List<UsageNode> {
        val references = ReferencesSearch.search(element).findAll()
        
        return references.mapNotNull { reference ->
            val refElement = reference.element
            val file = refElement.containingFile?.virtualFile ?: return@mapNotNull null
            val relativePath = AstUtils.getRelativePath(element.project, file)
            
            UsageNode(
                file = relativePath,
                line = AstUtils.getLineNumber(refElement),
                column = AstUtils.getColumnNumber(refElement),
                context = getContext(refElement),
                usageType = determineUsageType(refElement),
                elementType = determineElementType(element)
            )
        }
    }
    
    private fun getContext(element: PsiElement): String {
        val parent = element.parent
        return when (parent) {
            is PsiMethodCallExpression -> "METHOD_CALL"
            is PsiAssignmentExpression -> "ASSIGNMENT"
            is PsiDeclarationStatement -> "DECLARATION"
            else -> parent?.javaClass?.simpleName ?: "UNKNOWN"
        }
    }
    
    private fun determineUsageType(element: PsiElement): String {
        val parent = element.parent
        return when (parent) {
            is PsiMethodCallExpression -> "METHOD_CALL"
            is PsiAssignmentExpression -> {
                if (parent.lExpression == element) "WRITE" else "READ"
            }
            is PsiDeclarationStatement -> "DECLARATION"
            is PsiImportStatement -> "IMPORT"
            else -> "READ"
        }
    }
    
    private fun determineElementType(element: PsiElement): String {
        return when (element) {
            is PsiClass -> "CLASS"
            is PsiMethod -> "METHOD"
            is PsiField -> "FIELD"
            is PsiVariable -> "VARIABLE"
            else -> "UNKNOWN"
        }
    }
}
