# Bug Analysis Report from runIde.log

This report summarizes the critical errors and warnings found in the `runIde.log` file, along with their causes and recommended solutions.

---

### 1. `IllegalArgumentException`: Invalid Java Version in Gradle Compatibility Check

-   **Symptom**: An `IllegalArgumentException: 25` is thrown from `com.intellij.util.lang.JavaVersion.parse`.
-   **Cause**: The Gradle plugin's compatibility check (`GradleJvmSupportMatrix`) is attempting to parse a Java version string but receives an unexpected value "25". This likely occurs when the plugin encounters a Java version it doesn't recognize, possibly from a misconfigured build environment or an unsupported JDK.
-   **Solution**:
    1.  **Verify Gradle and Java Versions**: Check the `gradle.properties` and `build.gradle.kts` files to ensure that the specified Java versions (for the project, toolchain, and Gradle daemon) are standard and correctly formatted (e.g., 17, 21).
    2.  **Check System Java**: Ensure the system's default `JAVA_HOME` points to a standard JDK installation.
    3.  **Plugin Bug**: If the configurations are correct, this may be a bug in the IntelliJ Gradle plugin's handling of newer or custom Java runtimes. The temporary fix would be to align all Java versions to a more common LTS release like 17 or 21.

---

### 2. `ClassNotFoundException`: Missing Extension Classes

-   **Symptom**: Multiple `PluginException` errors indicating that extension classes cannot be created, caused by `ClassNotFoundException`.
    -   `ai.voitta.jetbrains.MyCustomTool`
    -   `ai.voitta.jetbrains.ast.common.GetSymbolAtPositionTool`
    -   `ai.voitta.jetbrains.ast.common.FindAllReferencesTool`
-   **Cause**: These classes are registered as extensions in the `plugin.xml` file, but they do not exist in the compiled source code of the plugin. This typically happens when source files are deleted or renamed, but the corresponding declarations in `plugin.xml` are not updated.

-   **Historical Context**: 
    -   **GetSymbolAtPositionTool and FindAllReferencesTool**: These tools were fully implemented and functional as of commit `7955f33` (July 27, 2025 at 14:24:29). They were located in `src/main/kotlin/org/jetbrains/mcpextensiondemo/ast/common/NavigationTools.kt` and provided:
        - `GetSymbolAtPositionTool`: Symbol analysis at specific file positions with support for classes, methods, fields, and variables
        - `FindAllReferencesTool`: Reference finding across the project with usage type detection
    -   **Deletion**: Both tools were deleted in commit `a6018a6` (July 27, 2025 at 17:45:35) when the entire `NavigationTools.kt` file was removed, but the `plugin.xml` references were not updated accordingly.
    -   **Package Structure**: The original implementations used the package `org.jetbrains.mcpextensiondemo.ast.common` but the current `plugin.xml` references `ai.voitta.jetbrains.ast.common`, indicating a package restructuring occurred.

-   **Solution**:
    1.  **Audit `plugin.xml`**: Open `src/main/resources/META-INF/plugin.xml`.
    2.  **Remove Obsolete Extensions**: Search for the missing class names listed above within `<extension>` tags.
    3.  **Delete the corresponding `<extension>` blocks entirely.** For example, if you find `<myExtension implementation="ai.voitta.jetbrains.MyCustomTool"/>`, delete that entire line. This will stop the IDE from trying to load classes that no longer exist.
    4.  **Consider Re-implementing**: If the navigation tools functionality is still needed, the original implementations can be restored from commit `7955f33` and adapted to the current package structure (`ai.voitta.jetbrains.ast.common`).

---

### 3. Concurrency Error: Read Access Violation from Background Thread

-   **Symptom**: Numerous `RuntimeExceptionWithAttachments` with the message: `Read access is allowed from inside read-action only`. The stack traces point to code in `ai.voitta.jetbrains.ast.GetJavaFileAstTool` and `ai.voitta.jetbrains.ast.AstUtils`.
-   **Cause**: The IntelliJ Platform has strict threading rules. Any code that reads from the project's model or PSI (Program Structure Interface) tree must be executed within a "read action." The errors show that PSI access methods (like `PsiManager.findFile`, `psiFile.getClasses`, etc.) are being called from a background thread (`Netty Builtin Server`) without acquiring the necessary read lock. This is a common and critical bug in IntelliJ plugin development that can lead to data corruption or freezes.
-   **Solution**:
    1.  **Identify PSI Access Points**: Locate all code in the `ai.voitta.jetbrains.ast` package (specifically in `JavaAstTools.kt` and `AstUtils.kt`) that interacts with IntelliJ's PSI or VFS APIs.
    2.  **Wrap in `runReadAction`**: Wrap every block of code that accesses the PSI tree with `ApplicationManager.getApplication().runReadAction { ... }`. This ensures the code is executed safely with a read lock.

    **Example Fix in `JavaAstTools.kt`:**

    ```kotlin
    // import com.intellij.openapi.application.ApplicationManager

    fun handle(...) {
        // ...
        val psiFile = ApplicationManager.getApplication().runReadAction<PsiFile?> {
            PsiManager.getInstance(project).findFile(virtualFile)
        }
        // ...
    }

    fun analyzeJavaFile(...) {
        // ...
        val classes = ApplicationManager.getApplication().runReadAction<Array<PsiClass>> {
            psiFile.classes
        }
        // ...
    }
    ```
    This pattern must be applied to **all direct and indirect PSI interactions** originating from the background thread to resolve the threading violations.
