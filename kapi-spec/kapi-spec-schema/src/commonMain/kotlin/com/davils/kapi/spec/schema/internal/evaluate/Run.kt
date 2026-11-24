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

package com.davils.kapi.spec.schema.internal.evaluate

import com.davils.kapi.spec.schema.RefusalReason
import com.davils.kapi.spec.schema.SchemaLimits
import com.davils.kapi.spec.schema.internal.Meter
import com.davils.kapi.spec.schema.internal.Refusal
import com.davils.kapi.spec.schema.internal.Trail
import com.davils.kapi.spec.schema.internal.value.Instance

private const val CELLS_PER_STEP: Long = 64

private const val CELL_COPIES: Long = 2

internal class Run(
    private val graph: Graph,
    val limits: SchemaLimits,
    private val reported: List<Failure>? = null,
) : Meter {
    private var steps: Long = 0L

    private var depth: Int = 0

    val isTrackingAnnotations: Boolean get() = graph.isTrackingAnnotations

    private var trail: Trail? = null

    override fun charge(steps: Long) {
        this.steps += steps
        if (this.steps > limits.maximumEvaluationSteps) {
            throw Refusal(
                RefusalReason.EvaluationSteps,
                "The validation needs more than the ${limits.maximumEvaluationSteps} steps it may take",
                trail,
            )
        }
    }

    fun resource(index: Int): ResourceInfo = graph.resources[index]

    fun reportedTo(target: List<Failure>) {
        val isReported = target === reported
        if (isReported && target.size > limits.maximumErrors) {
            throw Refusal(RefusalReason.ReportedErrors, "The value has more than ${limits.maximumErrors} errors", null)
        }
    }

    fun evaluate(index: Int, visit: Visit, sink: MutableList<Failure>?): Evaluated? {
        trail = visit.instanceTrail
        charge(1L)
        depth++
        if (depth > limits.maximumEvaluationDepth) {
            throw Refusal(
                RefusalReason.EvaluationDepth,
                "The validation nests subschemas deeper than ${limits.maximumEvaluationDepth} levels",
                visit.instanceTrail,
            )
        }
        val node = graph.nodes[index]
        val evaluated = annotationsFor(visit.instance)
        val frame = Frame(
            run = this,
            node = node,
            visit = visit.entering(node.resource),
            evaluated = evaluated,
            sink = sink,
        )
        val isValid = frame.evaluateKeywords()
        depth--
        return evaluated.takeIf { isValid }
    }

    fun refuseLongText(text: String) {
        val isTooLong = text.length > limits.maximumStringLength
        if (isTooLong) {
            throw Refusal(
                RefusalReason.StringLength,
                "The string has more than the ${limits.maximumStringLength} characters a pattern or format may read",
                trail,
            )
        }
    }

    private fun annotationsFor(instance: Instance): Evaluated {
        val items = (instance as? Instance.ArrayValue)?.run { items.size }
        val size = items ?: (instance as? Instance.ObjectValue)?.run { keys.size }
        val tracked = size?.takeIf { isTrackingAnnotations }
        tracked?.let { cells -> charge(cells.toLong() * CELL_COPIES / CELLS_PER_STEP) }
        return tracked?.let(Evaluated::of) ?: Evaluated.UNTRACKED
    }
}
