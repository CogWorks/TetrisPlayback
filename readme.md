# reTetris

A Tetris playback analysis tool for CogWorks

#### Project Structure
The project is structured as a multi-project [Gradle](https://gradle.org/) build.
Gradle itself need not be installed, since a [wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) is included in the repository.

More specifically, it consists of reusable modules that may define their own dependencies and API exposures. Additionally, the build process creates library JARs for each module, which may be leveraged by other projects as dependencies.

Eventually, if the project begins to sprawl, it may be wise to move dependency modules into separate repositories. Then other modules may use [JitPack](https://jitpack.io/) and Gradle to specify dependencies by version, futher modularizing the project.

#### Contributing
This repository uses a branch oriented workflow that goes like this:
##### Topic branches
1. A topic branch is checked out from `dev`.
    - The name of the branch should be of the form `topic/feature` or `topic/module/feature`, where `feature` is a succinct kebab-case name for the work. For example, `topic/eye-tracking` or `topic/ui/layout`.
2. Atomic units of work are committed with succinct messages.
    - To allow tools like `git bisect` to remain effective, individual commits should not prevent the project from compiling and running. Ideally, they should also preserve correctness (all tests should still pass).
3. Before pushing the branch to `origin`, the topic branch should be rebased onto `dev` if it is not up to date already.
    - Rebasing also provides an opportunity to organize commits when done interactively. This is another reason atomic commits are preferred.
4. After pushing the topic branch to `origin`, a pull request should be submitted.
5. The pull request will be reviewed, and after it is approved, merged into `dev`.
    - Pull requests should be merged via merge-commit, so each commit on `dev` represents a completed feature.
6. The remote copy of the topic branch is deleted to remove clutter.

##### Releases
1. After `dev` is considered to be in a stable state, a pull request is submitted from `dev` onto `release`. No further topic pull requests should be accepted until after the release.
2. Any changes suggested in the reviewing process should be committed to a `hotfix` branch.
3. Once all hotfixes are completed, the hotfixes are pull-requested onto `dev` and the original pull request may be accepted onto `release`.
4. Finally, a distributable can be created and uploaded as a GitHub release.
