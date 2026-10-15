plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
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
        id = "com.infomaniak.multiplatform_core.plugin.sample"
        implementationClass = "com.infomaniak.multiplatform_core.plugin.SamplePlugin"
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

@Suppress("UnstableApiUsage")
tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}
