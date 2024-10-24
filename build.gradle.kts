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

// Define project configurations
extra["projectConfigs"] = mapOf(
    "openssl" to mapOf(
        "libVersion" to "3.4.0",
        "libName" to "OpenSSL"
    ),
)

tasks.register("exportProjectInfo") {
    // Create an output file property
    val outputFile = project.objects.property(File::class)

    doFirst {
        // Get the output file path from the provided property or use a default
        outputFile.set(project.layout.buildDirectory.file("ndkports-matrix.json").get().asFile)
        // Ensure the parent directory exists
        outputFile.get().parentFile.mkdirs()
    }

    doLast {
        val projects = subprojects.filter { proj ->
            proj.plugins.hasPlugin("com.android.ndkports.NdkPorts")
        }.map { proj ->
            // Get project-specific configuration
            val configs = rootProject.extra["projectConfigs"] as Map<String, Map<String, String>>
            val projectConfig = configs[proj.name] ?: error("No configuration found for project ${proj.name}")

            mapOf(
                "name" to proj.name,
                "version" to projectConfig["libVersion"],
                "libName" to projectConfig["libName"]
            )
        }

        val matrix = mapOf("include" to projects)
        val json = groovy.json.JsonBuilder(matrix).toPrettyString()

        // Write to the output file
        outputFile.get().writeText(json)

        // Print the file path for debugging
        println("Matrix JSON written to: ${outputFile.get().absolutePath}")
    }
}

tasks.register("openssl") {
     dependsOn(":openssl:distZip")
    // dependsOn(project.getTasksByName("test", true))
    // dependsOn(project.getTasksByName("distZip", true))
}
