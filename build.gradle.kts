buildscript {
    val snapshotSuffix = if (hasProperty("release")) {
        "-beta-1"
    } else {
        "-SNAPSHOT"
    }

    extra.apply {
        set("snapshotSuffix", snapshotSuffix)
    }

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    // Add jreleaser to buildscript classpath for Gradle 7.5
    dependencies {
        classpath("org.jreleaser:jreleaser-gradle-plugin:1.14.0")  // Use older version compatible with Gradle 7.5
    }
}

group = "com.android"
version = "1.0.0${extra.get("snapshotSuffix")}"

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

tasks.register("release") {
    dependsOn(project.getTasksByName("distZip", true))
    dependsOn(project.getTasksByName("publish", true))
    finalizedBy(project.getTasksByName("jreleaserFullRelease", true))
}