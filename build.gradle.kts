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
    doLast {
        val projectInfo = subprojects.map { project ->
            val version = project.findProperty("portVersion") as? String ?: project.version.toString()
            val libName = project.findProperty("libName") as? String ?: project.name
            mapOf(
                "directory" to project.projectDir.name,
                "name" to libName,  // Now using libName instead of project.name
                "version" to version
            )
        }

        // Create output directory if it doesn't exist
        val outputDir = project.buildDir.resolve("project-info")
        outputDir.mkdirs()

        // Write to JSON file
        val outputFile = outputDir.resolve("projects.json")
        outputFile.writeText(groovy.json.JsonOutput.toJson(mapOf(
            "project" to projectInfo
        )))
    }
}

tasks.register("release") {
    // dependsOn(project.getTasksByName("test", true))
    dependsOn(project.getTasksByName("distZip", true))
}