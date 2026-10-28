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

package com.infomaniak.multiplatform_core.plugins

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtlintPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val sourceFile by lazy { projectDir.resolve("src/main/kotlin/com/example/Hello.kt") }

    // Groovy build scripts so that ktlintKotlinScriptCheck has nothing to lint besides our sources.
    @BeforeEach
    fun setUp() {
        projectDir.resolve("settings.gradle").writeText("rootProject.name = 'ktlint-test'\n")
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm'
                id 'com.infomaniak.multiplatform_core.plugins.ktlint'
            }

            repositories {
                mavenCentral()
            }
            """.trimIndent(),
        )
        sourceFile.parentFile.mkdirs()
    }

    @Test
    fun `ktlintCheck succeeds on well formatted sources`() {
        sourceFile.writeText(WELL_FORMATTED_SOURCE)

        val result = runner("ktlintCheck").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":ktlintMainSourceSetCheck")?.outcome)
    }

    @Test
    fun `ktlintCheck fails on badly formatted sources`() {
        sourceFile.writeText(BADLY_FORMATTED_SOURCE)

        val result = runner("ktlintCheck").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":ktlintMainSourceSetCheck")?.outcome)
        assertTrue(result.output.contains("Hello.kt"))
    }

    @Test
    fun `ktlintFormat fixes badly formatted sources`() {
        sourceFile.writeText(BADLY_FORMATTED_SOURCE)

        runner("ktlintFormat").build()

        assertTrue(sourceFile.readText() != BADLY_FORMATTED_SOURCE)
        val result = runner("ktlintCheck").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":ktlintMainSourceSetCheck")?.outcome)
    }

    @Test
    fun `ktlintCheck is compatible with configuration cache`() {
        sourceFile.writeText(WELL_FORMATTED_SOURCE)

        runner("ktlintCheck", "--configuration-cache").build()
        val result: BuildResult = runner("ktlintCheck", "--configuration-cache").build()

        assertTrue(result.output.contains("Reusing configuration cache"))
    }

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withProjectDir(projectDir)
        .withArguments(*arguments)

    private companion object {
        val WELL_FORMATTED_SOURCE = """
            package com.example

            fun hello(): String = "Hello"

        """.trimIndent()

        val BADLY_FORMATTED_SOURCE = """
            package com.example

            fun hello( ) : String{ return  "Hello" }

        """.trimIndent()
    }
}
