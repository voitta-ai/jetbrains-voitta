package org.jetbrains.mcpextensiondemo.ast

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.javadoc.PsiDocComment
import kotlin.math.max

/**
 * Utility functions for AST analysis and PSI manipulation
 */
object AstUtils {
    
    /**
     * Gets the line number (1-based) for a PSI element
     */
    fun getLineNumber(element: PsiElement): Int {
        val document = getDocument(element) ?: return -1
        val offset = element.textOffset
        return document.getLineNumber(offset) + 1 // Convert to 1-based
    }
    
    /**
     * Gets the column number (1-based) for a PSI element
     */
    fun getColumnNumber(element: PsiElement): Int {
        val document = getDocument(element) ?: return -1
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        return offset - lineStartOffset + 1 // Convert to 1-based
    }
    
    /**
     * Gets the relative path from project root to the file
     */
    fun getRelativePath(project: Project, file: VirtualFile): String {
        val projectDir = project.basePath ?: return file.path
        val projectPath = java.nio.file.Paths.get(projectDir)
        val filePath = java.nio.file.Paths.get(file.path)
        
        return try {
            projectPath.relativize(filePath).toString()
        } catch (e: IllegalArgumentException) {
            file.path
        }
    }
    
    /**
     * Calculates cyclomatic complexity for a method
     */
    fun calculateComplexity(method: PsiMethod): Int {
        var complexity = 1 // Base complexity
        
        val visitor = object : JavaRecursiveElementVisitor() {
            override fun visitIfStatement(statement: PsiIfStatement) {
                complexity++
                super.visitIfStatement(statement)
            }
            
            override fun visitWhileStatement(statement: PsiWhileStatement) {
                complexity++
                super.visitWhileStatement(statement)
            }
            
            override fun visitForStatement(statement: PsiForStatement) {
                complexity++
                super.visitForStatement(statement)
            }
            
            override fun visitForeachStatement(statement: PsiForeachStatement) {
                complexity++
                super.visitForeachStatement(statement)
            }
            
            override fun visitSwitchStatement(statement: PsiSwitchStatement) {
                // Add complexity for each case
                val caseCount = statement.body?.statements?.count { it is PsiSwitchLabelStatement } ?: 0
                complexity += max(1, caseCount)
                super.visitSwitchStatement(statement)
            }
            
            override fun visitConditionalExpression(expression: PsiConditionalExpression) {
                complexity++
                super.visitConditionalExpression(expression)
            }
            
            override fun visitTryStatement(statement: PsiTryStatement) {
                // Add complexity for each catch block
                complexity += statement.catchSections.size
                super.visitTryStatement(statement)
            }
        }
        
        method.accept(visitor)
        return complexity
    }
    
    /**
     * Extracts modifiers from a modifier list
     */
    fun extractModifiers(modifierList: PsiModifierList?): List<String> {
        if (modifierList == null) return emptyList()
        
        val modifiers = mutableListOf<String>()
        
        if (modifierList.hasModifierProperty(PsiModifier.PUBLIC)) modifiers.add("public")
        if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) modifiers.add("private")
        if (modifierList.hasModifierProperty(PsiModifier.PROTECTED)) modifiers.add("protected")
        if (modifierList.hasModifierProperty(PsiModifier.STATIC)) modifiers.add("static")
        if (modifierList.hasModifierProperty(PsiModifier.FINAL)) modifiers.add("final")
        if (modifierList.hasModifierProperty(PsiModifier.ABSTRACT)) modifiers.add("abstract")
        if (modifierList.hasModifierProperty(PsiModifier.SYNCHRONIZED)) modifiers.add("synchronized")
        if (modifierList.hasModifierProperty(PsiModifier.VOLATILE)) modifiers.add("volatile")
        if (modifierList.hasModifierProperty(PsiModifier.TRANSIENT)) modifiers.add("transient")
        if (modifierList.hasModifierProperty(PsiModifier.NATIVE)) modifiers.add("native")
        if (modifierList.hasModifierProperty(PsiModifier.STRICTFP)) modifiers.add("strictfp")
        
        return modifiers
    }
    
    /**
     * Extracts annotations from a modifier list
     */
    fun extractAnnotations(modifierList: PsiModifierList?): List<AnnotationNode> {
        if (modifierList == null) return emptyList()
        
        return modifierList.annotations.map { annotation ->
            val parameters = mutableMapOf<String, String>()
            
            annotation.parameterList.attributes.forEach { attribute ->
                val name = attribute.name ?: "value"
                val value = attribute.value?.text ?: ""
                parameters[name] = value
            }
            
            AnnotationNode(
                name = annotation.nameReferenceElement?.referenceName ?: "",
                fullyQualifiedName = annotation.qualifiedName,
                parameters = parameters
            )
        }
    }
    
    /**
     * Formats a method signature
     */
    fun formatSignature(method: PsiMethod): String {
        val returnType = method.returnType?.presentableText ?: "void"
        val parameters = method.parameterList.parameters.joinToString(", ") { param ->
            "${param.type.presentableText} ${param.name}"
        }
        return "${method.name}($parameters): $returnType"
    }
    
    /**
     * Extracts Javadoc text from a doc comment
     */
    fun extractJavadoc(docComment: PsiDocComment?): String? {
        if (docComment == null) return null
        
        val text = docComment.text
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("/**") && !it.startsWith("*/") }
            .joinToString("\n") { it.removePrefix("*").trim() }
            .takeIf { it.isNotBlank() }
    }
    
    /**
     * Gets the document for a PSI element
     */
    private fun getDocument(element: PsiElement): Document? {
        val file = element.containingFile?.virtualFile ?: return null
        return FileDocumentManager.getInstance().getDocument(file)
    }
    
    /**
     * Gets the fully qualified name of a class
     */
    fun getFullyQualifiedName(psiClass: PsiClass): String? {
        return psiClass.qualifiedName
    }
    
    /**
     * Checks if a class is a user-defined class (not from standard libraries)
     */
    fun isUserCode(psiClass: PsiClass): Boolean {
        val qualifiedName = psiClass.qualifiedName ?: return true
        return !qualifiedName.startsWith("java.") && 
               !qualifiedName.startsWith("javax.") &&
               !qualifiedName.startsWith("sun.") &&
               !qualifiedName.startsWith("com.sun.")
    }
    
    /**
     * Converts PSI type to string representation
     */
    fun typeToString(type: PsiType?): String? {
        return type?.presentableText
    }
    
    /**
     * Gets the containing class for a PSI element
     */
    fun getContainingClass(element: PsiElement): PsiClass? {
        return PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
    }
    
    /**
     * Gets method parameters as ParameterNode list
     */
    fun getMethodParameters(method: PsiMethod): List<ParameterNode> {
        return method.parameterList.parameters.map { param ->
            ParameterNode(
                name = param.name,
                type = param.type.presentableText,
                annotations = extractAnnotations(param.modifierList),
                isVarArgs = param.isVarArgs
            )
        }
    }
    
    /**
     * Gets thrown exceptions for a method
     */
    fun getThrownExceptions(method: PsiMethod): List<String> {
        return method.throwsList.referenceElements.mapNotNull { ref ->
            ref.qualifiedName
        }
    }
    
    /**
     * Safely gets text range for error handling
     */
    fun safeGetText(element: PsiElement?): String {
        return try {
            element?.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Creates a truncated code snippet for patterns
     */
    fun createCodeSnippet(element: PsiElement, maxLength: Int = 200): String {
        val text = safeGetText(element)
        return if (text.length > maxLength) {
            text.take(maxLength) + "..."
        } else {
            text
        }
    }
    
    /**
     * Determines if a method is a constructor
     */
    fun isConstructor(method: PsiMethod): Boolean {
        return method.isConstructor
    }
    
    /**
     * Gets the package name for a PSI file
     */
    fun getPackageName(file: PsiJavaFile): String? {
        return file.packageName.takeIf { it.isNotEmpty() }
    }
    
    /**
     * Counts lines of code in a method (excluding comments and empty lines)
     */
    fun countLinesOfCode(method: PsiMethod): Int {
        val body = method.body ?: return 0
        val text = body.text
        
        return text.lines().count { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && 
            !trimmed.startsWith("//") && 
            !trimmed.startsWith("/*") && 
            !trimmed.startsWith("*")
        }
    }
}
