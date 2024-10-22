# NDK Ports Guide

This repository was forked from [the official NDK Ports repository](https://android.googlesource.com/platform/tools/ndkports/+/refs/heads/main). It provides instructions and tools for building ports with the Android NDK.

## Building a Port

To build a port using `ndkports`, follow these steps:

1. **Prepare Your Environment**:
   Ensure you have all necessary dependencies and your environment is set up for building.

2. **Run the Build Script**:
   Execute the build script by running the following command in your terminal:

   ```bash
   ./scripts/build_release.sh
   ```

3. **Control Library Inclusion**:
   Modify the `settings.gradle.kts` file to control which libraries are built. This file enumerates all available ports. Include a library in your build by adding a line like this:
   ```kotlin
   include("blst")
   ```
   Exclude a library by not listing it or commenting it out.

//Run locally

./gradlew -PndkPath="/Users/ronaldgoedeke/Library/Android/sdk/ndk/26.1.10909125" -Prelease release
