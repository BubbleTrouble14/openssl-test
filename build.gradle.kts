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
        val projects = subprojects.filter { proj ->
            proj.plugins.hasPlugin("com.android.ndkports.NdkPorts")
        }.map { proj ->
            // Add debug logging
            println("Processing project: ${proj.name}")
            println("Available properties: ${proj.properties.keys}")

            // Read properties from subproject's gradle.properties
            // Change this line to use the correct property names
            val version: String? = proj.findProperty("libVersion") as? String  // Changed from "version"
            val libName: String? = proj.findProperty("libName") as? String     // Keep as is

            // Debug log the values
            println("Found version: $version")
            println("Found libName: $libName")

            mapOf(
                "name" to proj.name,
                "version" to version,
                "libName" to libName
            )
        }

        val matrix = mapOf("project" to projects)
        val json = groovy.json.JsonBuilder(matrix).toPrettyString()

        // Debug log the final JSON
        println("Generated JSON: $json")

        project.buildDir.resolve("project-info").apply {
            mkdirs()
            resolve("projects.json").writeText(json)
        }
    }
}

tasks.register("openssl") {
     dependsOn(":openssl:distZip")
    // dependsOn(project.getTasksByName("test", true))
    // dependsOn(project.getTasksByName("distZip", true))
}
