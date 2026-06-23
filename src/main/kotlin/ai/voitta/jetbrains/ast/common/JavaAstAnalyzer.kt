package ai.voitta.jetbrains.ast.common

import ai.voitta.jetbrains.ast.*
import ai.voitta.jetbrains.ast.common.LanguageAstAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.JavaPsiFacade
import java.util.*

/**
 * Java-specific AST analyzer using IntelliJ's Java PSI
 */
class JavaAstAnalyzer : LanguageAstAnalyzer() {
    
    override val languageName = "Java"
    
    override fun canAnalyze(psiFile: PsiFile): Boolean {
        return psiFile is PsiJavaFile
    }
    
    override fun analyzeFile(
        project: Project,
        psiFile: PsiFile,
        includeMethodBodies: Boolean,
        includePrivateMembers: Boolean
    ): List<ClassNode> {
        if (psiFile !is PsiJavaFile) {
            throw IllegalArgumentException("File is not a Java file")
        }
        
        return psiFile.classes.map { psiClass ->
            analyzeJavaClass(psiClass, includeMethodBodies, includePrivateMembers)
        }
    }
    
    private fun analyzeJavaClass(
        psiClass: PsiClass, 
        includeMethodBodies: Boolean,
        includePrivateMembers: Boolean
    ): ClassNode {
        val methods = mutableListOf<MethodNode>()
        val fields = mutableListOf<FieldNode>()
        val innerClasses = mutableListOf<ClassNode>()
        
        // Analyze methods
        for (method in psiClass.methods) {
            if (!includePrivateMembers && method.modifierList?.hasModifierProperty(PsiModifier.PRIVATE) == true) continue
            
            methods.add(analyzeJavaMethod(method, includeMethodBodies))
        }
        
        // Analyze fields
        for (field in psiClass.fields) {
            if (!includePrivateMembers && field.modifierList?.hasModifierProperty(PsiModifier.PRIVATE) == true) continue
            
            fields.add(analyzeJavaField(field))
        }
        
        // Analyze inner classes
        for (innerClass in psiClass.innerClasses) {
            if (!includePrivateMembers && innerClass.modifierList?.hasModifierProperty(PsiModifier.PRIVATE) == true) continue
            
            innerClasses.add(analyzeJavaClass(innerClass, includeMethodBodies, includePrivateMembers))
        }
        
        // Get superclass and interfaces
        val superClass = psiClass.superClass?.qualifiedName
        val interfaces = psiClass.implementsList?.referencedTypes?.map { it.canonicalText } ?: emptyList()
        
        // Get modifiers
        val modifiers = extractModifiers(psiClass.modifierList)
        
        // Get annotations
        val annotations = psiClass.annotations.map { analyzeAnnotation(it) }
        
        return ClassNode(
            name = psiClass.name,
            fullyQualifiedName = psiClass.qualifiedName,
            superClass = superClass,
            interfaces = interfaces,
            modifiers = modifiers,
            methods = methods,
            fields = fields,
            innerClasses = innerClasses,
            annotations = annotations,
            javadoc = psiClass.docComment?.text,
            packageName = (psiClass.containingFile as? PsiJavaFile)?.packageName,
            isInterface = psiClass.isInterface,
            isEnum = psiClass.isEnum,
            isAbstract = psiClass.modifierList?.hasModifierProperty(PsiModifier.ABSTRACT) == true
        )
    }
    
    private fun analyzeJavaMethod(method: PsiMethod, includeMethodBodies: Boolean): MethodNode {
        val parameters = method.parameterList.parameters.map { param ->
            ParameterNode(
                name = param.name,
                type = param.type.canonicalText,
                isVarArgs = param.isVarArgs,
                annotations = param.annotations.map { analyzeAnnotation(it) }
            )
        }
        
        val modifiers = extractModifiers(method.modifierList)
        val annotations = method.annotations.map { analyzeAnnotation(it) }
        
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
        
        // Get thrown exceptions
        val throwsExceptions = method.throwsList.referencedTypes.map { it.canonicalText }
        
        return MethodNode(
            name = method.name,
            returnType = method.returnType?.canonicalText,
            parameters = parameters,
            modifiers = modifiers,
            annotations = annotations,
            javadoc = method.docComment?.text,
            bodyStatements = bodyStatements,
            complexity = complexity,
            isConstructor = method.isConstructor,
            isStatic = method.modifierList?.hasModifierProperty(PsiModifier.STATIC) == true,
            isAbstract = method.modifierList?.hasModifierProperty(PsiModifier.ABSTRACT) == true,
            throwsExceptions = throwsExceptions,
            lineNumber = lineNumber,
            firstExecutableLineNumber = firstExecutableLineNumber,
            lastLineNumber = lastLineNumber,
            bodyLineRange = MethodLineRange(
                signatureLineNumber = lineNumber ?: -1,
                firstExecutableLineNumber = firstExecutableLineNumber,
                lastLineNumber = lastLineNumber ?: -1,
                lastExecutableLineNumber = lastLineNumber,
                bodyStartLine = lineNumber?.plus(1),
                bodyEndLine = lastLineNumber
            )
        )
    }
    
    private fun analyzeJavaField(field: PsiField): FieldNode {
        val modifiers = extractModifiers(field.modifierList)
        val annotations = field.annotations.map { analyzeAnnotation(it) }
        
        return FieldNode(
            name = field.name,
            type = field.type.canonicalText,
            modifiers = modifiers,
            annotations = annotations,
            initializer = field.initializer?.text,
            isStatic = field.modifierList?.hasModifierProperty(PsiModifier.STATIC) == true,
            isFinal = field.modifierList?.hasModifierProperty(PsiModifier.FINAL) == true,
            lineNumber = getLineNumber(field)
        )
    }
    
    private fun analyzeAnnotation(annotation: PsiAnnotation): AnnotationNode {
        val attributes = mutableMapOf<String, String>()
        
        annotation.parameterList.attributes.forEach { attr ->
            val name = attr.name ?: "value"
            val value = attr.value?.text ?: ""
            attributes[name] = value
        }
        
        return AnnotationNode(
            name = annotation.nameReferenceElement?.referenceName ?: "",
            fullyQualifiedName = annotation.qualifiedName,
            parameters = attributes
        )
    }
    
    private fun extractModifiers(modifierList: PsiModifierList?): List<String> {
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
    
    private fun getFirstExecutableLineNumber(method: PsiMethod, document: Document?): Int? {
        if (document == null) return null
        
        val body = method.body ?: return null
        val statements = body.statements
        
        val firstStatement = statements.firstOrNull { statement ->
            !isNonExecutableStatement(statement)
        }
        
        return firstStatement?.let { 
            document.getLineNumber(it.textOffset) + 1 
        }
    }
    
    private fun isNonExecutableStatement(statement: PsiStatement): Boolean {
        return when (statement) {
            is PsiEmptyStatement -> true
            is PsiDeclarationStatement -> {
                // Variable declarations without initialization are not executable
                val declaredElements = statement.declaredElements
                declaredElements.all { element ->
                    (element as? PsiVariable)?.initializer == null
                }
            }
            else -> statement.text.trim().startsWith("//") || 
                   statement.text.trim().startsWith("/*") ||
                   statement.text.trim().isEmpty()
        }
    }
    
    private fun calculateMethodComplexity(method: PsiMethod): Int {
        var complexity = 1 // Base complexity
        
        // Count decision points
        PsiTreeUtil.processElements(method) { element ->
            when (element) {
                is PsiIfStatement -> complexity++
                is PsiSwitchStatement -> complexity++
                is PsiForStatement -> complexity++
                is PsiWhileStatement -> complexity++
                is PsiDoWhileStatement -> complexity++
                is PsiForeachStatement -> complexity++
                is PsiTryStatement -> complexity++ // Each catch block adds complexity
                is PsiConditionalExpression -> complexity++
                is PsiLambdaExpression -> complexity++
            }
            true
        }
        
        // Add complexity for each catch block
        PsiTreeUtil.findChildrenOfType(method, PsiTryStatement::class.java).forEach { tryStatement ->
            complexity += tryStatement.catchSections.size
        }
        
        return complexity
    }
    
    private fun analyzeMethodBody(method: PsiMethod): List<StatementNode>? {
        val body = method.body ?: return null
        val statements = mutableListOf<StatementNode>()
        
        PsiTreeUtil.processElements(body) { element ->
            when (element) {
                is PsiStatement -> {
                    statements.add(StatementNode(
                        type = getStatementType(element),
                        text = element.text.take(100), // Limit text length
                        startOffset = element.textOffset,
                        endOffset = element.textOffset + element.textLength,
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
    
    private fun getStatementType(statement: PsiStatement): String {
        return when (statement) {
            is PsiIfStatement -> "IF_STATEMENT"
            is PsiForStatement -> "FOR_LOOP"
            is PsiForeachStatement -> "FOREACH_LOOP"
            is PsiWhileStatement -> "WHILE_LOOP"
            is PsiDoWhileStatement -> "DO_WHILE_LOOP"
            is PsiSwitchStatement -> "SWITCH_STATEMENT"
            is PsiTryStatement -> "TRY_STATEMENT"
            is PsiReturnStatement -> "RETURN"
            is PsiExpressionStatement -> {
                when (statement.expression) {
                    is PsiAssignmentExpression -> "ASSIGNMENT"
                    is PsiMethodCallExpression -> "METHOD_CALL"
                    else -> "EXPRESSION"
                }
            }
            is PsiDeclarationStatement -> "DECLARATION"
            else -> "STATEMENT"
        }
    }
    
    override fun getMethodDetails(
        project: Project,
        psiFile: PsiFile,
        methodName: String?
    ): List<MethodDetails> {
        if (psiFile !is PsiJavaFile) return emptyList()
        
        val details = mutableListOf<MethodDetails>()
        
        for (psiClass in psiFile.classes) {
            val methods = if (methodName != null) {
                psiClass.methods.filter { it.name == methodName }
            } else {
                psiClass.methods.toList()
            }
            
            for (method in methods) {
                val methodNode = analyzeJavaMethod(method, false)
                details.add(MethodDetails(
                    name = method.name,
                    className = psiClass.name ?: "",
                    file = psiFile.name,
                    lineRange = methodNode.bodyLineRange ?: MethodLineRange(-1, null, -1, null, null, null),
                    complexity = ComplexityMetric(
                        methodName = method.name,
                        className = psiClass.name ?: "",
                        cyclomaticComplexity = methodNode.complexity ?: 1,
                        linesOfCode = method.textLength / 50, // Rough estimate
                        parameterCount = method.parameterList.parametersCount,
                        file = psiFile.name,
                        lineNumber = getLineNumber(method) ?: -1
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
        if (psiFile !is PsiJavaFile) return emptyList()
        
        val suggestions = mutableListOf<BreakpointSuggestion>()
        
        for (psiClass in psiFile.classes) {
            val methods = if (methodName != null) {
                psiClass.methods.filter { it.name == methodName }
            } else {
                psiClass.methods.toList()
            }
            
            for (method in methods) {
                suggestions.addAll(suggestBreakpointLinesForMethod(project, method))
            }
        }
        
        return suggestions
    }
    
    private fun suggestBreakpointLinesForMethod(project: Project, method: PsiMethod): List<BreakpointSuggestion> {
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
        
        // Decision points and other significant statements
        PsiTreeUtil.processElements(method) { element ->
            val lineNumber = document.getLineNumber(element.textOffset) + 1
            when (element) {
                is PsiIfStatement -> suggestions.add(BreakpointSuggestion(
                    lineNumber = lineNumber,
                    reason = "DECISION_POINT",
                    description = "If statement condition",
                    priority = "MEDIUM",
                    methodName = method.name
                ))
                is PsiForStatement, is PsiForeachStatement, is PsiWhileStatement, is PsiDoWhileStatement -> 
                    suggestions.add(BreakpointSuggestion(
                        lineNumber = lineNumber,
                        reason = "LOOP_ENTRY",
                        description = "Loop entry point",
                        priority = "MEDIUM",
                        methodName = method.name
                    ))
                is PsiReturnStatement -> suggestions.add(BreakpointSuggestion(
                    lineNumber = lineNumber,
                    reason = "METHOD_EXIT",
                    description = "Return statement",
                    priority = "LOW",
                    methodName = method.name
                ))
                is PsiTryStatement -> suggestions.add(BreakpointSuggestion(
                    lineNumber = lineNumber,
                    reason = "EXCEPTION_HANDLING",
                    description = "Try block entry",
                    priority = "MEDIUM",
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
        if (psiFile !is PsiJavaFile) return emptyList()
        
        val metrics = mutableListOf<ComplexityMetric>()
        
        for (psiClass in psiFile.classes) {
            val methods = if (methodName != null) {
                psiClass.methods.filter { it.name == methodName }
            } else {
                psiClass.methods.toList()
            }
            
            for (method in methods) {
                metrics.add(ComplexityMetric(
                    methodName = method.name,
                    className = psiClass.name ?: "",
                    cyclomaticComplexity = calculateMethodComplexity(method),
                    linesOfCode = method.textLength / 50, // Rough estimate
                    parameterCount = method.parameterList.parametersCount,
                    file = psiFile.name,
                    lineNumber = getLineNumber(method) ?: -1
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
        if (psiFile !is PsiJavaFile) return emptyList()
        
        val detectedPatterns = mutableListOf<CodePattern>()
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: return detectedPatterns
        
        // Detect null checks
        if (patterns.contains("null_checks") || patterns.contains("all")) {
            PsiTreeUtil.processElements(psiFile) { element ->
                if (element.text.contains("== null") || element.text.contains("!= null") ||
                    element.text.contains("Objects.isNull") || element.text.contains("Objects.nonNull")) {
                    detectedPatterns.add(CodePattern(
                        patternType = "NULL_CHECK",
                        description = "Null check detected",
                        file = psiFile.name,
                        startLine = document.getLineNumber(element.textOffset) + 1,
                        endLine = document.getLineNumber(element.textOffset) + 1,
                        codeSnippet = element.text.take(100),
                        suggestion = "Consider using Optional for safer null handling"
                    ))
                }
                true
            }
        }
        
        // Detect exception handling
        if (patterns.contains("exception_handling") || patterns.contains("all")) {
            val tryStatements = PsiTreeUtil.findChildrenOfType(psiFile, PsiTryStatement::class.java)
            for (tryStatement in tryStatements) {
                detectedPatterns.add(CodePattern(
                    patternType = "EXCEPTION_HANDLING",
                    description = "Try-catch block found",
                    file = psiFile.name,
                    startLine = document.getLineNumber(tryStatement.textOffset) + 1,
                    endLine = document.getLineNumber(tryStatement.textOffset) + 1,
                    codeSnippet = tryStatement.text.take(100),
                    suggestion = "Ensure specific exception types are caught rather than generic Exception"
                ))
            }
        }
        
        // Detect loops
        if (patterns.contains("loops") || patterns.contains("all")) {
            val loops = PsiTreeUtil.findChildrenOfType(psiFile, PsiForStatement::class.java) +
                       PsiTreeUtil.findChildrenOfType(psiFile, PsiForeachStatement::class.java) +
                       PsiTreeUtil.findChildrenOfType(psiFile, PsiWhileStatement::class.java) +
                       PsiTreeUtil.findChildrenOfType(psiFile, PsiDoWhileStatement::class.java)
            
            for (loop in loops) {
                detectedPatterns.add(CodePattern(
                    patternType = "LOOP",
                    description = "${loop.javaClass.simpleName} detected",
                    file = psiFile.name,
                    startLine = document.getLineNumber(loop.textOffset) + 1,
                    endLine = document.getLineNumber(loop.textOffset) + 1,
                    codeSnippet = loop.text.take(100),
                    suggestion = "Consider using Streams API for functional operations"
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
        val scope = GlobalSearchScope.allScope(project)
        val psiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = psiFacade.findClass(className, scope) 
            ?: psiFacade.findClasses(className, scope).firstOrNull()
            ?: return null
        
        return HierarchyNode(
            className = psiClass.name ?: className,
            fullyQualifiedName = psiClass.qualifiedName ?: className,
            type = when {
                psiClass.isInterface -> "INTERFACE"
                psiClass.isEnum -> "ENUM"
                psiClass.isAnnotationType -> "ANNOTATION"
                else -> "CLASS"
            },
            file = psiClass.containingFile?.name,
            superClasses = if (direction == "up" || direction == "both") {
                getSuperClasses(psiClass)
            } else emptyList(),
            subClasses = if (direction == "down" || direction == "both") {
                getSubClasses(psiClass, scope)
            } else emptyList(),
            interfaces = psiClass.implementsList?.referencedTypes?.map { type ->
                HierarchyNode(
                    className = type.className ?: "",
                    fullyQualifiedName = type.canonicalText,
                    type = "INTERFACE",
                    file = null
                )
            } ?: emptyList()
        )
    }
    
    private fun getSuperClasses(psiClass: PsiClass): List<HierarchyNode> {
        val superClasses = mutableListOf<HierarchyNode>()
        var currentClass = psiClass.superClass
        
        while (currentClass != null) {
            superClasses.add(HierarchyNode(
                className = currentClass.name ?: "",
                fullyQualifiedName = currentClass.qualifiedName ?: "",
                type = when {
                    currentClass.isInterface -> "INTERFACE"
                    currentClass.isEnum -> "ENUM" 
                    currentClass.isAnnotationType -> "ANNOTATION"
                    else -> "CLASS"
                },
                file = currentClass.containingFile?.name
            ))
            currentClass = currentClass.superClass
        }
        
        return superClasses
    }
    
    private fun getSubClasses(psiClass: PsiClass, scope: GlobalSearchScope): List<HierarchyNode> {
        val subClasses = mutableListOf<HierarchyNode>()
        
        ClassInheritorsSearch.search(psiClass, scope, true).forEach { subClass ->
            subClasses.add(HierarchyNode(
                className = subClass.name ?: "",
                fullyQualifiedName = subClass.qualifiedName ?: "",
                type = when {
                    subClass.isInterface -> "INTERFACE"
                    subClass.isEnum -> "ENUM"
                    subClass.isAnnotationType -> "ANNOTATION"
                    else -> "CLASS"
                },
                file = subClass.containingFile?.name
            ))
        }
        
        return subClasses
    }
    
    private fun getLineNumber(element: PsiElement): Int? {
        val document = PsiDocumentManager.getInstance(element.project)
            .getDocument(element.containingFile) ?: return null
        return document.getLineNumber(element.textOffset) + 1
    }
}
