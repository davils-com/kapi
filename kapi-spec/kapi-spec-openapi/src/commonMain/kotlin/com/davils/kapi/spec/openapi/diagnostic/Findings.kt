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

package com.davils.kapi.spec.openapi.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.policy.SeverityPolicy
import com.davils.kore.core.text.TextSnippet
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

private const val UNLOCATED: Int = -1

internal class Findings(
    private val policy: SeverityPolicy,
    private val maximum: Int,
) {
    private val reported: MutableList<Diagnostic> = mutableListOf()

    private val seen: MutableSet<Diagnostic> = hashSetOf()

    private var dropped: Int = 0

    val hasErrors: Boolean get() = dropped > 0 || reported.any(Diagnostic::isError)

    fun report(code: ViolationCode, message: String, location: Location? = null, snippet: TextSnippet? = null) {
        add(
            Diagnostic(
                code = code,
                severity = policy.severityOf(code),
                message = message,
                location = location,
                snippet = snippet,
            ),
        )
    }

    fun adopt(code: ViolationCode, base: Severity, message: String, location: Location, snippet: TextSnippet) {
        add(
            Diagnostic(
                code = code,
                severity = policy.severityOf(code, base),
                message = message,
                location = location,
                snippet = snippet,
            ),
        )
    }

    fun pointersIn(document: DocumentUri): Set<JsonPointer> {
        val locations = reported.mapNotNull(Diagnostic::location)
        val inDocument = locations.filter { location -> location.document == document }
        return inDocument.map(Location::pointer).toSet()
    }

    fun sorted(order: Map<DocumentUri, Int>): List<Diagnostic> {
        val byPlace = compareBy<Diagnostic>(
            { diagnostic -> order[diagnostic.location?.document] ?: UNLOCATED },
            { diagnostic -> diagnostic.location?.range?.start ?: UNLOCATED },
            { diagnostic -> diagnostic.code.value },
            { diagnostic -> diagnostic.message },
        )
        return reported.sortedWith(byPlace) + overflow()
    }

    private fun add(diagnostic: Diagnostic) {
        if (!seen.add(diagnostic)) return
        if (reported.size >= maximum) {
            dropped += 1
            return
        }
        reported += diagnostic
    }

    private fun overflow(): List<Diagnostic> {
        if (dropped == 0) return emptyList()
        val message = "$dropped further findings were dropped, because a reading reports at most $maximum"
        return listOf(Diagnostic(DiagnosticCodes.LIMIT_DIAGNOSTICS, Severity.Error, message))
    }
}
