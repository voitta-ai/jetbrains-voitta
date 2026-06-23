package ai.voitta.jetbrains.utils

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger

/**
 * Utility for detecting the current IDE platform and available features
 */
object PlatformUtils {
    private val LOG = Logger.getInstance(PlatformUtils::class.java)
    
    val isPhpStorm: Boolean by lazy {
        ApplicationInfo.getInstance().build.productCode == "PS"
    }
    
    val isIntelliJIdea: Boolean by lazy {
        val productCode = ApplicationInfo.getInstance().build.productCode
        productCode == "IU" || productCode == "IC"
    }
    
    val isUltimate: Boolean by lazy {
        ApplicationInfo.getInstance().build.productCode == "IU"
    }
    
    val isCommunity: Boolean by lazy {
        ApplicationInfo.getInstance().build.productCode == "IC"
    }
    
    val hasJavaSupport: Boolean by lazy {
        try {
            // Try to load a Java PSI class
            Class.forName("com.intellij.psi.PsiJavaFile")
            true
        } catch (e: ClassNotFoundException) {
            LOG.info("Java PSI support not available in current platform")
            false
        }
    }
    
    val hasPhpSupport: Boolean by lazy {
        try {
            // Try to load a PHP PSI class
            Class.forName("com.jetbrains.php.lang.psi.PhpFile")
            true
        } catch (e: ClassNotFoundException) {
            LOG.info("PHP PSI support not available in current platform")
            false
        }
    }
    
    fun getPlatformInfo(): String {
        val appInfo = ApplicationInfo.getInstance()
        return buildString {
            appendLine("Platform: ${appInfo.fullApplicationName}")
            appendLine("Product Code: ${appInfo.build.productCode}")
            appendLine("Version: ${appInfo.fullVersion}")
            appendLine("Java Support: $hasJavaSupport")
            appendLine("PHP Support: $hasPhpSupport")
        }
    }
}