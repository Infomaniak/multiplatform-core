plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    implementation("com.infomaniak.multiplatform_core:plugins")
}

gradlePlugin {
    plugins {
        register("android") {
            id = "com.infomaniak.multiplatform_core.convention.android"
            implementationClass = "com.infomaniak.multiplatform_core.convention.AndroidConvention"
        }
    }
}
