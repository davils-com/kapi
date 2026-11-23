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

package com.davils.kapi.codegen.core.launch

import com.davils.kore.core.annotation.DavilsInternal

/**
 * What a build makes of a run: the lines to show and the failures that stop it.
 *
 * Every error fails the build. A warning fails it too unless the build acknowledges its code with a reason, so no
 * warning goes unnoticed; an acknowledgement without a reason, or one that matches no warning of a generated run, fails
 * as well, so acknowledgements never outlive the warnings they were written for.
 *
 * @property lines every diagnostic, rendered.
 * @property failures why the build fails; empty when it passes.
 * @since 1.0.0
 */
@DavilsInternal
public class LaunchVerdict(
    public val lines: List<String>,
    public val failures: List<String>,
) {
    /**
     * Whether the build passes.
     *
     * @since 1.0.0
     */
    public val isAccepted: Boolean get() = failures.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is LaunchVerdict && lines == other.lines && failures == other.failures

    override fun hashCode(): Int = 31 * lines.hashCode() + failures.hashCode()

    override fun toString(): String = "LaunchVerdict(${lines.size} lines, ${failures.size} failures)"

    /**
     * Judges reports.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Judges [report] against [acknowledgements].
         *
         * @param report the report of the run.
         * @param acknowledgements the codes of warnings the build accepts, each with its reason.
         * @return the verdict.
         * @since 1.0.0
         */
        public fun of(report: LaunchReport, acknowledgements: Map<String, String>): LaunchVerdict {
            val warnings = report.diagnostics.filterNot(LaunchDiagnostic::isError)
            val errors = report.diagnostics.filter(LaunchDiagnostic::isError)
            val open = warnings.filter { warning -> acknowledgements[warning.code].isNullOrBlank() }
            val unmatched = acknowledgements.keys.filter { code -> warnings.none { warning -> warning.code == code } }
            val unused = unmatched.takeIf { report.outcome == LaunchOutcome.Generated }.orEmpty()
            val blank = acknowledgements.filterValues(String::isBlank).keys
            val failures = listOfNotNull(report.reason) +
                errors.map(LaunchDiagnostic::render) +
                open.map { warning -> warning.render() + " (acknowledge it with a reason or fix the document)" } +
                blank.map { code -> "The acknowledgement of $code needs a reason" } +
                unused.map { code -> "The acknowledgement of $code matches no warning and has to go" }
            return LaunchVerdict(report.diagnostics.map(LaunchDiagnostic::render), failures)
        }
    }
}
