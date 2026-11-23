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
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.overlay.fixture.overlaid
import com.davils.kapi.spec.openapi.overlay.fixture.overlayHead
import com.davils.kapi.spec.openapi.upgrade.fixture.nodeOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val TARGET_INFO: String = "  - target: ${'$'}.info"

private const val PATHS: String = "paths"

private const val PETS: String = "/pets"

private const val REMOVE: String = "    remove: true"

private val document: List<String> = listOf(
    "info: {title: Petstore, version: 1.0.0, contact: {name: Team, email: team@example.com}}",
    "tags: [{name: a}, {name: b}, {name: c}]",
    "paths:",
    "  /pets:",
    "    get: {operationId: list, summary: List, tags: [a]}",
    "    post: {operationId: create, summary: Create}",
)

private fun applied(vararg actions: String) = overlaid(document, overlayHead + actions)

class OverlayApplicationTest : FunSpec({
    context("an update") {
        test("merges objects at every depth, replaces scalars and lists and adds new fields") {
            val result = applied(
                TARGET_INFO,
                "    update: {title: Pets, contact: {name: Owners, url: 'https://example.com'}, x-audience: internal}",
                "  - target: \"$.paths['/pets'].get\"",
                "    update: {tags: [b]}",
            )

            result.root["info"] shouldBe nodeOf(
                "title: Pets",
                "version: 1.0.0",
                "contact: {name: Owners, email: team@example.com, url: 'https://example.com'}",
                "x-audience: internal",
            )
            result.root[PATHS][PETS]["get"]["tags"] shouldBe nodeOf("[b]")
            result.diagnostics.shouldBeEmpty()
        }

        test("appends its value to a list") {
            applied("  - target: $.tags", "    update: {name: d}").root["tags"] shouldBe
                nodeOf("[{name: a}, {name: b}, {name: c}, {name: d}]")
        }

        test("changes every node a filter selects") {
            val result = applied(
                "  - target: \"$.paths['/pets'][?@.operationId == 'create']\"",
                "    update: {deprecated: true}",
            )

            result.root[PATHS][PETS]["post"]["deprecated"] shouldBe nodeOf("true")
            result.root[PATHS][PETS]["get"]["deprecated"].shouldBeNull()
        }

        test("sees what the actions before it changed") {
            val result = applied(
                TARGET_INFO,
                "    update: {x-first: 1}",
                "  - target: \"$.info['x-first']\"",
                REMOVE,
            )

            result.root["info"]["x-first"].shouldBeNull()
        }

        test("inserts nodes that name no place in the document") {
            applied(TARGET_INFO, "    update: {summary: Pets}").root["info"]["summary"]?.range.shouldBeNull()
        }

        test("is refused for a scalar target and for a value that is no object") {
            val result = applied(
                "  - target: $.info.title",
                "    update: {text: Pets}",
                TARGET_INFO,
                "    update: Pets",
            )

            result.codes shouldBe List(2) { DiagnosticCodes.OVERLAY_UPDATE }
            result.root["info"]["title"] shouldBe nodeOf("Petstore")
        }
    }

    context("a removal") {
        test("removes every selected node, the items of a list included") {
            val result = applied(
                "  - target: $.tags[0,2]",
                REMOVE,
                "  - target: $.info.contact",
                REMOVE,
            )

            result.root["tags"] shouldBe nodeOf("[{name: b}]")
            result.root["info"]["contact"].shouldBeNull()
        }

        test("removes a node together with the nodes below it that the target selects as well") {
            val result = applied("  - target: $.info..*", REMOVE)

            result.root["info"] shouldBe nodeOf("{}")
            result.diagnostics.shouldBeEmpty()
        }

        test("is refused for the whole document and for a node it already removed") {
            val result = applied(
                "  - target: $",
                REMOVE,
                "  - target: \"$.info['title','title']\"",
                REMOVE,
            )

            result.codes shouldBe List(2) { DiagnosticCodes.OVERLAY_UPDATE }
            result.root["info"]["title"] shouldBe nodeOf("Petstore")
        }
    }

    context("a target that selects nothing") {
        test("is reported and changes nothing") {
            val result = applied("  - target: $.servers", "    update: {url: /}")

            result.codes shouldBe listOf(DiagnosticCodes.OVERLAY_UNMATCHED)
            result.root["servers"].shouldBeNull()
        }
    }
})
