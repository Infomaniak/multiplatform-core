plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("com.infomaniak.multiplatform_core.convention.android")
}

kotlin {
    android {
        namespace = "com.infomaniak.multiplatform_core.features.sample"
    }

    js {
        outputModuleName = "multiplatform-core-sample"
        browser()
        nodejs()
        useEsModules()
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
