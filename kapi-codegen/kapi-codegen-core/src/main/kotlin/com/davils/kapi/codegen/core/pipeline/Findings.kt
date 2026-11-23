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

package com.davils.kapi.codegen.core.pipeline

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

private const val NOWHERE: Int = -1

private val order: Comparator<Diagnostic> = compareBy(
    { diagnostic -> diagnostic.location?.run { document.value }.orEmpty() },
    { diagnostic -> diagnostic.location?.range?.start ?: NOWHERE },
    { diagnostic -> diagnostic.location?.run { pointer.toString() }.orEmpty() },
    { diagnostic -> diagnostic.code.value },
    { diagnostic -> diagnostic.message },
)

internal class Findings {
    private val collected: MutableList<Diagnostic> = mutableListOf()

    val hasErrors: Boolean get() = collected.any(Diagnostic::isError)

    fun error(code: ViolationCode, message: String, location: Location?) {
        collected += Diagnostic(
            code = code,
            severity = Severity.Error,
            message = message,
            location = location,
        )
    }

    fun warn(code: ViolationCode, message: String, location: Location?) {
        collected += Diagnostic(
            code = code,
            severity = Severity.Warning,
            message = message,
            location = location,
        )
    }

    fun add(diagnostic: Diagnostic) {
        collected += diagnostic
    }

    fun sorted(): List<Diagnostic> = collected.distinct().sortedWith(order)
}
