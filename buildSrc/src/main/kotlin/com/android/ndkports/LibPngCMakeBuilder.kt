// package com.android.ndkports

// import org.gradle.api.provider.Property
// import org.gradle.api.tasks.Input
// import java.io.File

// class LibPngCMakeBuilder(val toolchain: Toolchain, val sysroot: File) : RunBuilder()

// abstract class LibPngCMakePortTask : PortTask() {
//     @get:Input
//     abstract val cmake: Property<LibPngCMakeBuilder.() -> Unit>

//     fun cmake(block: LibPngCMakeBuilder.() -> Unit) = cmake.set(block)

//     override fun buildForAbi(
//         toolchain: Toolchain,
//         workingDirectory: File,
//         buildDirectory: File,
//         installDirectory: File
//     ) {
//         logger.lifecycle("Building for ABI: ${toolchain.abi.abiName}")
//         logger.lifecycle("Working directory: $workingDirectory")
//         logger.lifecycle("Build directory: $buildDirectory")
//         logger.lifecycle("Install directory: $installDirectory")

//         configure(toolchain, buildDirectory, installDirectory)
//         build(buildDirectory)
//         install(buildDirectory)
//         inspectBuildResults(buildDirectory, installDirectory)
//     }

//     private fun configure(
//         toolchain: Toolchain, buildDirectory: File, installDirectory: File
//     ) {
//         val cmakeBlock = cmake.get()
//         val builder = LibPngCMakeBuilder(
//             toolchain,
//             prefabGenerated.get().asFile.resolve(toolchain.abi.triple)
//         )
//         builder.cmakeBlock()

//         val toolchainFile =
//             toolchain.ndk.path.resolve("build/cmake/android.toolchain.cmake")

//         buildDirectory.mkdirs()
//         val cmakeArgs = listOf(
//             "cmake",
//             "-DCMAKE_TOOLCHAIN_FILE=${toolchainFile.absolutePath}",
//             "-DCMAKE_BUILD_TYPE=RelWithDebInfo",
//             "-DCMAKE_INSTALL_PREFIX=${installDirectory.absolutePath}",
//             "-DANDROID_ABI=${toolchain.abi.abiName}",
//             "-DANDROID_API_LEVEL=${toolchain.api}",
//             "-DPNG_SHARED=ON",
//             "-DPNG_STATIC=OFF",
//             "-DPNG_TESTS=OFF",
//             "-DPNG_EXECUTABLES=OFF",
//             "-DZLIB_ROOT=${builder.sysroot}",
//             "-DPNG_LIBRARY_NAME=png",
//             "-DPNG_LIBRARIES=png",
//             "-DPNG_NO_SYMLINKS=ON",
//             "-GNinja",
//             sourceDirectory.get().asFile.absolutePath,
//         ) + builder.cmd

//         logger.lifecycle("CMake arguments: ${cmakeArgs.joinToString(" ")}")
//         executeSubprocess(cmakeArgs, buildDirectory, builder.env)

//         // Print CMake configuration
//         logger.lifecycle("CMake configuration:")
//         executeSubprocess(listOf("cmake", "-L", "-N", "."), buildDirectory)
//     }

//     private fun build(buildDirectory: File) {
//         logger.lifecycle("Building libpng")
//         executeSubprocess(listOf("ninja", "-v"), buildDirectory)
//     }

//     private fun install(buildDirectory: File) {
//         logger.lifecycle("Installing libpng")
//         executeSubprocess(listOf("ninja", "-v", "install"), buildDirectory)
//     }

//     private fun inspectBuildResults(buildDirectory: File, installDirectory: File) {
//         logger.lifecycle("Inspecting build results")
//         logger.lifecycle("Build directory contents:")
//         buildDirectory.walkTopDown().forEach { logger.lifecycle(it.absolutePath) }
//         logger.lifecycle("Install directory contents:")
//         installDirectory.walkTopDown().forEach { logger.lifecycle(it.absolutePath) }
//     }
// }
package com.android.ndkports

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.io.File

class LibPngCMakeBuilder(val toolchain: Toolchain, val sysroot: File) : RunBuilder()

abstract class LibPngCMakePortTask : PortTask() {
    @get:Input
    abstract val cmake: Property<LibPngCMakeBuilder.() -> Unit>

    fun cmake(block: LibPngCMakeBuilder.() -> Unit) = cmake.set(block)

    override fun buildForAbi(
        toolchain: Toolchain,
        workingDirectory: File,
        buildDirectory: File,
        installDirectory: File
    ) {
        configure(toolchain, buildDirectory, installDirectory)
        build(buildDirectory)
        install(buildDirectory)
        postProcessLibraries(installDirectory)
    }

    private fun configure(
        toolchain: Toolchain,
        buildDirectory: File,
        installDirectory: File
    ) {
        val cmakeBlock = cmake.get()
        val builder = LibPngCMakeBuilder(
            toolchain,
            prefabGenerated.get().asFile.resolve(toolchain.abi.triple)
        )
        builder.cmakeBlock()

        val toolchainFile =
            toolchain.ndk.path.resolve("build/cmake/android.toolchain.cmake")

        buildDirectory.mkdirs()
        executeSubprocess(
            listOf(
                "cmake",
                "-DCMAKE_TOOLCHAIN_FILE=${toolchainFile.absolutePath}",
                "-DCMAKE_BUILD_TYPE=RelWithDebInfo",
                "-DCMAKE_INSTALL_PREFIX=${installDirectory.absolutePath}",
                "-DANDROID_ABI=${toolchain.abi.abiName}",
                "-DANDROID_API_LEVEL=${toolchain.api}",
                "-DPNG_SHARED=ON",
                "-DPNG_STATIC=OFF",
                "-DPNG_TESTS=OFF",
                "-DPNG_EXECUTABLES=OFF",
                "-DZLIB_ROOT=${builder.sysroot}",
                "-DPNG_LIBRARY_NAME=png",
                "-DPNG_LIBRARIES=png",
                "-DPNG_NO_SYMLINKS=ON",
                "-DHAVE_LD_VERSION_SCRIPT=OFF",  // Disable version script
                "-GNinja",
                sourceDirectory.get().asFile.absolutePath,
            ) + builder.cmd, buildDirectory, builder.env
        )
    }

    private fun build(buildDirectory: File) =
        executeSubprocess(listOf("ninja", "-v"), buildDirectory)

    private fun install(buildDirectory: File) =
        executeSubprocess(listOf("ninja", "-v", "install"), buildDirectory)

    private fun postProcessLibraries(installDirectory: File) {
        val libDir = installDirectory.resolve("lib")
        libDir.listFiles()?.forEach { file ->
            when {
                file.name == "libpng16.so" -> {
                    val newName = file.parentFile.resolve("libpng.so")
                    logger.lifecycle("Renaming ${file.absolutePath} to ${newName.absolutePath}")
                    file.renameTo(newName)
                }
                file.name == "libpng.so" && file.isFile -> {
                    logger.lifecycle("Removing ${file.absolutePath}")
                    file.delete()
                }
            }
        }
    }
}
// package com.android.ndkports

// import org.gradle.api.provider.Property
// import org.gradle.api.tasks.Input
// import java.io.File

// class LibPngCMakeBuilder(val toolchain: Toolchain, val sysroot: File) : RunBuilder()

// abstract class LibPngCMakePortTask : PortTask() {
//     @get:Input
//     abstract val cmake: Property<LibPngCMakeBuilder.() -> Unit>

//     fun cmake(block: LibPngCMakeBuilder.() -> Unit) = cmake.set(block)

//     override fun buildForAbi(
//         toolchain: Toolchain,
//         workingDirectory: File,
//         buildDirectory: File,
//         installDirectory: File
//     ) {
//         configure(toolchain, buildDirectory, installDirectory)
//         build(buildDirectory)
//         install(buildDirectory)
//         renameLibraries(installDirectory)
//     }

//     private fun configure(
//         toolchain: Toolchain, buildDirectory: File, installDirectory: File
//     ) {
//         val cmakeBlock = cmake.get()
//         val builder = LibPngCMakeBuilder(
//             toolchain,
//             prefabGenerated.get().asFile.resolve(toolchain.abi.triple)
//         )
//         builder.cmakeBlock()

//         val toolchainFile =
//             toolchain.ndk.path.resolve("build/cmake/android.toolchain.cmake")

//         buildDirectory.mkdirs()
//         executeSubprocess(
//             listOf(
//                 "cmake",
//                 "-DCMAKE_TOOLCHAIN_FILE=${toolchainFile.absolutePath}",
//                 "-DCMAKE_BUILD_TYPE=RelWithDebInfo",
//                 "-DCMAKE_INSTALL_PREFIX=${installDirectory.absolutePath}",
//                 "-DANDROID_ABI=${toolchain.abi.abiName}",
//                 "-DANDROID_API_LEVEL=${toolchain.api}",
//                 "-DPNG_SHARED=ON",
//                 "-DPNG_STATIC=OFF",
//                 "-DPNG_TESTS=OFF",
//                 "-DPNG_EXECUTABLES=OFF",
//                 "-DZLIB_ROOT=${builder.sysroot}",
//                 "-GNinja",
//                 sourceDirectory.get().asFile.absolutePath,
//             ) + builder.cmd, buildDirectory, builder.env
//         )
//     }

//     private fun build(buildDirectory: File) =
//         executeSubprocess(listOf("ninja", "-v"), buildDirectory)

//     private fun install(buildDirectory: File) =
//         executeSubprocess(listOf("ninja", "-v", "install"), buildDirectory)

//     private fun renameLibraries(installDirectory: File) {
//         val libDir = installDirectory.resolve("lib")
//         libDir.listFiles()?.forEach { file ->
//             when {
//                 file.name == "libpng.so" -> {
//                     val newName = file.parentFile.resolve("libpng_original.so")
//                     println("Renaming ${file.absolutePath} to ${newName.absolutePath}")
//                     file.renameTo(newName)
//                 }
//                 file.name.startsWith("libpng16") -> {
//                     val newName = file.parentFile.resolve("libpng.so")
//                     println("Renaming ${file.absolutePath} to ${newName.absolutePath}")
//                     file.renameTo(newName)
//                 }
//             }
//         }
//     }
// }