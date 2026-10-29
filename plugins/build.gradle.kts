plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    `maven-publish`
    signing
}

group = "com.infomaniak.multiplatform_core"

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        // Configure the built-in test suite
        val test = named<JvmTestSuite>("test") {
            // Use Kotlin test framework
            useKotlinTest(libs.versions.kotlin.get())
        }

        // Create a new test suite
        val functionalTest = register<JvmTestSuite>("functionalTest") {
            // Use Kotlin test framework
            useKotlinTest(libs.versions.kotlin.get())

            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project())
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) } 
                }
            }
        }
    }
}

gradlePlugin {
    // Define the plugin
    plugins.create("samplePlugin") {
        id = "com.infomaniak.multiplatform_core.plugins.sample"
        implementationClass = "com.infomaniak.multiplatform_core.plugins.SamplePlugin"
    }
    plugins.create("ktlint") {
        id = "com.infomaniak.multiplatform_core.plugins.ktlint"
        implementationClass = "com.infomaniak.multiplatform_core.plugins.KtlintPlugin"
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

// Exposes compileOnly plugins such as the Kotlin Gradle plugin (with their runtime dependencies) to GradleTestKit builds,
// so test projects can apply them alongside our plugins without making them runtime dependencies of the published plugins.
@Suppress("UnstableApiUsage")
val functionalTestPluginsClasspath = configurations.resolvable("functionalTestPluginsClasspath") {
    extendsFrom(configurations.compileOnly.get())
    attributes.addAllLater(configurations.runtimeClasspath.get().attributes)
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(functionalTestPluginsClasspath)
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

@Suppress("UnstableApiUsage")
tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}

//region Publishing (see docs/PUBLISHING.md)
// Mirrors build-logic's PublishConvention, which cannot be used here: build-logic itself depends on this build.
// The version lives in gradle.properties and CI overrides it with -Ppublish.version.plugins=<version>.
val declaredVersion = version.toString()
providers.gradleProperty("publish.version.plugins").orNull?.let { version = it }
val publishedVersion = version.toString()
val isSnapshot = publishedVersion.endsWith("-SNAPSHOT")

fun propertyOrEnv(name: String): String? = providers.gradleProperty(name).orNull ?: System.getenv(name)

val verifyPublishVersion = tasks.register("verifyPublishVersion") {
    group = "publishing"
    description = "Checks that a release version matches the version declared in gradle.properties."
    // Local copies: script-level values cannot be captured by the configuration cache.
    val published = publishedVersion
    val declared = declaredVersion
    doLast {
        check(published.endsWith("-SNAPSHOT") || published == declared) {
            "Release version $published differs from the version declared in gradle.properties ($declared)"
        }
    }
}

java {
    withSourcesJar()
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("multiplatform-core-plugins")
            description.set("Infomaniak Core - Multiplatform - Gradle plugins")
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
    }
    repositories.maven {
        name = "reposilite"
        url = uri(if (isSnapshot) "https://maven.infomaniak.app/snapshots" else "https://maven.infomaniak.app/releases")
        credentials {
            username = propertyOrEnv("reposiliteUsername")
            password = propertyOrEnv("reposilitePassword")
        }
    }
}

val signingKeyId = propertyOrEnv("GPG_key_id")
val signingKey = propertyOrEnv("GPG_private_key")?.replace('#', '\n')
val signingPassword = propertyOrEnv("GPG_private_password")
if (signingKeyId != null && signingKey != null && signingPassword != null) {
    signing {
        isRequired = true
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
    // Workaround for https://github.com/gradle/gradle/issues/26091
    tasks.withType<AbstractPublishToMaven>().configureEach { mustRunAfter(tasks.withType<Sign>()) }
}

tasks.withType<AbstractPublishToMaven>().configureEach { dependsOn(verifyPublishVersion) }
//endregion
