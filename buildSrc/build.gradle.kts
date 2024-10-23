val kotlinVersion = "1.7.10"  // Match this with the Kotlin plugin version

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.android.ndkports"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

// Force all Kotlin dependencies to use the same version
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(kotlin("stdlib-jdk7", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3") {
        // Force Kotlin stdlib version for coroutines
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }

    implementation("com.google.prefab:api:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }
    implementation("org.redundent:kotlin-xml-builder:1.6.1")

    testImplementation(kotlin("test", kotlinVersion))
    testImplementation(kotlin("test-junit", kotlinVersion))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks {
    compileJava {
        @Suppress("UnstableApiUsage")
        options.release.set(8)
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            apiVersion = "1.7"
            languageVersion = "1.7"
            freeCompilerArgs = listOf("-Xskip-metadata-version-check")
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            apiVersion = "1.7"
            languageVersion = "1.7"
            freeCompilerArgs = listOf("-Xskip-metadata-version-check")
        }
    }
}

gradlePlugin {
    plugins {
        create("ndkports") {
            id = "com.android.ndkports.NdkPorts"
            implementationClass = "com.android.ndkports.NdkPortsPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("${rootProject.buildDir}/repository")
        }
    }
}