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

package com.davils.kapi.runtime.core.pattern

internal class PatternReading private constructor(val node: PatternNode?, val issue: PatternIssue?) {
    companion object {
        fun read(source: String, limits: PatternLimits): PatternReading {
            val parsed = parse(source, limits)
            val node = parsed.node ?: return parsed
            val states = ProgramCompiler.states(node)
            val isTooLarge = states > limits.maximumStates
            val issue = PatternIssue(
                reason = PatternRefusal.TooManyStates,
                offset = 0,
                detail = "the automaton would need more than ${limits.maximumStates} states",
            )
            return PatternReading(node = null, issue = issue).takeIf { isTooLarge } ?: parsed
        }

        fun syntaxIssue(source: String): PatternIssue? {
            val issue = parse(source, PatternLimits.Ceilings).issue
            return issue?.takeUnless { it.reason.isValidSyntax }
        }

        private fun parse(source: String, limits: PatternLimits): PatternReading {
            if (source.length > limits.maximumSourceLength) {
                val detail = "the source has ${source.length} code units, more than ${limits.maximumSourceLength}"
                val issue = PatternIssue(PatternRefusal.TooLong, limits.maximumSourceLength, detail)
                return PatternReading(node = null, issue = issue)
            }
            return parseWithin(source, limits.maximumNesting)
        }

        private fun parseWithin(source: String, maximumNesting: Int): PatternReading {
            val parser = PatternParser(source, maximumNesting)
            var failed: PatternIssue? = null
            val node = try {
                parser.parse()
            } catch (failure: PatternFailure) {
                failed = failure.issue
                null
            }
            val issue = failed ?: parser.unsupported
            return PatternReading(node = node.takeIf { issue == null }, issue = issue)
        }
    }
}
