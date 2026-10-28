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

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidConvention : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.android.application") { configureAndroid() }
        pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
            configureMultiplatformAndroidLibrary()
        }
    }

    private fun Project.configureAndroid() {
        extensions.configure<CommonExtension> {
            compileSdk = ANDROID_COMPILE_SDK
            defaultConfig.apply {
                minSdk = ANDROID_MIN_SDK
            }
            compileOptions.apply {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
        }

        pluginManager.apply("com.infomaniak.multiplatform_core.plugin.ktlint")
    }

    private fun Project.configureMultiplatformAndroidLibrary() {
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.apply {
            this.extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                compileSdk = ANDROID_COMPILE_SDK
                minSdk = ANDROID_MIN_SDK
                compilerOptions.jvmTarget.set(JVM_TARGET)
                withHostTest {}
            }
        }
        pluginManager.apply("com.infomaniak.multiplatform_core.plugin.ktlint")
    }

    companion object AndroidSdk {
        const val ANDROID_MIN_SDK = 27
        const val ANDROID_COMPILE_SDK = 37
        val JAVA_VERSION = JavaVersion.VERSION_17
        val JVM_TARGET = JvmTarget.fromTarget(JAVA_VERSION.toString())
    }
}
