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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

/**
 * Compares two JSON Schemas, read through [SchemaView]s, and records in a [ChangeLog] what got narrower, wider or
 * different, judged by the [Direction] the data flows in.
 *
 * The comparison follows references one hop at a time, at most `maximumReferenceHops` of the limits of the log in a
 * row, visits every pair of schemas of one direction once, so cyclic schemas end, descends at most `maximumDepth`
 * levels and counts a step of the log per pair and per value of an enum. What it cannot look at within these limits
 * it records as incomplete instead of skipping silently. A comparison is not safe to share between threads.
 *
 * The rules, each recorded as `<prefix>.compatibility.<rule>`: `type-narrowed`, `type-widened`, `type-changed`,
 * `nullable-removed`, `nullable-added`, `accepts`, `opaque`, `max-length`, `min-length`, `maximum`, `minimum`,
 * `max-items`, `min-items`, `max-properties`, `min-properties`, `multiple-of`, `pattern`, `format`, `unique-items`,
 * `enum`, `enum-value-removed`, `enum-value-added`, `const`, `discriminator`, `deprecated`, `property-removed`,
 * `property-added`, `required-property-added`, `property-required`, `property-optional`, `additional-properties`,
 * `items`, `all-of`, `any-of`, `one-of` and `incomplete`.
 *
 * @param log the log the changes go to.
 * @since 1.0.0
 */
@DavilsInternal
public class SchemaComparison(private val log: ChangeLog) {
    private val visited: OrderedSet<String> = OrderedSet()

    private val objects: ObjectRules = ObjectRules(peek = ::peek, nested = ::nested)

    /**
     * Compares [baseline] with [candidate] as schemas of data that flows in [direction].
     *
     * @param baseline the schema of the baseline.
     * @param candidate the schema of the candidate.
     * @param direction the way the data flows.
     * @since 1.0.0
     */
    public fun compare(baseline: SchemaView, candidate: SchemaView, direction: Direction) {
        nested(Pairing(baseline = baseline, candidate = candidate, direction = direction, depth = 0, log = log))
    }

    private fun nested(pairing: Pairing) {
        val baseline = resolved(view = pairing.baseline, pairing = pairing)
        val candidate = resolved(view = pairing.candidate, pairing = pairing)
        if (baseline == null || candidate == null) return
        val resolved = pairing.resolved(baseline = baseline, candidate = candidate)
        if (isWithin(resolved) && isFirstVisit(resolved)) rules(resolved)
    }

    private fun rules(pairing: Pairing) {
        val isStructured = ScalarRules.compare(pairing)
        if (!isStructured) return
        objects.compare(pairing)
        items(pairing)
        alternatives(pairing = pairing, keyword = Keyword.AllOf) { view -> view.allOf }
        alternatives(pairing = pairing, keyword = Keyword.AnyOf) { view -> view.anyOf }
        alternatives(pairing = pairing, keyword = Keyword.OneOf) { view -> view.oneOf }
    }

    private fun items(pairing: Pairing) {
        val before = pairing.baseline.items
        val after = pairing.candidate.items
        if (before != null && after != null) nested(pairing.child(baseline = before, candidate = after))
        if (before == null && after != null) {
            val message = "The items of an array ${pairing.label} are now described by a schema"
            pairing.report(rule = "items", impacts = pairing.narrowed, message = message)
        }
        if (before != null && after == null) {
            val message = "The items of an array ${pairing.label} are no longer described by a schema"
            pairing.report(rule = "items", impacts = pairing.widened, message = message)
        }
    }

    private fun alternatives(pairing: Pairing, keyword: Keyword, read: (SchemaView) -> List<SchemaView>) {
        val before = read(pairing.baseline)
        val after = read(pairing.candidate)
        if (before.size != after.size) {
            val isNarrowed = after.size > before.size == keyword.isNarrowedByMore
            val impacts = pairing.narrowed.takeIf { isNarrowed } ?: pairing.widened
            val counts = "from ${before.size} to ${after.size} schemas"
            val message = "The ${keyword.text} of a schema ${pairing.label} changes $counts"
            pairing.report(rule = keyword.rule, impacts = impacts, message = message)
        }
        before.zip(after).forEach { (old, new) -> nested(pairing.child(baseline = old, candidate = new)) }
    }

    private fun resolved(view: SchemaView, pairing: Pairing): SchemaView? {
        val limit = log.limits.maximumReferenceHops
        val chain = generateSequence(view, SchemaView::target)
        val hops = chain.take(limit + 2).toList()
        val last = hops.last()
        val isTooLong = hops.size > limit + 1
        val isUnresolved = last.isUnresolved
        if (isTooLong) pairing.incomplete("A schema ${pairing.label} passes through more than $limit references")
        if (isUnresolved) pairing.incomplete("A reference of a schema ${pairing.label} leads nowhere")
        return last.takeIf { !isTooLong && !isUnresolved }
    }

    private fun peek(view: SchemaView): SchemaView = view.dereferenced(log.limits.maximumReferenceHops)

    private fun isWithin(pairing: Pairing): Boolean {
        val limit = log.limits.maximumDepth
        val isShallow = pairing.depth <= limit
        if (!isShallow) pairing.incomplete("A schema ${pairing.label} is nested deeper than $limit levels")
        return isShallow && log.step(baseline = pairing.baseline.location, candidate = pairing.candidate.location)
    }

    private fun isFirstVisit(pairing: Pairing): Boolean {
        val baseline = pairing.baseline.location
        val candidate = pairing.candidate.location
        if (baseline == null || candidate == null) return true
        return visited.add("${pairing.direction}|$baseline|$candidate")
    }

    private enum class Keyword(val rule: String, val text: String, val isNarrowedByMore: Boolean) {
        AllOf(rule = "all-of", text = "allOf", isNarrowedByMore = true),
        AnyOf(rule = "any-of", text = "anyOf", isNarrowedByMore = false),
        OneOf(rule = "one-of", text = "oneOf", isNarrowedByMore = false),
    }
}
