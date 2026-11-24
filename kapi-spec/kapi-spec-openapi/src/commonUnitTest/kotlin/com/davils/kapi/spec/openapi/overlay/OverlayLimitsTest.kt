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

package com.davils.kapi.spec.openapi.overlay

import com.davils.kaml.core.node.get
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.upgrade.nodeOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val NODES: Long = 40

private const val DEPTH: Int = 6

private const val MANY: Int = 10_001

private val small = ReadLimitsBuilder().run {
    maximumNodes = NODES
    maximumDepth = DEPTH
    build()
}

private val items: List<String> = listOf("items:") + (1..MANY).map { index -> "  - $index" }

class OverlayLimitsTest : FunSpec({
    context("an overlay at the limits of a reading") {
        test("leaves out an action that grows the document past its nodes, with a finding") {
            val document = listOf("a: {}")
            val fields = (1..NODES).joinToString { index -> "k$index: $index" }
            val overlay = overlayHead + listOf("  - target: $.a", "    update: {$fields}")

            val result = overlaid(document, overlay, small)

            result.root["a"] shouldBe nodeOf("{}")
            result.codes shouldBe listOf(DiagnosticCodes.OVERLAY_UPDATE)
        }

        test("leaves out an action that nests the document past its depth, with a finding") {
            val document = listOf("a: {}")
            val overlay = overlayHead + listOf("  - target: $.a", "    update: {b: {c: {d: {e: {f: {g: 1}}}}}}")

            overlaid(document, overlay, small).codes shouldBe listOf(DiagnosticCodes.OVERLAY_UPDATE)
        }

        test("refuses an update that every one of many matches would copy before it copies it") {
            val document = listOf("a: [{}, {}, {}, {}, {}]")
            val update = "    update: {b: 1, c: 2, d: 3, e: 4, f: 5, g: 6, h: 7, i: 8}"
            val overlay = overlayHead + listOf("  - target: $.a[*]", update)

            val result = overlaid(document, overlay, small)

            result.root["a"] shouldBe nodeOf("[{}, {}, {}, {}, {}]")
            result.codes shouldBe listOf(DiagnosticCodes.OVERLAY_UPDATE)
        }

        test("leaves out an action whose patch goes past the operations of an overlay instead of failing the reading") {
            val overlay = overlayHead + listOf("  - target: $.items[*]", "    remove: true")

            val result = overlaid(items, overlay)

            result.codes shouldBe listOf(DiagnosticCodes.OVERLAY_UPDATE)
        }
    }
})
