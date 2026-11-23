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

package com.davils.kapi.runtime.testing.contract

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * What a [ContractCheck] found in a list of exchanges.
 *
 * @param findings the findings, in the order of the exchanges.
 * @property exchanges how many exchanges were checked.
 * @since 1.0.0
 */
public class ContractReport(findings: List<ContractFinding>, public val exchanges: Int) {
    /**
     * The findings, in the order of the exchanges.
     *
     * @since 1.0.0
     */
    public val findings: List<ContractFinding> = findings.toList()

    /**
     * Whether every exchange conforms to the document.
     *
     * @since 1.0.0
     */
    public val isConforming: Boolean get() = findings.isEmpty()

    /**
     * The codes of the findings, in order.
     *
     * @since 1.0.0
     */
    public val codes: List<ViolationCode> get() = findings.map(ContractFinding::code)

    private val identity: List<Any?> get() = listOf(exchanges, findings)

    /**
     * Renders the report: a summary line and one line per finding.
     *
     * @return the text, in which every text of the exchanges is escaped.
     * @since 1.0.0
     */
    public fun render(): String {
        val summary = "$exchanges exchanges checked, ${findings.size} findings"
        return (listOf(summary) + findings.map(ContractFinding::render)).joinToString("\n")
    }

    /**
     * Throws if the exchanges do not conform; works with every test framework.
     *
     * @throws AssertionError with the [render]ed report if there is a finding.
     * @since 1.0.0
     */
    public fun assertConforms() {
        if (!isConforming) throw AssertionError(render())
    }

    override fun equals(other: Any?): Boolean = other is ContractReport && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = "ContractReport(exchanges=$exchanges, findings=${findings.size})"
}
