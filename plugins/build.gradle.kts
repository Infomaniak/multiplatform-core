plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
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
