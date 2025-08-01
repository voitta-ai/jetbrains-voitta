package ai.voitta.jetbrains.ast.common

import ai.voitta.jetbrains.ast.*
import ai.voitta.jetbrains.utils.PlatformUtils
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Abstract base class for language-specific AST analysis.
 * Provides a common interface for analyzing different programming languages.
 */
abstract class LanguageAstAnalyzer {
    
    /**
     * Analyze a source file and return its AST representation
     */
    abstract fun analyzeFile(
        project: Project,
        psiFile: PsiFile, 
        includeMethodBodies: Boolean = false,
        includePrivateMembers: Boolean = true
    ): List<ClassNode>
    
    /**
     * Get detailed information about methods/functions in a file
     */
    abstract fun getMethodDetails(
        project: Project,
        psiFile: PsiFile,
        methodName: String? = null
    ): List<MethodDetails>
    
    /**
     * Suggest optimal breakpoint locations for methods/functions
     */
    abstract fun suggestBreakpointLines(
        project: Project,
        psiFile: PsiFile,
        methodName: String? = null
    ): List<BreakpointSuggestion>
    
    /**
     * Calculate complexity metrics for methods/functions
     */
    abstract fun getMethodComplexity(
        project: Project,
        psiFile: PsiFile,
        methodName: String? = null
    ): List<ComplexityMetric>
    
    /**
     * Detect common code patterns and potential issues
     */
    abstract fun detectCodePatterns(
        project: Project,
        psiFile: PsiFile,
        patterns: List<String>
    ): List<CodePattern>
    
    /**
     * Get class hierarchy information
     */
    abstract fun getClassHierarchy(
        project: Project,
        className: String,
        direction: String = "both"
    ): HierarchyNode?
    
    /**
     * Check if this analyzer can handle the given file type
     */
    abstract fun canAnalyze(psiFile: PsiFile): Boolean
    
    /**
     * Get the language name this analyzer handles
     */
    abstract val languageName: String
}

/**
 * Factory for creating language-specific analyzers
 */
object LanguageAnalyzerFactory {
    
    private val analyzers = mutableListOf<LanguageAstAnalyzer>()
    
    init {
        // Register analyzers based on platform capabilities
        if (PlatformUtils.hasJavaSupport) {
            try {
                registerAnalyzer(JavaAstAnalyzer())
            } catch (e: NoClassDefFoundError) {
                // Java analyzer not available in this build
            }
        }
        
        if (PlatformUtils.hasPhpSupport) {
            try {
                // registerAnalyzer(ai.voitta.jetbrains.ast.php.PhpAstAnalyzer())
                // TODO: PHP analyzer implementation pending - classes available but implementation incomplete
            } catch (e: NoClassDefFoundError) {
                // PHP analyzer not available in this build
            }
        }
    }
    
    fun registerAnalyzer(analyzer: LanguageAstAnalyzer) {
        analyzers.add(analyzer)
    }
    
    fun getAnalyzer(psiFile: PsiFile): LanguageAstAnalyzer? {
        return analyzers.firstOrNull { it.canAnalyze(psiFile) }
    }
    
    fun getAnalyzerByLanguage(languageName: String): LanguageAstAnalyzer? {
        return analyzers.firstOrNull { 
            it.languageName.equals(languageName, ignoreCase = true) 
        }
    }
    
    fun getSupportedLanguages(): List<String> {
        return analyzers.map { it.languageName }
    }
}
