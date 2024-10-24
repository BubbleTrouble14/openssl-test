// buildscript {
//     val snapshotSuffix = if (hasProperty("release")) {
//         // We're still tagging releases as betas until we have more thorough
//         // test automation.
//         "port-beta-1"
//     } else {
//         "-SNAPSHOT"
//     }

//     extra.apply {
//         set("snapshotSuffix", snapshotSuffix)
//     }
// }

group = "com.android"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
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

tasks.register("release") {
    // dependsOn(project.getTasksByName("test", true))
    dependsOn(project.getTasksByName("distZip", true))
}