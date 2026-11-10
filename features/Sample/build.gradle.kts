import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("com.infomaniak.multiplatform_core.convention.android")
    id("com.infomaniak.multiplatform_core.convention.publish")
}

kotlin {
    android {
        namespace = "com.infomaniak.multiplatform_core.features.sample"
    }

    js {
        outputModuleName = "multiplatform-sample"
        browser()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "multiplatform-sample"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
