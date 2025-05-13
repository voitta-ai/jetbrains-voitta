# Demo plugin

This demo plugins extends [JetBrains MCP Server Plugin](https://github.com/JetBrains/mcp-server-plugin) with custom command with arguments.

You can use it as a starting point to provide your own tools.

## Important parts:
In `build.gradle.kts`:

* Add correct dependency on kotlinx.serialization
```kotlin
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
```
This is needed to use the same version of serialization as in the main plugin.
* Add dependency on the main plugin 
```
dependencies {
  intellijPlatform {
    plugin("com.intellij.mcpServer", "1.0.30")
  }
}
```
* Make kotlinx.serialization compileOnly dependendency to avoid runtime errors due to different class loaders of the library
```kotlin
dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

In `plugin.xml`:
* Add the following lines to depend on the plugin and to provide a custom tool
```xml
<depends>com.intellij.mcpServer</depends>

<extensions defaultExtensionNs="com.intellij.mcpServer">
    <mcpTool implementation="org.jetbrains.mcpextensiondemo.MyCustomTool"/>
</extensions>
```

