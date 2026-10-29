/*
 * Infomaniak Core - Multiplatform
 * Copyright (C) 2026 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.infomaniak.multiplatform_core.convention

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson

/**
 * Makes a module independently publishable to Reposilite (and to npm when it has a JS target).
 *
 * - The module version lives in its own `gradle.properties` (`version=…`), bumped by release-please.
 * - CI overrides it with `-Ppublish.version.<module path>=<version>` (e.g. `publish.version.features.Sample`),
 *   which only affects this module, so the POMs of its dependencies keep their released versions.
 *
 * See docs/PUBLISHING.md.
 */
class PublishConvention : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("maven-publish")
        pluginManager.apply("signing")

        group = GROUP
        val declaredVersion = version.toString()
        val versionProperty = "publish.version." + path.removePrefix(":").replace(':', '.')
        providers.gradleProperty(versionProperty).orNull?.let { version = it }
        val publishedVersion = version.toString()
        val isSnapshot = publishedVersion.endsWith("-SNAPSHOT")

        val verifyPublishVersion = tasks.register("verifyPublishVersion") {
            group = "publishing"
            description = "Checks that a release version matches the version declared in gradle.properties."
            doLast {
                check(isSnapshot || publishedVersion == declaredVersion) {
                    "Release version $publishedVersion differs from the version declared in gradle.properties ($declaredVersion)"
                }
            }
        }
        tasks.withType<AbstractPublishToMaven>().configureEach { dependsOn(verifyPublishVersion) }

        configurePublishing(publishedVersion)
        configureSigning()
        configureNpmPackage(publishedVersion, isSnapshot)
        registerDependencyCheck()
    }

    private fun Project.configurePublishing(publishedVersion: String) {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach { pom { configureInfomaniakPom(project.name) } }
            // Lowercase artifactIds (`sample`, `sample-android`, …). Done after evaluation because the
            // Kotlin plugin assigns some publication artifactIds (e.g. JS) late.
            afterEvaluate {
                publications.withType<MavenPublication>().configureEach {
                    artifactId = artifactId.replaceFirst(project.name, project.name.lowercase())
                }
            }
            repositories.maven {
                name = "reposilite"
                url = uri(if (publishedVersion.endsWith("-SNAPSHOT")) SNAPSHOTS_URL else RELEASES_URL)
                credentials {
                    username = propertyOrEnv("reposiliteUsername")
                    password = propertyOrEnv("reposilitePassword")
                }
            }
        }
    }

    private fun Project.configureSigning() {
        val keyId = propertyOrEnv("GPG_key_id") ?: return
        val key = propertyOrEnv("GPG_private_key")?.replace('#', '\n') ?: return
        val password = propertyOrEnv("GPG_private_password") ?: return

        extensions.configure<SigningExtension> {
            isRequired = true
            useInMemoryPgpKeys(keyId, key, password)
            sign(extensions.getByType(PublishingExtension::class.java).publications)
        }
        // Workaround for https://github.com/gradle/gradle/issues/26091
        tasks.withType<AbstractPublishToMaven>().configureEach { mustRunAfter(tasks.withType<Sign>()) }
    }

    /**
     * Names the npm package `@<publish.npmScope>/multiplatform-core-<module>` and aligns its version with the module.
     * npm versions are immutable, so a snapshot gets a unique `<version>-snapshot.<publish.npmSnapshotId>` version.
     */
    private fun Project.configureNpmPackage(publishedVersion: String, isSnapshot: Boolean) {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val scope = providers.gradleProperty("publish.npmScope").getOrElse(DEFAULT_NPM_SCOPE)
            val snapshotId = providers.gradleProperty("publish.npmSnapshotId").getOrElse("local")
            val npmVersion = if (isSnapshot) {
                publishedVersion.removeSuffix("-SNAPSHOT") + "-snapshot.$snapshotId"
            } else {
                publishedVersion
            }
            val npmName = "@$scope/multiplatform-core-${project.name.lowercase()}"
            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinJsIrTarget>().configureEach {
                    compilations.named("main") { packageJson(npmPackageJson(npmName, npmVersion)) }
                }
            }
        }
    }

    /**
     * A published module can only depend on modules that are published too (listed in release-please-config.json),
     * otherwise its POM would reference an artifact that does not exist.
     */
    private fun Project.registerDependencyCheck() {
        val releaseConfig = rootProject.layout.projectDirectory.file("release-please-config.json")
        val projectDependencyPaths = provider {
            configurations
                .filter { it.name.endsWith("Api") || it.name.endsWith("Implementation") || it.name in setOf("api", "implementation") }
                .flatMap { it.dependencies.withType<ProjectDependency>() }
                .map { it.path }
                .filter { it != path }
                .toSortedSet()
        }
        val checkTask = tasks.register("verifyPublishableDependencies") {
            group = "publishing"
            description = "Checks that every project dependency of this module is published too."
            inputs.file(releaseConfig)
            inputs.property("projectDependencyPaths", projectDependencyPaths.map { it.toList() })
            doLast {
                @Suppress("UNCHECKED_CAST")
                val config = groovy.json.JsonSlurper().parse(releaseConfig.asFile) as Map<String, Any>
                val publishedPaths = (config["packages"] as Map<String, Any>).keys
                    .map { ":" + it.trim('/').replace('/', ':') }
                    .toSet()
                val unpublished = projectDependencyPaths.get() - publishedPaths
                check(unpublished.isEmpty()) {
                    "Published modules cannot depend on unpublished modules: $unpublished. " +
                        "Add them to release-please-config.json (see docs/PUBLISHING.md)."
                }
            }
        }
        tasks.withType<AbstractPublishToMaven>().configureEach { dependsOn(checkTask) }
    }

    // Built outside of the Project scope: the action is serialized by the configuration cache.
    private fun npmPackageJson(npmName: String, npmVersion: String) = Action<PackageJson> {
        name = npmName
        version = npmVersion
    }

    private fun Project.propertyOrEnv(name: String): String? = providers.gradleProperty(name).orNull ?: System.getenv(name)

    companion object {
        const val GROUP = "com.infomaniak.multiplatform_core"
        const val RELEASES_URL = "https://maven.infomaniak.app/releases"
        const val SNAPSHOTS_URL = "https://maven.infomaniak.app/snapshots"
        const val DEFAULT_NPM_SCOPE = "infomaniak"
    }
}

internal fun org.gradle.api.publish.maven.MavenPom.configureInfomaniakPom(moduleName: String) {
    name.set("multiplatform-core-${moduleName.lowercase()}")
    description.set("Infomaniak Core - Multiplatform - $moduleName")
    url.set("https://github.com/Infomaniak/multiplatform-core")
    licenses {
        license {
            name.set("GPL-3.0")
            url.set("https://www.gnu.org/licenses/gpl-3.0.html")
        }
    }
    issueManagement {
        system.set("GitHub")
        url.set("https://github.com/Infomaniak/multiplatform-core/issues")
    }
    scm {
        connection.set("scm:git:https://github.com/Infomaniak/multiplatform-core.git")
        url.set("https://github.com/Infomaniak/multiplatform-core")
    }
    organization {
        name.set("Infomaniak Network SA")
        url.set("https://www.infomaniak.com/")
    }
    developers {
        developer {
            id.set("Infomaniak")
            email.set("mobile+libraries@infomaniak-dev.ch")
            name.set("Infomaniak Development Team")
            url.set("https://www.infomaniak.com/")
        }
    }
}
