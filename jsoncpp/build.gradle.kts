import com.android.ndkports.AndroidExecutableTestTask
import com.android.ndkports.CMakeCompatibleVersion
import com.android.ndkports.MesonPortTask
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import java.io.File
import java.net.URL
import java.security.MessageDigest

// Get project-specific configuration
val configs = rootProject.extra["projectConfigs"] as Map<String, Map<String, String>>
val projectConfig = configs[project.name] ?: error("No configuration found for project ${project.name}")

val libVersion = projectConfig["libVersion"]!!
val libName = projectConfig["libName"]!!

val portVersion = libVersion

val jsoncppDownloadUrl = "https://github.com/open-source-parsers/jsoncpp/archive/refs/tags/${portVersion}.tar.gz"

group = "com.github.bubbletrouble14"
version = "$portVersion"

plugins {
    id("maven-publish")
    id("com.android.ndkports.NdkPorts")
    id("signing")
    distribution
}

ndkPorts {
    ndkPath.set(File(project.findProperty("ndkPath") as String))
    source.set(project.file("src.tar.gz"))
    minSdkVersion.set(16)
}

// Task to download JsonCpp source
tasks.register<DefaultTask>("downloadJsonCpp") {
    val outputFile = project.file("src.tar.gz")
    outputs.file(outputFile)

    doLast {
        // Download source
        URL(jsoncppDownloadUrl).openStream().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Set the source property after download
        ndkPorts.source.set(outputFile)
    }
}

tasks.extractSrc {
    dependsOn("downloadJsonCpp")

    doLast {
        // jsoncpp has a "version" file on the include path that conflicts with
        // https://en.cppreference.com/w/cpp/header/version. Remove it so we can
        // build.
        outDir.get().asFile.resolve("version").delete()
    }
}

val buildTask = tasks.register<MesonPortTask>("buildPort")

tasks.prefabPackage {
    version.set(CMakeCompatibleVersion.parse(portVersion))

    modules {
        create("jsoncpp")
    }
}

tasks.register<AndroidExecutableTestTask>("test") {
    push {
        push(
            buildTask.get().buildDirectoryFor(abi).resolve("jsoncpp_test"),
            File("jsoncpp_test")
        )
        push(
            buildTask.get().installDirectoryFor(abi)
                .resolve("lib/libjsoncpp.so"), File("libjsoncpp.so")
        )
        push(
            toolchain.sysrootLibs.resolve("libc++_shared.so"),
            File("libc++_shared.so")
        )
    }

    run {
        shellTest(
            "jsoncpp_test", listOf(
                "LD_LIBRARY_PATH=$deviceDirectory",
                deviceDirectory.resolve("jsoncpp_test").toString()
            )
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["prefab"])
            pom {
                name.set("JsonCpp")
                description.set("The ndkports AAR for JsonCpp.")
                url.set(
                    "https://android.googlesource.com/platform/tools/ndkports"
                )
                licenses {
                    license {
                        name.set("The JsonCpp License")
                        url.set("https://github.com/open-source-parsers/jsoncpp/blob/master/LICENSE")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("The Android Open Source Project")
                    }
                }
                scm {
                    url.set("https://android.googlesource.com/platform/tools/ndkports")
                    connection.set("scm:git:https://android.googlesource.com/platform/tools/ndkports")
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("${project.buildDir}/repository")
        }
    }
}

// Configure signing
signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}

distributions {
    main {
        contents {
            from("${project.buildDir}/repository")
            include("**/*")
        }
    }
}

tasks {
    distZip {
        dependsOn("publish")
        destinationDirectory.set(File(rootProject.buildDir, "distributions"))
    }
}