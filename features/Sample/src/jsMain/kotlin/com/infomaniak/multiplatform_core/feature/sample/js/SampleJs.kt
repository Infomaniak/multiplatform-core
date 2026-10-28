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
@file:OptIn(ExperimentalJsExport::class)

package com.infomaniak.multiplatform_core.feature.sample.js

import com.infomaniak.multiplatform_core.feature.sample.sample as commonSample

/**
 * JavaScript/TypeScript facade of the Sample module.
 *
 * Only the declarations of this package are exported to JavaScript, so `commonMain` stays free of JS-specific
 * annotations and the TypeScript API is explicitly controlled.
 */
@JsExport
fun sample(): String = commonSample()
