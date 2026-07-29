import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

group = "com.gg.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    // Kotlin 标准库
    compileOnly(kotlin("stdlib"))

    // Kotlin 协程（WorkshopEventBus 使用 Flow）
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // SPW Workshop API
    val localApiProject = rootProject.findProject(":api")
        ?.takeUnless { it == project }
    if (localApiProject != null) {
        compileOnly(localApiProject)
        kapt(localApiProject)
    } else {
        compileOnly("com.github.Moriafly:spw-workshop-api:0.2.0-dev02")
        kapt("com.github.Moriafly:spw-workshop-api:0.2.0-dev02")
    }
}

// 插件元数据配置
val pluginClass = "com.gg.example.AudioPipelineExamplePlugin"
val pluginId = "com.gg.example"
val pluginName = "AudioPipelineExample"
val pluginDescription = "SPW Workshop 音频管线示例插件"
val pluginVersion = "2.0.0"
val pluginProvider = "Zeshi Palace"
val pluginRepository = "https://github.com/Moriafly/spw-workshop-api/tree/main/example"

// 配置主 JAR 任务
tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Plugin-Class" to pluginClass,
            "Plugin-Id" to pluginId,
            "Plugin-Name" to pluginName,
            "Plugin-Description" to pluginDescription,
            "Plugin-Version" to pluginVersion,
            "Plugin-Provider" to pluginProvider,
            "Plugin-Has-Config" to "false",
            "Plugin-Open-Source-Url" to pluginRepository,
            "Plugin-Api-Min" to "2",
            "Plugin-Api-Max" to "2",
            "Plugin-Capabilities" to
                "audio.gain,audio.decoder.trusted,audio.processor.trusted",
        )
    }
}

// 创建插件分发包
tasks.register<Jar>("plugin") {
    destinationDirectory.set(
        file(System.getenv("APPDATA") + "/Salt Player for Windows/workshop/plugins/")
    )
    archiveFileName.set("$pluginName-$pluginVersion.zip")

    into("classes") {
        with(tasks.named<Jar>("jar").get())
    }
    dependsOn(configurations.runtimeClasspath)
    into("lib") {
        from({
            configurations.runtimeClasspath
                .get()
                .filter { it.name.endsWith("jar") }
        })
    }
    archiveExtension.set("zip")
}
