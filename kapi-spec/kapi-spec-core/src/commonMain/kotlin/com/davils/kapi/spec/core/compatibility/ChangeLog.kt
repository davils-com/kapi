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

package com.davils.kapi.spec.core.compatibility

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kore.core.validation.violation.ViolationCode

private const val INCOMPLETE: String = "incomplete"

/**
 * Collects the changes of one comparison within its limits, each change once.
 *
 * The codes of the changes are `<prefix>.compatibility.<rule>`. Once [ReadLimits.maximumDiagnostics] changes are
 * collected, further changes are counted and reported as one change of the rule `incomplete`; once
 * [ReadLimits.maximumNodes] steps were taken, [step] answers `false` and the comparison is reported as incomplete
 * once. Nothing is dropped silently. A log is not safe to share between threads.
 *
 * @param prefix the prefix of the codes, such as `openapi`.
 * @param settings the settings of the comparison.
 * @since 1.0.0
 */
@DavilsInternal
public class ChangeLog(private val prefix: String, settings: ComparisonSettings) {
    /**
     * The limits of the comparison.
     *
     * @since 1.0.0
     */
    public val limits: ReadLimits = settings.limits

    private val recorded: MutableList<Change> = mutableListOf()

    private val seen: OrderedSet<String> = OrderedSet()

    private val incompletes: OrderedSet<String> = OrderedSet()

    private var dropped: Int = 0

    private var steps: Long = 0L

    /**
     * Returns the code of [rule].
     *
     * @param rule the rule, such as `path-removed`.
     * @return the code `<prefix>.compatibility.<rule>`.
     * @since 1.0.0
     */
    public fun code(rule: String): ViolationCode = ViolationCode("$prefix.compatibility.$rule")

    /**
     * Records a change of [rule], once.
     *
     * @param rule the rule that found the change.
     * @param impacts what the change means for clients and for providers, in this order.
     * @param message what changed, with every text of a document shown through `SafeText`.
     * @param baseline where the changed part stands in the baseline.
     * @param candidate where the changed part stands in the candidate.
     * @since 1.0.0
     */
    public fun report(
        rule: String,
        impacts: Pair<Impact, Impact>,
        message: String,
        baseline: Location?,
        candidate: Location?,
    ) {
        val change = Change(
            code = code(rule),
            client = impacts.first,
            provider = impacts.second,
            message = message,
            baseline = baseline,
            candidate = candidate,
        )
        val places = "${baseline?.toString().orEmpty()}|${candidate?.toString().orEmpty()}"
        val key = "${change.code}|$impacts|$message|$places"
        val isFull = recorded.size >= limits.maximumDiagnostics
        if (isFull) dropped += 1
        if (!isFull && seen.add(key)) recorded += change
    }

    /**
     * Counts one step of the comparison.
     *
     * @param baseline where the comparison stands in the baseline.
     * @param candidate where the comparison stands in the candidate.
     * @return `true` if the comparison may go on, `false` once it took more steps than [ReadLimits.maximumNodes].
     * @since 1.0.0
     */
    public fun step(baseline: Location?, candidate: Location?): Boolean =
        steps(count = 1L, baseline = baseline, candidate = candidate)

    /**
     * Counts [count] steps of the comparison, such as one per value of two enums.
     *
     * @param count the number of steps.
     * @param baseline where the comparison stands in the baseline.
     * @param candidate where the comparison stands in the candidate.
     * @return `true` if the comparison may go on, `false` once it took more steps than [ReadLimits.maximumNodes].
     * @since 1.0.0
     */
    public fun steps(count: Long, baseline: Location?, candidate: Location?): Boolean {
        steps += count
        val isWithin = steps <= limits.maximumNodes
        if (!isWithin) {
            val message = "The comparison stopped after ${limits.maximumNodes} steps, so it may miss changes"
            incomplete(message = message, baseline = baseline, candidate = candidate)
        }
        return isWithin
    }

    /**
     * Records that the comparison could not look at a part of the documents, which breaks both sides because nothing
     * is known about it.
     *
     * @param message why the part was left out.
     * @param baseline where the part stands in the baseline.
     * @param candidate where the part stands in the candidate.
     * @since 1.0.0
     */
    public fun incomplete(message: String, baseline: Location?, candidate: Location?) {
        if (!incompletes.add(message)) return
        report(
            rule = INCOMPLETE,
            impacts = Impact.Breaking to Impact.Breaking,
            message = message,
            baseline = baseline,
            candidate = candidate,
        )
    }

    /**
     * Returns the comparison of every change recorded so far.
     *
     * @return the comparison, with one change of the rule `incomplete` at its end if changes were dropped.
     * @since 1.0.0
     */
    public fun comparison(): Comparison {
        if (dropped == 0) return Comparison(recorded)
        val message = "$dropped further changes were dropped, because a comparison reports at most " +
            "${limits.maximumDiagnostics}"
        val overflow = Change(
            code = code(INCOMPLETE),
            client = Impact.Breaking,
            provider = Impact.Breaking,
            message = message,
        )
        return Comparison(recorded + overflow)
    }
}
