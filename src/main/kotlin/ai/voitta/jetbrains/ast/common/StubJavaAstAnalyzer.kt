package ai.voitta.jetbrains.ast.common

import ai.voitta.jetbrains.ast.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Stub implementation of JavaAstAnalyzer for platforms without Java support.
 * This allows the plugin to compile and run on PhpStorm while gracefully
 * handling the absence of Java PSI classes.
 */
class JavaAstAnalyzer : LanguageAstAnalyzer() {
    
    override val languageName: String = "Java"
    
    override fun canAnalyze(psiFile: PsiFile): Boolean = false
    
    override fun analyzeFile(
        project: Project,
        psiFile: PsiFile,
        includeMethodBodies: Boolean,
        includePrivateMembers: Boolean
    ): List<ClassNode> {
        throw UnsupportedOperationException("Java analysis not supported on this platform")
    }
    
    override fun getMethodDetails(
        project: Project,
        psiFile: PsiFile,
        methodName: String?
    ): List<MethodDetails> {
        throw UnsupportedOperationException("Java analysis not supported on this platform")
    }
    
    override fun suggestBreakpointLines(
        project: Project,
        psiFile: PsiFile,
        methodName: String?
    ): List<BreakpointSuggestion> {
        throw UnsupportedOperationException("Java analysis not supported on this platform")
    }
    
    override fun getMethodComplexity(
        project: Project,
        psiFile: PsiFile,
        methodName: String?
    ): List<ComplexityMetric> {
        throw UnsupportedOperationException("Java analysis not supported on this platform")
    }
    
    override fun detectCodePatterns(
        project: Project,
        psiFile: PsiFile,
        patterns: List<String>
    ): List<CodePattern> {
        throw UnsupportedOperationException("Java analysis not supported on this platform")
    }
    
    override fun getClassHierarchy(
        project: Project,
        className: String,
        direction: String
    ): HierarchyNode? {
        throw UnsupportedOperationException("Java analysis not supported on this platform")
    }
}