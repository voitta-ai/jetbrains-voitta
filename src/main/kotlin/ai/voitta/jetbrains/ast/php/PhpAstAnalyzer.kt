package ai.voitta.jetbrains.ast.php

import ai.voitta.jetbrains.ast.*
import ai.voitta.jetbrains.ast.common.LanguageAstAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.*
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass

/**
 * PHP-specific AST analyzer using PhpStorm's PHP PSI
 */
class PhpAstAnalyzer : LanguageAstAnalyzer() {
    
    override val languageName = "PHP"
    
    override fun canAnalyze(psiFile: PsiFile): Boolean {
        return psiFile is PhpFile
    }
    
    override fun analyzeFile(
        project: Project,
        psiFile: PsiFile,
        includeMethodBodies: Boolean,
        includePrivateMembers: Boolean
    ): List<ClassNode> {
        if (psiFile !is PhpFile) {
            throw IllegalArgumentException("File is not a PHP file")
        }
        
        val classes = mutableListOf<ClassNode>()
        
        // Find all classes in the file
        val phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass::class.java)
        
        for (phpClass in phpClasses) {
            classes.add(analyzePhpClass(phpClass, includeMethodBodies, includePrivateMembers))
        }
        
        return classes
    }
    
    private fun analyzePhpClass(
        phpClass: PhpClass, 
        includeMethodBodies: Boolean,
        includePrivateMembers: Boolean
    ): ClassNode {
        val methods = mutableListOf<MethodNode>()
        val fields = mutableListOf<FieldNode>()
        
        // Analyze methods
        for (method in phpClass.ownMethods) {
            if (!includePrivateMembers && method.modifier.isPrivate) continue
            
            methods.add(analyzePhpMethod(method, includeMethodBodies))
        }
        
        // Analyze fields
        for (field in phpClass.ownFields) {
            if (!includePrivateMembers && field.modifier.isPrivate) continue
            
            fields.add(analyzePhpField(field))
        }
        
        // Get superclass and interfaces
        val superClass = phpClass.superClass?.name
        val interfaces = phpClass.implementedInterfaces.map { it.name }
        
        // Get modifiers
        val modifiers = mutableListOf<String>()
        if (phpClass.modifier.isAbstract) modifiers.add("abstract")
        if (phpClass.modifier.isFinal) modifiers.add("final")
        
        return ClassNode(
            name = phpClass.name,
            fullyQualifiedName = phpClass.fqn.toString(),
            superClass = superClass,
            interfaces = interfaces,
            modifiers = modifiers,
            methods = methods,
            fields = fields,
            packageName = phpClass.namespaceName,
            isInterface = phpClass.isInterface,
            isAbstract = phpClass.modifier.isAbstract,
            javadoc = phpClass.docComment?.text
        )
    }
    
    private fun analyzePhpMethod(method: Method, includeMethodBodies: Boolean): MethodNode {
        val parameters = method.parameters.map { param ->
            ParameterNode(
                name = param.name,
                type = param.type.toString(),
                hasDefaultValue = param.isOptional,
                defaultValue = param.defaultValue?.text,
                annotations = emptyList()
            )
        }
        
        val modifiers = mutableListOf<String>()
        val modifier = method.modifier
        if (modifier.isPublic) modifiers.add("public")
        if (modifier.isPrivate) modifiers.add("private")
        if (modifier.isProtected) modifiers.add("protected")
        if (modifier.isStatic) modifiers.add("static")
        if (modifier.isAbstract) modifiers.add("abstract")
        if (modifier.isFinal) modifiers.add("final")
        
        // Calculate line numbers
        val document = PsiDocumentManager.getInstance(method.project).getDocument(method.containingFile)
        val lineNumber = document?.getLineNumber(method.textOffset)?.plus(1)
        val firstExecutableLineNumber = getFirstExecutableLineNumber(method, document)
        val lastLineNumber = document?.getLineNumber(method.textRange.endOffset)?.plus(1)
        
        // Calculate complexity
        val complexity = calculateMethodComplexity(method)
        
        val bodyStatements = if (includeMethodBodies) {
            analyzeMethodBody(method)
        } else null
        
        return MethodNode(
            name = method.name,
            returnType = method.type.toString(),
            parameters = parameters,
            modifiers = modifiers,
            javadoc = method.docComment?.text,
            bodyStatements = bodyStatements,
            complexity = complexity,
            isConstructor = method.name == "__construct",
            isStatic = modifier.isStatic,
            isAbstract = modifier.isAbstract,
            lineNumber = lineNumber,
            firstExecutableLineNumber = firstExecutableLineNumber,
            lastLineNumber = lastLineNumber,
            bodyLineRange = MethodLineRange(
                signatureLineNumber = lineNumber ?: -1,
                firstExecutableLineNumber = firstExecutableLineNumber,
                lastLineNumber = lastLineNumber ?: -1,
                bodyStartLine = lineNumber?.plus(1),
                bodyEndLine = lastLineNumber
            )
        )
    }
    
    private fun analyzePhpField(field: Field): FieldNode {
        val modifiers = mutableListOf<String>()
        val modifier = field.modifier
        if (modifier.isPublic) modifiers.add("public")
        if (modifier.isPrivate) modifiers.add("private")
        if (modifier.isProtected) modifiers.add("protected")
        if (modifier.isStatic) modifiers.add("static")
        
        return FieldNode(
            name = field.name,
            type = field.type.toString(),
            modifiers = modifiers,
            annotations = emptyList(),
            javadoc = field.docComment?.text,
            initialValue = field.defaultValue?.text,
            isStatic = modifier.isStatic,
            isFinal = false // PHP doesn't have final fields in the same way
        )
    }
    
    private fun getFirstExecutableLineNumber(method: Method, document: Document?): Int? {
        if (document == null) return null
        
        // Find the first statement in the method body
        val statements = PsiTreeUtil.findChildrenOfType(method, Statement::class.java)
        val firstStatement = statements.firstOrNull { statement ->
            !isNonExecutableStatement(statement)
        }
        
        return firstStatement?.let { 
            document.getLineNumber(it.textOffset) + 1 
        }
    }
    
    private fun isNonExecutableStatement(statement: Statement): Boolean {
        return when (statement) {
            is PhpDocComment -> true
            else -> statement.text.trim().startsWith("//") || 
                   statement.text.trim().startsWith("/*") ||
                   statement.text.trim().isEmpty()
        }
    }
    
    private fun calculateMethodComplexity(method: Method): Int {
        var complexity = 1 // Base complexity
        
        // Count decision points
        PsiTreeUtil.processElements(method) { element ->
            when (element) {
                is If -> complexity++
                is ElseIf -> complexity++
                is For -> complexity++
                is Foreach -> complexity++
                is While -> complexity++
                is Switch -> complexity++
                is Catch -> complexity++
                is TernaryExpression -> complexity++
                // Add more PHP-specific control structures as needed
            }
            true
        }
        
        return complexity
    }
    
    private fun analyzeMethodBody(method: Method): List<StatementNode>? {
        val statements = mutableListOf<StatementNode>()
        
        PsiTreeUtil.processElements(method) { element ->
            when (element) {
                is Statement -> {
                    statements.add(StatementNode(
                        type = getStatementType(element),
                        text = element.text.take(100), // Limit text length
                        lineNumber = PsiDocumentManager.getInstance(element.project)
                            .getDocument(element.containingFile)
                            ?.getLineNumber(element.textOffset)?.plus(1)
                    ))
                }
            }
            true
        }
        
        return statements
    }
    
    private fun getStatementType(statement: Statement): String {
        return when (statement) {
            is If -> "IF_STATEMENT"
            is For -> "FOR_LOOP"
            is Foreach -> "FOREACH_LOOP"
            is While -> "WHILE_LOOP"
            is Switch -> "SWITCH_STATEMENT"
            is Return -> "RETURN"
            is AssignmentExpression -> "ASSIGNMENT"
            is MethodReference -> "METHOD_CALL"
            is PhpEchoStatement -> "ECHO"
            else -> "STATEMENT"
        }
    }
    
    override fun getMethodDetails(
        project: Project,
        psiFile: PsiFile,
        methodName: String?
    ): List<MethodDetails> {
        if (psiFile !is PhpFile) return emptyList()
        
        val details = mutableListOf<MethodDetails>()
        val phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass::class.java)
        
        for (phpClass in phpClasses) {
            val methods = if (methodName != null) {
                phpClass.ownMethods.filter { it.name == methodName }
            } else {
                phpClass.ownMethods.toList()
            }
            
            for (method in methods) {
                val methodNode = analyzePhpMethod(method, false)
                details.add(MethodDetails(
                    name = method.name,
                    className = phpClass.name ?: "",
                    file = psiFile.name,
                    lineRange = methodNode.bodyLineRange ?: MethodLineRange(-1, null, -1, null, null),
                    complexity = ComplexityMetric(
                        methodName = method.name,
                        cyclomaticComplexity = methodNode.complexity ?: 1,
                        linesOfCode = method.textLength / 50, // Rough estimate
                        parameterCount = method.parameters.size
                    ),
                    parameters = methodNode.parameters,
                    breakpointSuggestions = suggestBreakpointLinesForMethod(project, method)
                ))
            }
        }
        
        return details
    }
    
    override fun suggestBreakpointLines(
        project: Project,
        psiFile: PsiFile,
        methodName: String?
    ): List<BreakpointSuggestion> {
        if (psiFile !is PhpFile) return emptyList()
        
        val suggestions = mutableListOf<BreakpointSuggestion>()
        val phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass::class.java)
        
        for (phpClass in phpClasses) {
            val methods = if (methodName != null) {
                phpClass.ownMethods.filter { it.name == methodName }
            } else {
                phpClass.ownMethods.toList()
            }
            
            for (method in methods) {
                suggestions.addAll(suggestBreakpointLinesForMethod(project, method))
            }
        }
        
        return suggestions
    }
    
    private fun suggestBreakpointLinesForMethod(project: Project, method: Method): List<BreakpointSuggestion> {
        val suggestions = mutableListOf<BreakpointSuggestion>()
        val document = PsiDocumentManager.getInstance(project).getDocument(method.containingFile)
            ?: return suggestions
        
        // First executable line
        val firstExecutableLineNumber = getFirstExecutableLineNumber(method, document)
        if (firstExecutableLineNumber != null) {
            suggestions.add(BreakpointSuggestion(
                lineNumber = firstExecutableLineNumber,
                reason = "FIRST_EXECUTABLE",
                description = "First executable line in ${method.name}",
                priority = "HIGH",
                methodName = method.name
            ))
        }
        
        // Decision points
        PsiTreeUtil.processElements(method) { element ->
            val lineNumber = document.getLineNumber(element.textOffset) + 1
            when (element) {
                is If -> suggestions.add(BreakpointSuggestion(
                    lineNumber = lineNumber,
                    reason = "DECISION_POINT",
                    description = "If statement condition",
                    priority = "MEDIUM",
                    methodName = method.name
                ))
                is For, is Foreach, is While -> suggestions.add(BreakpointSuggestion(
                    lineNumber = lineNumber,
                    reason = "LOOP_ENTRY",
                    description = "Loop entry point",
                    priority = "MEDIUM",
                    methodName = method.name
                ))
                is Return -> suggestions.add(BreakpointSuggestion(
                    lineNumber = lineNumber,
                    reason = "METHOD_EXIT",
                    description = "Return statement",
                    priority = "LOW",
                    methodName = method.name
                ))
            }
            true
        }
        
        return suggestions
    }
    
    override fun getMethodComplexity(
        project: Project,
        psiFile: PsiFile,
        methodName: String?
    ): List<ComplexityMetric> {
        if (psiFile !is PhpFile) return emptyList()
        
        val metrics = mutableListOf<ComplexityMetric>()
        val phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass::class.java)
        
        for (phpClass in phpClasses) {
            val methods = if (methodName != null) {
                phpClass.ownMethods.filter { it.name == methodName }
            } else {
                phpClass.ownMethods.toList()
            }
            
            for (method in methods) {
                metrics.add(ComplexityMetric(
                    methodName = method.name,
                    cyclomaticComplexity = calculateMethodComplexity(method),
                    linesOfCode = method.textLength / 50, // Rough estimate
                    parameterCount = method.parameters.size
                ))
            }
        }
        
        return metrics
    }
    
    override fun detectCodePatterns(
        project: Project,
        psiFile: PsiFile,
        patterns: List<String>
    ): List<CodePattern> {
        if (psiFile !is PhpFile) return emptyList()
        
        val detectedPatterns = mutableListOf<CodePattern>()
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: return detectedPatterns
        
        // Detect null checks
        if (patterns.contains("null_checks") || patterns.contains("all")) {
            PsiTreeUtil.processElements(psiFile) { element ->
                if (element.text.contains("=== null") || element.text.contains("== null") ||
                    element.text.contains("is_null(")) {
                    detectedPatterns.add(CodePattern(
                        type = "NULL_CHECK",
                        description = "Null check detected",
                        lineNumber = document.getLineNumber(element.textOffset) + 1,
                        suggestion = "Consider using null coalescing operator (??) for safer null handling"
                    ))
                }
                true
            }
        }
        
        // Detect exception handling
        if (patterns.contains("exception_handling") || patterns.contains("all")) {
            val tryStatements = PsiTreeUtil.findChildrenOfType(psiFile, Try::class.java)
            for (tryStatement in tryStatements) {
                detectedPatterns.add(CodePattern(
                    type = "EXCEPTION_HANDLING",
                    description = "Try-catch block found",
                    lineNumber = document.getLineNumber(tryStatement.textOffset) + 1,
                    suggestion = "Ensure specific exception types are caught rather than generic Exception"
                ))
            }
        }
        
        // Detect loops
        if (patterns.contains("loops") || patterns.contains("all")) {
            val loops = PsiTreeUtil.findChildrenOfType(psiFile, For::class.java) +
                       PsiTreeUtil.findChildrenOfType(psiFile, Foreach::class.java) +
                       PsiTreeUtil.findChildrenOfType(psiFile, While::class.java)
            
            for (loop in loops) {
                detectedPatterns.add(CodePattern(
                    type = "LOOP",
                    description = "${loop.javaClass.simpleName} loop detected",
                    lineNumber = document.getLineNumber(loop.textOffset) + 1,
                    suggestion = "Consider using built-in array functions for better performance"
                ))
            }
        }
        
        return detectedPatterns
    }
    
    override fun getClassHierarchy(
        project: Project,
        className: String,
        direction: String
    ): HierarchyNode? {
        val phpIndex = PhpIndex.getInstance(project)
        val classes = phpIndex.getClassesByName(className)
        
        if (classes.isEmpty()) return null
        
        val phpClass = classes.first()
        
        return HierarchyNode(
            name = phpClass.name,
            fullyQualifiedName = phpClass.fqn.toString(),
            type = "CLASS",
            superClasses = if (direction == "up" || direction == "both") {
                getSuperClasses(phpClass)
            } else emptyList(),
            subClasses = if (direction == "down" || direction == "both") {
                getSubClasses(phpClass, project)
            } else emptyList(),
            interfaces = phpClass.implementedInterfaces.map { 
                HierarchyNode(
                    name = it.name,
                    fullyQualifiedName = it.fqn.toString(),
                    type = "INTERFACE"
                )
            }
        )
    }
    
    private fun getSuperClasses(phpClass: PhpClass): List<HierarchyNode> {
        val superClasses = mutableListOf<HierarchyNode>()
        var currentClass = phpClass.superClass
        
        while (currentClass != null) {
            superClasses.add(HierarchyNode(
                name = currentClass.name,
                fullyQualifiedName = currentClass.fqn.toString(),
                type = "CLASS"
            ))
            currentClass = currentClass.superClass
        }
        
        return superClasses
    }
    
    private fun getSubClasses(phpClass: PhpClass, project: Project): List<HierarchyNode> {
        val phpIndex = PhpIndex.getInstance(project)
        val subClasses = phpIndex.getAllSubclasses(phpClass.fqn.toString())
        
        return subClasses.map { subClass ->
            HierarchyNode(
                name = subClass.name,
                fullyQualifiedName = subClass.fqn.toString(),
                type = "CLASS"
            )
        }
    }
}
