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

package com.davils.kapi.runtime.core.parameter

import com.davils.kapi.runtime.core.parameter.fixture.ASSIGNED
import com.davils.kapi.runtime.core.parameter.fixture.BLUE
import com.davils.kapi.runtime.core.parameter.fixture.FLAT
import com.davils.kapi.runtime.core.parameter.fixture.LISTED
import com.davils.kapi.runtime.core.parameter.fixture.StyleRow
import com.davils.kapi.runtime.core.parameter.fixture.fields
import com.davils.kapi.runtime.core.parameter.fixture.format
import com.davils.kapi.runtime.core.parameter.fixture.items
import com.davils.kapi.runtime.core.parameter.fixture.text
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val MANY: Int = 1_025

private val rows: List<StyleRow> = listOf(
    StyleRow(style = ParameterStyle.Simple, isExploded = false, text = BLUE, items = LISTED, fields = FLAT),
    StyleRow(style = ParameterStyle.Simple, isExploded = true, text = BLUE, items = LISTED, fields = ASSIGNED),
    StyleRow(style = ParameterStyle.Label, isExploded = false, text = ".blue", items = ".$LISTED", fields = ".$FLAT"),
    StyleRow(
        style = ParameterStyle.Label,
        isExploded = true,
        text = ".blue",
        items = ".blue.black.brown",
        fields = ".R=100.G=200.B=150",
    ),
    StyleRow(
        style = ParameterStyle.Matrix,
        isExploded = false,
        text = ";color=blue",
        items = ";color=$LISTED",
        fields = ";color=$FLAT",
    ),
    StyleRow(
        style = ParameterStyle.Matrix,
        isExploded = true,
        text = ";color=blue",
        items = ";color=blue;color=black;color=brown",
        fields = ";R=100;G=200;B=150",
    ),
    StyleRow(style = ParameterStyle.Form, isExploded = false, text = BLUE, items = LISTED, fields = FLAT),
)

private val otherStyles: List<ParameterStyle> = listOf(
    ParameterStyle.Form,
    ParameterStyle.SpaceDelimited,
    ParameterStyle.PipeDelimited,
    ParameterStyle.DeepObject,
    ParameterStyle.Cookie,
)

class ParameterPathTest : FunSpec({
    context("pathSegmentOrNull") {
        rows.forEach { row ->
            val format = format(row.style, row.isExploded)

            test("writes a text, a list and an object as ${row.style} with explode ${row.isExploded}") {
                format.pathSegmentOrNull(text) shouldBe row.text
                format.pathSegmentOrNull(items) shouldBe row.items
                format.pathSegmentOrNull(fields) shouldBe row.fields
            }
        }

        otherStyles.forEach { style ->
            test("writes and reads $style, which no path knows, as the simple style") {
                val format = format(style)

                format.pathSegmentOrNull(items) shouldBe LISTED
                format.readPath(LISTED, ParameterShape.Items) shouldBe ParameterReading.Present(items)
            }
        }

        test("escapes whatever is not unreserved, byte by byte") {
            val value = ParameterValue.Items(listOf("a/b", "é?", "x y", "~._-"))

            format(ParameterStyle.Simple).pathSegmentOrNull(value) shouldBe "a%2Fb,%C3%A9%3F,x%20y,~._-"
        }

        test("escapes the name of a matrix parameter") {
            val format = ParameterFormat("a b", ParameterStyle.Matrix, isExploded = false)

            format.pathSegmentOrNull(text) shouldBe ";a%20b=blue"
        }

        test("writes an empty matrix value without an assignment") {
            format(ParameterStyle.Matrix).pathSegmentOrNull(ParameterValue.Text("")) shouldBe ";color"
        }

        test("refuses a segment that would change the path") {
            val simple = format(ParameterStyle.Simple)

            simple.pathSegmentOrNull(ParameterValue.Text(".")) shouldBe null
            simple.pathSegmentOrNull(ParameterValue.Text("..")) shouldBe null
            format(ParameterStyle.Label).pathSegmentOrNull(ParameterValue.Text("")) shouldBe null
        }
    }

    context("readPath") {
        rows.forEach { row ->
            val format = format(row.style, row.isExploded)

            test("reads a text, a list and an object as ${row.style} with explode ${row.isExploded}") {
                format.readPath(row.text, ParameterShape.Text) shouldBe ParameterReading.Present(text)
                format.readPath(row.items, ParameterShape.Items) shouldBe ParameterReading.Present(items)
                format.readPath(row.fields, ParameterShape.Fields) shouldBe ParameterReading.Present(fields)
            }
        }

        test("is absent without a text and reads an empty text as no items") {
            format(ParameterStyle.Simple).readPath(null, ParameterShape.Text) shouldBe ParameterReading.Absent
            format(ParameterStyle.Simple).readPath("", ParameterShape.Items) shouldBe
                ParameterReading.Present(ParameterValue.Items(emptyList()))
        }

        test("reads a matrix value without an assignment as an empty text") {
            format(ParameterStyle.Matrix).readPath(";color", ParameterShape.Text) shouldBe
                ParameterReading.Present(ParameterValue.Text(""))
        }

        test("is malformed when the mark of the style is missing") {
            val malformed = ParameterReading.Malformed

            format(ParameterStyle.Label).readPath(BLUE, ParameterShape.Text) shouldBe malformed
            format(ParameterStyle.Matrix).readPath(";colour=blue", ParameterShape.Text) shouldBe malformed
            format(ParameterStyle.Matrix, isExploded = true).readPath(BLUE, ParameterShape.Items) shouldBe malformed
            format(
                ParameterStyle.Matrix,
                isExploded = true,
            ).readPath(";color=blue;size=1", ParameterShape.Items) shouldBe
                malformed
        }

        test("is malformed when an object has a key without a value or names a key twice") {
            val malformed = ParameterReading.Malformed

            format(ParameterStyle.Simple).readPath("R,100,G", ParameterShape.Fields) shouldBe malformed
            format(
                ParameterStyle.Simple,
                isExploded = true,
            ).readPath("R=100,G", ParameterShape.Fields) shouldBe malformed
            format(ParameterStyle.Simple).readPath("R,100,R,200", ParameterShape.Fields) shouldBe malformed
        }

        test("reads 1024 parts and refuses one more") {
            val simple = format(ParameterStyle.Simple)

            simple.readPath(List(MANY - 1) { "a" }.joinToString(","), ParameterShape.Items) shouldBe
                ParameterReading.Present(ParameterValue.Items(List(MANY - 1) { "a" }))
            simple.readPath(
                List(MANY) { "a" }.joinToString(","),
                ParameterShape.Items,
            ) shouldBe ParameterReading.Malformed
            format(ParameterStyle.Matrix, isExploded = true)
                .readPath(";color=a".repeat(MANY), ParameterShape.Items) shouldBe ParameterReading.Malformed
        }
    }
})
