import com.android.ndkports.AdHocPortTask
import com.android.ndkports.CMakeCompatibleVersion
import java.io.File
import java.net.URL
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

val portVersion = "3.4.0"
val opensslDownloadUrl = "https://github.com/openssl/openssl/releases/download/openssl-${portVersion}/openssl-${portVersion}.tar.gz"
val opensslSha256Url = "${opensslDownloadUrl}.sha256"
val opensslAscUrl = "${opensslDownloadUrl}.asc"

group = "com.android.ndk.thirdparty"
version = "$portVersion${rootProject.extra.get("snapshotSuffix")}"

plugins {
    id("maven-publish")
    id("com.android.ndkports.NdkPorts")
    distribution
}

ndkPorts {
    ndkPath.set(File(project.findProperty("ndkPath") as String))
    source.set(project.file("src.tar.gz"))
    minSdkVersion.set(21)
}

// Task to download OpenSSL source
tasks.register<DefaultTask>("downloadOpenSSL") {
    val outputFile = project.file("src.tar.gz")
    val sha256File = project.file("openssl.sha256")
    val ascFile = project.file("openssl.asc")

    outputs.file(outputFile)
    outputs.file(sha256File)
    outputs.file(ascFile)

    doLast {
        // Download source
        URL(opensslDownloadUrl).openStream().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Download SHA256
        URL(opensslSha256Url).openStream().use { input ->
            sha256File.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Download ASC signature
        URL(opensslAscUrl).openStream().use { input ->
            ascFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Set the source property after download
        ndkPorts.source.set(outputFile)
    }
}

// Task to verify OpenSSL integrity
tasks.register<DefaultTask>("verifyOpenSSL") {
    dependsOn("downloadOpenSSL")

    doLast {
        val sourceFile = project.file("src.tar.gz")
        val sha256File = project.file("openssl.sha256")

        // Read expected SHA256
        val expectedHash = sha256File.readText().trim().split(" ")[0]

        // Calculate actual SHA256
        val digest = MessageDigest.getInstance("SHA-256")
        val actualHash = sourceFile.inputStream().use { input ->
            digest.digest(input.readBytes())
        }.joinToString("") { "%02x".format(it) }

        // Verify hash
        if (expectedHash != actualHash) {
            throw GradleException("SHA256 verification failed! Expected: $expectedHash, Got: $actualHash")
        }

        try {
            exec {
                commandLine("gpg", "--verify", "openssl.asc", "src.tar.gz")
            }
        } catch (e: Exception) {
            logger.warn("GPG verification skipped or failed. Please verify manually if needed.")
        }
    }
}

// Make extractSrc task depend on verifyOpenSSL
tasks.named("extractSrc") {
    dependsOn("verifyOpenSSL")
}

tasks.register<AdHocPortTask>("buildPort") {
    dependsOn("extractSrc")

    builder {
        run {
            args(
                sourceDirectory.resolve("Configure").absolutePath,
                "android-${toolchain.abi.archName}",
                "-D__ANDROID_API__=${toolchain.api}",
                "--prefix=${installDirectory.absolutePath}",
                "--openssldir=${installDirectory.absolutePath}",
                "no-sctp",
                "shared"
            )

            env("ANDROID_NDK", toolchain.ndk.path.absolutePath)
            env("PATH", "${toolchain.binDir}:${System.getenv("PATH")}")
        }

        run {
            args("make", "-j$ncpus", "SHLIB_EXT=.so")
            env("ANDROID_NDK", toolchain.ndk.path.absolutePath)
            env("PATH", "${toolchain.binDir}:${System.getenv("PATH")}")
        }

        run {
            args("make", "install_sw", "SHLIB_EXT=.so")
            env("ANDROID_NDK", toolchain.ndk.path.absolutePath)
            env("PATH", "${toolchain.binDir}:${System.getenv("PATH")}")
        }
    }
}

tasks.prefabPackage {
    version.set(CMakeCompatibleVersion.parse(portVersion))
    licensePath.set("LICENSE.txt")
    modules {
        create("crypto")
        create("ssl")
    }
}

// Task to run all necessary steps in one command
tasks.register<DefaultTask>("buildAll") {
    dependsOn("buildPort")
    group = "build"
    description = "Downloads, verifies, and builds OpenSSL."

    doLast {
        println("All tasks completed successfully.")
    }
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["prefab"])
            pom {
                name.set("OpenSSL")
                description.set("The ndkports AAR for OpenSSL.")
                url.set("https://github.com/${System.getenv("GITHUB_REPOSITORY")}")
                licenses {
                    license {
                        name.set("Dual OpenSSL and SSLeay License")
                        url.set("https://www.openssl.org/source/license-openssl-ssleay.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("The Android Open Source Project")
                    }
                }
                scm {
                    val repoUrl = "https://github.com/${System.getenv("GITHUB_REPOSITORY")}"
                    url.set(repoUrl)
                    connection.set("scm:git:$repoUrl.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY")}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

distributions {
    main {
        contents {
            from("${project.buildDir}/repository")
            include("**/*.aar")
            include("**/*.pom")
        }
    }
}

tasks {
    distZip {
        dependsOn("publish")
        destinationDirectory.set(File(rootProject.buildDir, "distributions"))
    }
}