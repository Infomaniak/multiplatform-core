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

package com.infomaniak.multiplatform_core.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtlintPluginTest {

    private fun projectWithPlugin(): Project = ProjectBuilder.builder().build().also {
        it.pluginManager.apply(PLUGIN_ID)
    }

    @Test
    fun `plugin applies ktlint gradle plugin`() {
        val project = projectWithPlugin()

        assertTrue(project.pluginManager.hasPlugin("org.jlleitschuh.gradle.ktlint"))
    }

    @Test
    fun `plugin registers ktlint extension with expected version`() {
        val project = projectWithPlugin()

        val extension = assertNotNull(project.extensions.findByType(KtlintExtension::class.java))
        assertEquals("1.8.0", extension.version.get())
    }

    @Test
    fun `plugin registers ktlint check and format tasks`() {
        val project = projectWithPlugin()

        assertNotNull(project.tasks.findByName("ktlintCheck"))
        assertNotNull(project.tasks.findByName("ktlintFormat"))
    }

    @Test
    fun `plugin can be applied by its class`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtlintPlugin::class.java)

        assertTrue(project.pluginManager.hasPlugin(PLUGIN_ID))
        assertTrue(project.pluginManager.hasPlugin("org.jlleitschuh.gradle.ktlint"))
    }

    private companion object {
        const val PLUGIN_ID = "com.infomaniak.multiplatform_core.plugin.ktlint"
    }
}
