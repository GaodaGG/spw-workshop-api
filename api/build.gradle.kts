import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    `maven-publish`
}

group = "com.github.Moriafly"
version = "0.2.0-dev02"

val legacyDev20Api by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

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
    api(libs.pf4j)
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.salt.ui)
    testImplementation(libs.junit)
    legacyDev20Api("com.github.Moriafly:spw-workshop-api:0.1.0-dev20")
}

tasks.test {
    inputs.files(legacyDev20Api)
    doFirst {
        systemProperty(
            "spw.workshop.legacyDev20Jar",
            legacyDev20Api.singleFile.absolutePath
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "spw-workshop-api"
            from(components["java"])
        }
    }
}
