# IDE Configuration Options Analysis

## Option 1: Single Plugin with Multiple runIde Tasks (✅ Recommended)

### What We Have Now:
- Single plugin that works in both IntelliJ IDEA and PhpStorm
- Universal tools that auto-detect language (Java/PHP)
- Language-specific tools for fine-grained control

### Implementation:
```kotlin
// Current: IntelliJ Community Edition with PHP plugin
dependencies {
    intellijPlatform {
        create("IC", "2024.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.jetbrains.php")
    }
}

// Add custom tasks for different IDEs
tasks {
    register<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runIntelliJ") {
        type.set("IC")
        version.set("2024.3")
    }
    
    register<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runPhpStorm") {
        // Option A: Use local PhpStorm installation
        ideDir.set(File("/path/to/local/phpstorm/installation"))
        
        // Option B: Use PhpStorm type (if supported in future)
        // type.set("PS") 
        // version.set("2024.3")
    }
}
```

### Advantages:
- ✅ Single codebase to maintain
- ✅ Universal tools work everywhere
- ✅ Already 95% implemented
- ✅ Same debugging capabilities across IDEs
- ✅ Easy testing with ./gradlew runPhpStorm

### Test Commands:
```bash
./gradlew runIde          # Default IntelliJ IDEA
./gradlew runIntelliJ     # Explicit IntelliJ IDEA
./gradlew runPhpStorm     # PhpStorm with PHP plugin
```

### ✅ IMPLEMENTED
The gradle tasks have been successfully implemented in build.gradle.kts:
- `runIde`: Default task that runs IntelliJ IDEA Community Edition
- `runIntelliJ`: Explicit task for IntelliJ IDEA (same as runIde)
- `runPhpStorm`: Runs IntelliJ Community with PHP plugin support

All tasks use the same IntelliJ Community base (IC 2024.3) with both Java and PHP plugins bundled, ensuring compatibility across different development environments.

---

## Option 2: Separate Plugins (❌ Not Recommended)

### Implementation:
```
jetbrains-voitta-intellij/  # Java-focused plugin
jetbrains-voitta-phpstorm/  # PHP-focused plugin
shared-core/                # Common debugging tools
```

### Why Not Recommended:
- ❌ 90% code duplication
- ❌ Double maintenance burden  
- ❌ Debug tools identical anyway
- ❌ Complex release coordination
- ❌ User confusion (which plugin to install?)

---

## Option 3: Multi-Module Gradle Project (⚖️ Overkill for Our Case)

### Implementation:
```
root/
├── common/           # Shared debug + infrastructure
├── java-support/     # Java PSI analyzer
├── php-support/      # PHP PSI analyzer  
└── plugin/          # Main plugin assembly
```

### Why Overkill:
- ⚖️ Complex for current scope
- ⚖️ Our universal tools already solve the modularity
- ⚖️ Most complexity is in debug tools (shared anyway)

