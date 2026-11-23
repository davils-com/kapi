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

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.overlay.fixture.actionsOf
import com.davils.kapi.spec.openapi.overlay.fixture.overlaid
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val UNKNOWN_FIELDS: Int = 3

private const val INFO: String = "info: {title: Fixes, version: '1'}"

private const val ACTIONS: String = "actions: [{target: ${'$'}, update: {}}]"

private const val VERSION: String = "overlay: 1.0.0"

private val shape: List<ViolationCode> = listOf(DiagnosticCodes.OVERLAY_SHAPE)

private fun overlayOf(vararg lines: String) = overlaid(document = listOf("a: 1"), overlay = lines.asList())

class OverlayReaderTest : FunSpec({
    context("the document of an overlay") {
        test("is refused when it is no object") {
            overlayOf("- update").codes shouldBe shape
        }

        test("names Overlay 1.0 as its version") {
            val result = overlayOf(
                "overlay: 2.0.0",
                INFO,
                ACTIONS,
            )

            result.codes shouldBe listOf(DiagnosticCodes.OVERLAY_VERSION)
            result.diagnostics
                .single()
                .location
                ?.pointer shouldBe JsonPointer.of("overlay")
        }

        test("names its version as text") {
            overlayOf("overlay: 1.0", INFO, ACTIONS).codes shouldBe listOf(DiagnosticCodes.OVERLAY_VERSION)
            overlayOf(INFO, ACTIONS).codes shouldBe listOf(DiagnosticCodes.OVERLAY_VERSION)
        }

        test("refuses a field whose name is no text") {
            overlayOf(VERSION, INFO, ACTIONS, "[a]: b").codes shouldBe shape
        }

        test("needs info with a title and a version") {
            overlayOf(VERSION, "info: none", ACTIONS).codes shouldBe shape
            overlayOf(
                VERSION,
                "info: {title: Fixes}",
                ACTIONS,
            ).codes shouldBe shape
        }

        test("needs at least one action") {
            overlayOf(VERSION, INFO, "actions: []").codes shouldBe shape
            overlayOf(VERSION, INFO).codes shouldBe shape
        }

        test("allows extensions and refuses fields it does not know at every level") {
            val result = overlayOf(
                VERSION,
                "info: {title: Fixes, version: '1', x-team: api, owner: api}",
                "extends: petstore.yaml",
                "x-generated: false",
                "kind: fixes",
                "actions: [{target: $, update: {}, x-reason: none, why: none}]",
            )

            result.codes shouldBe List(UNKNOWN_FIELDS) { DiagnosticCodes.OVERLAY_SHAPE }
        }
    }

    context("an action") {
        test("needs a target that is a JSONPath query") {
            actionsOf("  - {update: {}}").codes shouldBe shape
            actionsOf(
                "  - {target: 'paths[', update: {}}",
            ).codes shouldBe listOf(DiagnosticCodes.OVERLAY_TARGET)
        }

        test("is an object") {
            val codes = actionsOf("  - fix").codes

            codes.toSet() shouldBe setOf(DiagnosticCodes.OVERLAY_SHAPE, DiagnosticCodes.OVERLAY_ACTION)
        }

        test("either updates or removes its target") {
            actionsOf("  - {target: $}").codes shouldBe listOf(DiagnosticCodes.OVERLAY_ACTION)
            actionsOf("  - {target: $.a, update: 2, remove: true}").codes shouldBe
                listOf(DiagnosticCodes.OVERLAY_ACTION)
            actionsOf("  - {target: $, update: {b: 2}, remove: false}").diagnostics.shouldBeEmpty()
        }
    }
})
