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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.TextSnippet
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

private const val UNLOCATED: Int = -1

/**
 * The findings of one reading, shared by the readers of kapi: each finding once, at most [maximum] of them, with the
 * severity the [policy] gives it.
 *
 * Findings are not safe to share between threads; a reading owns its findings.
 *
 * @param policy the severity policy of the reading.
 * @param maximum the most findings to keep; one more says how many were dropped.
 * @param overflow the code of the finding that says how many were dropped.
 * @param baseSeverity the severity of a code without the policy.
 * @since 1.0.0
 */
@DavilsInternal
public open class Findings(
    private val policy: SeverityPolicy,
    private val maximum: Int,
    private val overflow: ViolationCode,
    private val baseSeverity: (ViolationCode) -> Severity,
) {
    private val reported: MutableList<Diagnostic> = mutableListOf()

    private val seen: MutableSet<Diagnostic> = hashSetOf()

    private var dropped: Int = 0

    /**
     * Whether an error was found, or findings had to be dropped.
     *
     * @since 1.0.0
     */
    public val hasErrors: Boolean get() = dropped > 0 || reported.any(Diagnostic::isError)

    /**
     * Reports a finding of [code].
     *
     * @param code the code.
     * @param message the message, which may contain text of the document.
     * @param location where the finding is, or `null`.
     * @param snippet the line of the source, or `null`.
     * @since 1.0.0
     */
    public fun report(code: ViolationCode, message: String, location: Location? = null, snippet: TextSnippet? = null) {
        add(
            Diagnostic(
                code = code,
                severity = policy.severityOf(code, baseSeverity(code)),
                message = message,
                location = location,
                snippet = snippet,
            ),
        )
    }

    /**
     * Reports a finding of another component, such as the YAML parser, with its own severity [base].
     *
     * @param code the code.
     * @param base the severity the component gave the finding.
     * @param message the message.
     * @param location where the finding is.
     * @param snippet the line of the source.
     * @since 1.0.0
     */
    public fun adopt(code: ViolationCode, base: Severity, message: String, location: Location, snippet: TextSnippet) {
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

    /**
     * Returns the places in [document] that findings name.
     *
     * @param document the document.
     * @return the JSON Pointers of the findings in it.
     * @since 1.0.0
     */
    public fun pointersIn(document: DocumentUri): Set<JsonPointer> {
        val locations = reported.mapNotNull(Diagnostic::location)
        val inDocument = locations.filter { location -> location.document == document }
        return inDocument.map(Location::pointer).toSet()
    }

    /**
     * Returns the findings sorted by document, by place and by code, and the finding about dropped ones last.
     *
     * @param order the position of every document in the reading.
     * @return the sorted findings.
     * @since 1.0.0
     */
    public fun sorted(order: Map<DocumentUri, Int>): List<Diagnostic> {
        val byPlace = compareBy<Diagnostic>(
            { diagnostic -> order[diagnostic.location?.document] ?: UNLOCATED },
            { diagnostic -> diagnostic.location?.range?.start ?: UNLOCATED },
            { diagnostic -> diagnostic.code.value },
            { diagnostic -> diagnostic.message },
        )
        return reported.sortedWith(byPlace) + overflowed()
    }

    private fun add(diagnostic: Diagnostic) {
        if (diagnostic in seen) return
        if (reported.size >= maximum) {
            dropped += 1
            return
        }
        seen += diagnostic
        reported += diagnostic
    }

    private fun overflowed(): List<Diagnostic> {
        if (dropped == 0) return emptyList()
        val message = "$dropped further findings were dropped, because a reading reports at most $maximum"
        return listOf(Diagnostic(overflow, Severity.Error, message))
    }
}
