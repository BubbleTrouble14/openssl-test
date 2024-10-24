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
    doLast {
        val projects = subprojects.filter { proj ->
            proj.plugins.hasPlugin("com.android.ndkports.NdkPorts")
        }.map { proj ->
            println("Processing project: ${proj.name}")

            // Get project-specific configuration
            val configs = rootProject.extra["projectConfigs"] as Map<String, Map<String, String>>
            val projectConfig = configs[proj.name] ?: error("No configuration found for project ${proj.name}")
            
            val version = projectConfig["libVersion"]
            val libName = projectConfig["libName"]

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
