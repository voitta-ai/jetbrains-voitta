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
    
    // Specific IDE run tasks that properly delegate to runIde with platform set
    register("runIntelliJ") {
        group = "run"  
        description = "Run IntelliJ Ultimate with the plugin (port 63344)"
        doLast {
            // Use the configured runIde task with platform property
            exec {
                environment("GRADLE_OPTS", "-Didea.platform=ultimate")
                commandLine("./gradlew", "-Pidea.platform=ultimate", "runIde")
            }
        }
    }
    
    register("runPhpStorm") {
        group = "run"
        description = "Run PhpStorm with the plugin (port 63344)"
        doLast {
            // Use the configured runIde task with platform property
            exec {
                environment("GRADLE_OPTS", "-Didea.platform=phpstorm")
                commandLine("./gradlew", "-Pidea.platform=phpstorm", "runIde")
            }
        }
    }
    
    register("runCommunity") {
        group = "run"
        description = "Run IntelliJ Community Edition with the plugin (port 63344)"
        doLast {
            // Use the configured runIde task with platform property
            exec {
                environment("GRADLE_OPTS", "-Didea.platform=community")
                commandLine("./gradlew", "-Pidea.platform=community", "runIde")
            }
        }
    }
    
    // Build plugin distribution tasks
    register("buildPluginIntelliJ") {
        group = "build"
        description = "Build plugin distribution for IntelliJ Ultimate"
        doLast {
            exec {
                commandLine("./gradlew", "-Pidea.platform=ultimate", "buildPlugin")
            }
        }
    }
    
    register("buildPluginPhpStorm") {
        group = "build"
        description = "Build plugin distribution for PhpStorm"
        doLast {
            exec {
                commandLine("./gradlew", "-Pidea.platform=phpstorm", "buildPlugin")
            }
        }
    }
    
    register("buildPluginCommunity") {
        group = "build"
        description = "Build plugin distribution for Community Edition"
        doLast {
            exec {
                commandLine("./gradlew", "-Pidea.platform=community", "buildPlugin")
            }
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
                // Exclude Java-dependent files when building for PhpStorm
                if (ideaPlatform == "phpstorm") {
                    exclude("**/ai/voitta/jetbrains/ast/JavaAstTools.kt")
                    exclude("**/ai/voitta/jetbrains/ast/AstUtils.kt")
                    exclude("**/ai/voitta/jetbrains/ast/NavigationTools.kt")
                    exclude("**/ai/voitta/jetbrains/ast/common/JavaAstAnalyzer.kt")
                    exclude("**/ai/voitta/jetbrains/debug/**/*.kt")
                    // Keep universal tools and PHP tools
                } else {
                    // Exclude stub when building for IntelliJ
                    exclude("**/ai/voitta/jetbrains/ast/common/StubJavaAstAnalyzer.kt")
                }
            }
        }
    }
}
