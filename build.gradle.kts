// settings.gradle.kts
rootProject.name = "ndkports"

// Only include the projects you want to build
include("openssl")
// include other projects as needed...

// Root build.gradle.kts
group = "com.android"
version = "1.0.0"

// Define a base extension for project properties
abstract class NdkPortExtension {
    var libName: String? = null
    var portVersion: String? = null
}

// Create the extension in root project
extensions.create<NdkPortExtension>("ndkPort")

// Apply the extension to all subprojects
subprojects {
    extensions.create<NdkPortExtension>("ndkPort")
}

tasks.register("exportProjectInfo") {
    outputs.file(layout.buildDirectory.file("project-info/projects.json"))

    doLast {
        val projectInfo = subprojects.map { project ->
            val extension = project.extensions.findByType<NdkPortExtension>()
            mapOf(
                "directory" to project.projectDir.name,
                "name" to (extension?.libName ?: project.name),
                "version" to (extension?.portVersion ?: project.version.toString())
            )
        }

        val outputDir = layout.buildDirectory.file("project-info").get().asFile
        outputDir.mkdirs()

        val outputFile = outputDir.resolve("projects.json")
        outputFile.writeText(groovy.json.JsonOutput.toJson(mapOf(
            "project" to projectInfo
        )))
    }
}

// openssl/build.gradle.kts
configure<NdkPortExtension> {
    libName = "OpenSSL"
    portVersion = "1.0.0"
}
// // buildscript {
// //     val snapshotSuffix = if (hasProperty("release")) {
// //         // We're still tagging releases as betas until we have more thorough
// //         // test automation.
// //         "port-beta-1"
// //     } else {
// //         "-SNAPSHOT"
// //     }

// //     extra.apply {
// //         set("snapshotSuffix", snapshotSuffix)
// //     }
// // }

// group = "com.android"
// version = "1.0.0"

// repositories {
//     mavenCentral()
//     google()
// }

// tasks.register("exportProjectInfo") {
//     doLast {
//         val projectInfo = subprojects.map { project ->
//             val version = project.findProperty("portVersion") as? String ?: project.version.toString()
//             val libName = project.findProperty("libName") as? String ?: project.name
//             mapOf(
//                 "directory" to project.projectDir.name,
//                 "name" to libName,  // Now using libName instead of project.name
//                 "version" to version
//             )
//         }

//         // Create output directory if it doesn't exist
//         val outputDir = project.buildDir.resolve("project-info")
//         outputDir.mkdirs()

//         // Write to JSON file
//         val outputFile = outputDir.resolve("projects.json")
//         outputFile.writeText(groovy.json.JsonOutput.toJson(mapOf(
//             "project" to projectInfo
//         )))
//     }
// }

// tasks.register("release") {
//     // dependsOn(project.getTasksByName("test", true))
//     dependsOn(project.getTasksByName("distZip", true))
// }