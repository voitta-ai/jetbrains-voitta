plugins {
    id("java")
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij.platform") version "2.5.0"
    kotlin("plugin.serialization") version "1.9.24"
}

group = "ai.voitta"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Java toolchain to use specific JDK
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.ORACLE // or JvmVendorSpec.OPENJDK
    }
}

// Build configuration for IntelliJ Ultimate, PhpStorm, and Community Edition
val ideaPlatform = project.findProperty("idea.platform") as String? ?: "ultimate"

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        when (ideaPlatform) {
            "ultimate" -> {
                create("IU", "2024.3") // Ultimate Edition
                bundledPlugin("com.intellij.java") // Java support in Ultimate
                // PHP plugin for Ultimate Edition - commenting out for now due to version issues
                // plugin("com.jetbrains.php", "243.21565.197")
            }
            "phpstorm" -> {
                create("PS", "2024.3") // PhpStorm
                // PHP and Java are both bundled in PhpStorm - no explicit dependency needed
            }
            "community" -> {
                create("IC", "2024.3") // Community Edition
                bundledPlugin("com.intellij.java") // Java support in Community
                // PHP not available in Community Edition - Java-only support
            }
            else -> throw GradleException("Unsupported platform: $ideaPlatform. Use 'ultimate', 'phpstorm', or 'community'")
        }
        
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        plugin("com.intellij.mcpServer", "1.0.30")
    }
}

// this has to be compileOnly otherwise there is class collision for kotlinx serialization
// from the main plugin and the current one
dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
        }

        changeNotes = """
            JetbrainsVoitta - Enhanced MCP plugin with AST analysis and debugging tools:
            - Debug session inspection tools
            - Java and PHP AST analysis and complexity metrics
            - Code pattern detection for both languages
            - Symbol navigation and reference finding
            - Language-agnostic tool framework
            - Powered by Voitta AI
            - Built for ${ideaPlatform.replaceFirstChar { it.uppercase() }}
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    
    // Configure plugin distribution name based on platform
    val platformSuffix = when (ideaPlatform) {
        "ultimate" -> "Ultimate"
        "phpstorm" -> "PhpStorm"
        "community" -> "Community"
        else -> ""
    }
    
    // Configure archive name for different platforms
    withType<Jar> {
        archiveBaseName.set("JetbrainsVoitta-${platformSuffix}")
    }
    
    // Configure distribution archive name
    withType<Zip> {
        archiveBaseName.set("JetbrainsVoitta-${platformSuffix}")
    }
    
    // Configure the main runIde task with debug port
    runIde {
        // Force built-in server to use port 63344 for all sandbox IDEs
        jvmArguments.add("-Didea.builtin.server.port=63344")
        systemProperties["idea.builtin.server.port"] = "63344"
        systemProperties["log.level.all"] = "DEBUG"
        
        // Also add as environment variable for extra reliability
        environment("IDEA_BUILTIN_SERVER_PORT", "63344")
        
        // Log the configuration at runtime
        doFirst {
            println("🔌 Starting IDE with built-in server port: 63344")
            println("🏗️ Platform: ${project.findProperty("idea.platform") ?: "ultimate"}")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    
    sourceSets {
        main {
            kotlin {
                when (ideaPlatform) {
                    "community" -> {
                        // Community Edition: Java only, no PHP
                        exclude("**/ai/voitta/jetbrains/ast/php/**/*.kt")
                        exclude("**/ai/voitta/jetbrains/ast/common/StubJavaAstAnalyzer.kt")
                    }
                    "phpstorm" -> {
                        // PhpStorm: Both Java and PHP supported
                        exclude("**/ai/voitta/jetbrains/ast/common/StubJavaAstAnalyzer.kt")
                    }
                    "ultimate" -> {
                        // Ultimate: Java only for now (PHP plugin dependency issues)
                        exclude("**/ai/voitta/jetbrains/ast/php/**/*.kt")
                        exclude("**/ai/voitta/jetbrains/ast/common/StubJavaAstAnalyzer.kt")
                    }
                }
            }
        }
    }
}
