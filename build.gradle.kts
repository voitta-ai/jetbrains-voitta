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
                // PHP as external plugin for Ultimate (commenting out for now due to version issues)
                // plugin("com.jetbrains.php", "243.12818.47")
            }
            "phpstorm" -> {
                create("PS", "2024.3") // PhpStorm
                // Both Java and PHP are natively supported in PhpStorm
                // No need to explicitly add bundled plugins for PhpStorm
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
            - Built for ${ideaPlatform.capitalize()}
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
    
    // Default tasks for Ultimate Edition (default platform)
    runIde {
        description = "Run ${when (ideaPlatform) {
            "ultimate" -> "IntelliJ Ultimate"
            "phpstorm" -> "PhpStorm"
            "community" -> "IntelliJ Community"
            else -> "IntelliJ"
        }} with the plugin"
    }
    
    register("runIntelliJ") {
        group = "intellij platform"
        description = "Run IntelliJ Ultimate with the plugin"
        doFirst {
            project.setProperty("idea.platform", "ultimate")
        }
        finalizedBy("runIde")
    }
    
    // PhpStorm-specific tasks
    register("buildPhpStorm") {
        group = "phpstorm"
        description = "Build plugin for PhpStorm"
        dependsOn("build")
        doLast {
            project.setProperty("idea.platform", "phpstorm")
        }
    }
    
    register("buildPluginPhpStorm") {
        group = "phpstorm" 
        description = "Build plugin distribution for PhpStorm"
        dependsOn("buildPhpStorm")
    }
    
    register("runPhpStorm") {
        group = "phpstorm"
        description = "Run PhpStorm with the plugin"
        doFirst {
            project.setProperty("idea.platform", "phpstorm")
        }
        finalizedBy("runIde")
    }
    
    // Community Edition specific tasks
    register("buildCommunity") {
        group = "community"
        description = "Build plugin for Community Edition"
        dependsOn("build")
        doLast {
            project.setProperty("idea.platform", "community")
        }
    }
    
    register("buildPluginCommunity") {
        group = "community" 
        description = "Build plugin distribution for Community Edition"
        dependsOn("buildCommunity")
    }
    
    register("runCommunity") {
        group = "community"
        description = "Run IntelliJ Community Edition with the plugin"
        doFirst {
            project.setProperty("idea.platform", "community")
        }
        finalizedBy("runIde")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
