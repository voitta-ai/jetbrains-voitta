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

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IU", "2024.3") // Ultimate Edition includes PHP support
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        plugin("com.intellij.mcpServer", "1.0.30")

        // Add necessary plugin dependencies for compilation here
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.jetbrains.php") // Available in Ultimate Edition
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
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    
    // Default runIde task (IntelliJ IDEA Community)
    runIde {
        // Uses default IC configuration from dependencies
    }
    
    // Explicit IntelliJ IDEA task
    register<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runIntelliJ") {
        group = "intellij platform"
        description = "Run IntelliJ IDEA with the plugin"
        // Uses same configuration as runIde
    }
    
    // PhpStorm-specific task
    register<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runPhpStorm") {
        group = "intellij platform" 
        description = "Run PhpStorm with the plugin"
        
        // For PhpStorm, we'll use the same IntelliJ Community base but with PHP plugin
        // This ensures PHP plugin is available in the IDE
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
