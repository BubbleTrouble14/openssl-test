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
        println("Starting project info export...")

        val projects = subprojects.mapNotNull { proj ->
            val libName = proj.findProperty("libName")?.toString()
            val libVersion = proj.findProperty("libVersion")?.toString()

            if (libName != null && libVersion != null) {
                mapOf(
                    "name" to proj.name,  // This is all we need since it's the same as directory
                    "version" to libVersion,
                    "libName" to libName
                )
            } else {
                println("Warning: Project ${proj.name} is missing required properties (libName or libVersion)")
                null
            }
        }

        val matrix = mapOf("project" to projects)
        val json = groovy.json.JsonBuilder(matrix).toString()

        project.buildDir.resolve("project-info").apply {
            mkdirs()
            resolve("projects.json").writeText(json)
        }

        println("matrix=$json")
    }
}

tasks.register("openssl") {
     dependsOn(":openssl:distZip")
    // dependsOn(project.getTasksByName("test", true))
    // dependsOn(project.getTasksByName("distZip", true))
}
