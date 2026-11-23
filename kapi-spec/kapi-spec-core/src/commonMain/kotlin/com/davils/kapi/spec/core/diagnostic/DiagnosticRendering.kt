/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.core.diagnostic

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.render
import com.davils.kore.core.validation.violation.Severity

private const val MAXIMUM_MESSAGE_LENGTH: Int = 1024

internal object DiagnosticRendering {
    private val labels: Map<Severity, String> = mapOf(
        Severity.Error to "error",
        Severity.Warning to "warning",
    )

    fun line(diagnostic: Diagnostic): String {
        val label = labels.getValue(diagnostic.severity)
        val message = SafeText.text(diagnostic.message, MAXIMUM_MESSAGE_LENGTH)
        val head = "$label: [${diagnostic.code}] $message"
        val location = diagnostic.location ?: return head
        return "$location: $head"
    }

    fun block(diagnostic: Diagnostic): String {
        val snippet = diagnostic.snippet ?: return line(diagnostic)
        return line(diagnostic) + "\n" + snippet.render()
    }
}
