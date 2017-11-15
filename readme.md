# reTetris

A Tetris playback analysis tool for CogWorks

#### Project Structure
The project is structured as a multi-project [Gradle](https://gradle.org/) build.
Gradle itself need not be installed, since a [wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) is included in the repository.

More specifically, it consists of reusable modules that may define their own dependencies and API exposures. Additionally, the build process creates library JARs for each module, which may be leveraged by other projects as dependencies.

Eventually, if the project begins to sprawl, it may be wise to move dependency modules into separate repositories. Then other modules may use [JitPack](https://jitpack.io/) and Gradle to specify dependencies by version, futher modularizing the project.
