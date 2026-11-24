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
import com.davils.kapi.spec.schema.internal.Refusal
import com.davils.kapi.spec.schema.internal.Trail
import com.davils.kapi.spec.schema.internal.value.Instance
import com.davils.kapi.spec.schema.internal.value.Texts

internal class Frame(
    val run: Run,
    val node: Node,
    val visit: Visit,
    val evaluated: Evaluated,
    val sink: MutableList<Failure>?,
) {
    val instance: Instance get() = visit.instance

    val isCollecting: Boolean get() = sink != null

    fun evaluateKeywords(): Boolean {
        var isValid = true
        for (keyword in node.keywords) {
            val isPassed = keyword.evaluate(this)
            isValid = isValid && isPassed
            if (!isValid && sink == null) break
        }
        return isValid
    }

    fun trail(vararg tokens: String): Trail? = tokens.fold(visit.keywordTrail) { trail, token -> Trail(trail, token) }

    fun temporarySink(): MutableList<Failure>? = sink?.let { ArrayList() }

    fun inPlace(index: Int, keywordTrail: Trail?, sink: MutableList<Failure>?): Evaluated? {
        val inner = Visit(
            instance = instance,
            instanceTrail = visit.instanceTrail,
            keywordTrail = keywordTrail,
            scope = visit.scope,
            chain = visit.chain,
        )
        return run.evaluate(index, inner, sink)
    }

    fun referenced(index: Int, keywordTrail: Trail?): Evaluated? {
        val chain = visit.chain
        val isCycle = chain?.contains(index) == true
        if (isCycle) {
            throw Refusal(
                RefusalReason.ReferenceCycle,
                "A reference leads back to itself without reading any part of the value",
                visit.instanceTrail,
            )
        }
        val inner = Visit(
            instance = instance,
            instanceTrail = visit.instanceTrail,
            keywordTrail = keywordTrail,
            scope = visit.scope,
            chain = Chain(chain, index),
        )
        return run.evaluate(index, inner, sink)
    }

    fun descend(
        index: Int,
        child: Instance,
        token: String,
        keywordTrail: Trail?,
        sink: MutableList<Failure>?,
    ): Boolean {
        val inner = Visit(
            instance = child,
            instanceTrail = Trail(visit.instanceTrail, token),
            keywordTrail = keywordTrail,
            scope = visit.scope,
            chain = null,
        )
        return run.evaluate(index, inner, sink) != null
    }

    fun merge(result: Evaluated?) {
        evaluated.merge(result ?: Evaluated.UNTRACKED)
    }

    fun fail(keyword: String, message: String): Boolean {
        val target = sink ?: return false
        run.charge(1L)
        val trail = visit.keywordTrail.takeIf { keyword.isEmpty() } ?: Trail(visit.keywordTrail, keyword)
        val absolute = node.absolute.takeIf { keyword.isEmpty() } ?: (node.absolute + "/" + Texts.escapedToken(keyword))
        target += Failure(keyword = trail, absolute = absolute, instance = visit.instanceTrail, message = message)
        run.reportedTo(target)
        return false
    }

    fun failAll(failures: List<Failure>?) {
        val target = sink ?: return
        target.addAll(failures.orEmpty())
        run.reportedTo(target)
    }
}
