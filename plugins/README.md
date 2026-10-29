# Plugins

Gradle plugins meant to be **published** and applied by Infomaniak apps (and by `multiplatform-core` itself).

> This module is different from [`build-logic`](../build-logic): `build-logic` holds internal convention plugins that only
> configure this repository and are never published. `plugins` holds public plugins with a stable API.

All the plugins share a single version, declared in `gradle.properties` and released with release-please (tag
`plugins-v<version>`). See [Publishing](../docs/PUBLISHING.md) to release them, publish snapshots or consume them.

## Structure

`plugins` is an [included build](https://docs.gradle.org/current/userguide/composite_builds.html), not a regular subproject:

- The root `settings.gradle.kts` declares it with `pluginManagement { includeBuild("plugins") }`, so modules of this
  repository can apply its plugins by id, built from source.
- `build-logic/settings.gradle.kts` also includes it, so convention plugins can apply these plugins (dependency
  `com.infomaniak.multiplatform_core:plugins`, substituted with the local sources).
- Dependencies only go from `build-logic` to `plugins`, never the other way around.
- `plugins/settings.gradle.kts` reuses the shared version catalog from `../gradle/libs.versions.toml`.

## Available plugins

| Id                                                | Class          | Description                                                               |
|---------------------------------------------------|----------------|---------------------------------------------------------------------------|
| `com.infomaniak.multiplatform_core.plugins.ktlint` | `KtlintPlugin` | Applies [ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle) and pins the ktlint version. |
| `com.infomaniak.multiplatform_core.plugins.sample` | `SamplePlugin` | Template plugin registering a `samplePlugin` task that prints a message.  |

### Usage

```kotlin
plugins {
    id("com.infomaniak.multiplatform_core.plugins.ktlint")
}
```

In this repository, the Android convention plugin (`build-logic`) already applies the ktlint plugin to every Android module.

### Adding a plugin

1. Create the `Plugin<Project>` implementation in `src/main/kotlin/com/infomaniak/multiplatform_core/plugins/`.
2. Register it in the `gradlePlugin { }` block of `build.gradle.kts` with its id and implementation class.
3. Add unit tests and functional tests (see below).

The module uses `java-gradle-plugin` with the Kotlin JVM plugin (not `kotlin-dsl`), so use the plain Gradle API, e.g.
`extensions.configure(MyExtension::class.java) { it.foo.set(...) }`.

## Tests

The module has two test suites:

| Suite            | Sources               | Tooling          | What it checks                                                                   |
|------------------|-----------------------|------------------|----------------------------------------------------------------------------------|
| `test`           | `src/test/`           | `ProjectBuilder` | Fast, in-memory checks: applied plugins, extensions, registered tasks.           |
| `functionalTest` | `src/functionalTest/` | GradleTestKit    | End-to-end checks: runs a real Gradle build in a temporary project and asserts on task outcomes, output and files. |

Functional tests use `GradleRunner.withPluginClasspath()`, which injects the plugins of this module into the test build.
`compileOnly` dependencies, such as the Kotlin Gradle plugin, are also injected (through the
`functionalTestPluginsClasspath` configuration) so test projects can apply `org.jetbrains.kotlin.jvm` without making it a
runtime dependency of the published plugins.

The ktlint functional tests download ktlint from Maven Central, so they require network access.

### Running the tests

From the repository root:

```bash
# Unit tests
./gradlew :plugins:test --rerun

# Functional tests
./gradlew :plugins:functionalTest --rerun

# A single test class
./gradlew :plugins:functionalTest --tests '*KtlintPluginFunctionalTest' --rerun

# Both suites
./gradlew :plugins:test :plugins:functionalTest --rerun-tasks
```

Without `--rerun`, Gradle skips test tasks (`UP-TO-DATE` / `FROM-CACHE`) when neither the plugins nor the tests changed
since the last successful run, even after a clean, because results are restored from the build cache.
`--rerun` only applies to the task right before it; `--rerun-tasks` forces every task of the build (including
compilation) to run again.

HTML reports are generated in `plugins/build/reports/tests/<suite>/index.html`.
