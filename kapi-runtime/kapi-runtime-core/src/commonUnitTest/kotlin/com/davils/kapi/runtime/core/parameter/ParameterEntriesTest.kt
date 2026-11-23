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

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.fixture.ASSIGNED
import com.davils.kapi.runtime.core.parameter.fixture.BLUE
import com.davils.kapi.runtime.core.parameter.fixture.FLAT
import com.davils.kapi.runtime.core.parameter.fixture.LISTED
import com.davils.kapi.runtime.core.parameter.fixture.entry
import com.davils.kapi.runtime.core.parameter.fixture.fieldNames
import com.davils.kapi.runtime.core.parameter.fixture.fields
import com.davils.kapi.runtime.core.parameter.fixture.format
import com.davils.kapi.runtime.core.parameter.fixture.items
import com.davils.kapi.runtime.core.parameter.fixture.query
import com.davils.kapi.runtime.core.parameter.fixture.text
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

private const val DEEP_RED: String = "color[R]"

private const val MANY: Int = 1_025

private const val FORM_QUERY: String = "a=1&b=2"

private val spread: List<ParameterEntry> = listOf(
    entry(value = "100", name = "R"),
    entry(value = "200", name = "G"),
    entry(value = "150", name = "B"),
)

private val deep: List<ParameterEntry> = listOf(
    entry(value = "100", name = DEEP_RED),
    entry(value = "200", name = "color[G]"),
    entry(value = "150", name = "color[B]"),
)

private val joinedStyles: Map<ParameterStyle, String> = mapOf(
    ParameterStyle.Form to ",",
    ParameterStyle.SpaceDelimited to " ",
    ParameterStyle.PipeDelimited to "|",
)

class ParameterEntriesTest : FunSpec({
    context("entries") {
        test("writes a text under the name of the parameter") {
            format(ParameterStyle.Form).entries(text) shouldBe listOf(entry(BLUE))
        }

        joinedStyles.forEach { (style, separator) ->
            test("joins a list and an object as $style") {
                val format = format(style)

                format.entries(items) shouldBe listOf(entry(LISTED.replace(oldValue = ",", newValue = separator)))
                format.entries(fields) shouldBe listOf(entry(FLAT.replace(oldValue = ",", newValue = separator)))
            }
        }

        test("joins a list with commas in the styles no query knows") {
            listOf(ParameterStyle.Simple, ParameterStyle.Label, ParameterStyle.Matrix, ParameterStyle.DeepObject)
                .forEach { style -> format(style).entries(items) shouldBe listOf(entry(LISTED)) }
        }

        test("repeats the name for every item of an exploded list and every field of an exploded object") {
            format(ParameterStyle.Form, isExploded = true).entries(items) shouldBe
                listOf(entry(BLUE), entry("black"), entry("brown"))
            format(ParameterStyle.Form, isExploded = true).entries(fields) shouldBe spread
        }

        test("writes every field of a deep object in brackets") {
            format(ParameterStyle.DeepObject, isExploded = true).entries(fields) shouldBe deep
            format(ParameterStyle.DeepObject).entries(fields) shouldBe deep
        }

        test("writes nothing for an empty list and an empty object") {
            val format = format(ParameterStyle.Form)

            format.entries(ParameterValue.Items(emptyList())) shouldBe emptyList()
            format.entries(ParameterValue.Fields(emptyMap())) shouldBe emptyList()
        }
    }

    context("queryText") {
        test("percent-encodes every character that is not unreserved") {
            val value = ParameterValue.Text("a/b?c&d=e#f+g h%41ü")

            format(ParameterStyle.Form).queryText(value) shouldBe "color=a%2Fb%3Fc%26d%3De%23f%2Bg%20h%2541%C3%BC"
            ParameterFormat("a b", ParameterStyle.Form, isExploded = false).queryText(text) shouldBe "a%20b=blue"
        }

        test(
            "keeps reserved characters and percent-encoded octets with allowReserved, but never those that end a value",
        ) {
            val value = ParameterValue.Text("a/b?c&d=e#f+g h%41ü:@!$'()*,;[]%zz")

            format(ParameterStyle.Form, isReservedAllowed = true).queryText(value) shouldBe
                "color=a/b?c%26d%3De%23f%2Bg%20h%41%C3%BC:@!$'()*,;[]%25zz"
        }

        test("writes every item of an exploded list as its own pair") {
            format(ParameterStyle.Form, isExploded = true).queryText(items) shouldBe
                "color=blue&color=black&color=brown"
            format(ParameterStyle.Form).queryText(ParameterValue.Items(emptyList())) shouldBe ""
        }
    }

    context("readEntries") {
        test("reads a text") {
            format(ParameterStyle.Form).readEntries(query(listOf(entry(BLUE))), ParameterShape.Text) shouldBe
                ParameterReading.Present(text)
        }

        joinedStyles.keys.forEach { style ->
            test("reads back a joined list and object as $style") {
                val format = format(style)

                format.readEntries(query(format.entries(items)), ParameterShape.Items) shouldBe
                    ParameterReading.Present(items)
                format.readEntries(query(format.entries(fields)), ParameterShape.Fields) shouldBe
                    ParameterReading.Present(fields)
            }
        }

        test("reads every value of an exploded list and refuses more than 1024") {
            val format = format(ParameterStyle.Form, isExploded = true)

            format.readEntries(
                query(format.entries(items)),
                ParameterShape.Items,
            ) shouldBe ParameterReading.Present(items)
            format.readEntries(mapOf("color" to List(MANY) { "a" }), ParameterShape.Items) shouldBe
                ParameterReading.Malformed
        }

        test("reads the named fields of an exploded object and the fields of a deep object") {
            format(ParameterStyle.Form, isExploded = true)
                .readEntries(
                    query(spread + entry(value = "1", name = "page")),
                    ParameterShape.Fields,
                    fieldNames,
                ) shouldBe
                ParameterReading.Present(fields)
            format(ParameterStyle.DeepObject, isExploded = true)
                .readEntries(query(deep + entry(value = "1", name = "page")), ParameterShape.Fields) shouldBe
                ParameterReading.Present(fields)
        }

        test("is absent when nothing carries the parameter") {
            val absent = ParameterReading.Absent
            val entries = query(listOf(entry(value = "1", name = "page")))

            format(ParameterStyle.Form).readEntries(entries, ParameterShape.Text) shouldBe absent
            format(ParameterStyle.Form, isExploded = true).readEntries(entries, ParameterShape.Items) shouldBe absent
            format(
                ParameterStyle.Form,
                isExploded = true,
            ).readEntries(entries, ParameterShape.Fields, fieldNames) shouldBe
                absent
            format(ParameterStyle.DeepObject).readEntries(entries, ParameterShape.Fields) shouldBe absent
            format(
                ParameterStyle.DeepObject,
            ).readEntries(query(listOf(entry(value = "1", name = "color[R"))), ParameterShape.Fields) shouldBe
                absent
        }

        test("is malformed when a single value is given more than once or a deep object nests or repeats a field") {
            val malformed = ParameterReading.Malformed
            val entries = query(
                listOf(entry(BLUE), entry("black"), entry(value = "1", name = "R"), entry(value = "2", name = "R")),
            )
            val deepObject = format(ParameterStyle.DeepObject)

            format(ParameterStyle.Form).readEntries(entries, ParameterShape.Text) shouldBe malformed
            format(ParameterStyle.Form).readEntries(entries, ParameterShape.Items) shouldBe malformed
            format(
                ParameterStyle.Form,
                isExploded = true,
            ).readEntries(entries, ParameterShape.Fields, fieldNames) shouldBe
                malformed
            deepObject.readEntries(
                query(listOf(entry(value = "1", name = "color[R][x]"))),
                ParameterShape.Fields,
            ) shouldBe malformed
            deepObject.readEntries(
                query(listOf(entry(value = "1", name = DEEP_RED), entry(value = "2", name = DEEP_RED))),
                ParameterShape.Fields,
            ) shouldBe
                malformed
            format(
                ParameterStyle.Form,
            ).readEntries(query(listOf(entry("R,100,G"))), ParameterShape.Fields) shouldBe malformed
        }
    }

    context("header fields") {
        test("are written in the simple style and exploded objects assigned") {
            val format = format(ParameterStyle.Simple)

            format.headerTextOrNull(text) shouldBe BLUE
            format.headerTextOrNull(items) shouldBe LISTED
            format.headerTextOrNull(fields) shouldBe FLAT
            format(ParameterStyle.Simple, isExploded = true).headerTextOrNull(fields) shouldBe ASSIGNED
        }

        test("refuse a value that would inject another field") {
            val format = format(ParameterStyle.Simple)

            format.headerTextOrNull(ParameterValue.Text("a\r\nX-Injected: 1")).shouldBeNull()
            format.headerTextOrNull(ParameterValue.Items(listOf("a", "b\n"))).shouldBeNull()
            format.headerTextOrNull(ParameterValue.Text("\u0000")).shouldBeNull()
        }

        test("are read with the blanks around every part dropped") {
            val format = format(ParameterStyle.Simple)

            format.readHeader(" blue ", ParameterShape.Text) shouldBe ParameterReading.Present(text)
            format.readHeader("blue, black ,brown", ParameterShape.Items) shouldBe ParameterReading.Present(items)
            format.readHeader("R, 100, G, 200, B, 150", ParameterShape.Fields) shouldBe ParameterReading.Present(fields)
            format(ParameterStyle.Simple, isExploded = true).readHeader(ASSIGNED, ParameterShape.Fields) shouldBe
                ParameterReading.Present(fields)
            format.readHeader(null, ParameterShape.Text) shouldBe ParameterReading.Absent
            format.readHeader("R,100,G", ParameterShape.Fields) shouldBe ParameterReading.Malformed
            format.readHeader(
                List(MANY) { "a" }.joinToString(","),
                ParameterShape.Items,
            ) shouldBe ParameterReading.Malformed
        }
    }

    context("cookies") {
        test("are written in the cookie style of OpenAPI 3.2 without encoding") {
            val cookie = format(ParameterStyle.Cookie)
            val exploded = format(ParameterStyle.Cookie, isExploded = true)

            cookie.cookieTextOrNull(text) shouldBe "color=blue"
            cookie.cookieTextOrNull(items) shouldBe "color=blue,black,brown"
            exploded.cookieTextOrNull(items) shouldBe "color=blue; color=black; color=brown"
            exploded.cookieTextOrNull(fields) shouldBe "R=100; G=200; B=150"
        }

        test("are percent-encoded in the form style") {
            format(ParameterStyle.Form).cookieTextOrNull(ParameterValue.Text("a b;c")) shouldBe "color=a%20b%3Bc"
        }

        test("refuse a value or a name a cookie cannot carry") {
            val cookie = format(ParameterStyle.Cookie)

            listOf("a b", "a;b", "a\"b", "a\\b", "a\r\nb", "é").forEach { value ->
                cookie.cookieTextOrNull(ParameterValue.Text(value)).shouldBeNull()
            }
            val spaced = ParameterFormat(name = "a b", style = ParameterStyle.Cookie, isExploded = false)

            spaced.cookieTextOrNull(text).shouldBeNull()
        }

        test("are read from a Cookie field in every shape") {
            val cookie = format(ParameterStyle.Cookie)
            val exploded = format(ParameterStyle.Cookie, isExploded = true)
            val header = "session=x; color=blue,black,brown; R=100; G=200; B=150; junk"

            cookie.readCookie(header, ParameterShape.Items) shouldBe ParameterReading.Present(items)
            exploded.readCookie(header, ParameterShape.Fields, fieldNames) shouldBe ParameterReading.Present(fields)
            exploded.readCookie("color=blue; color=black; color=brown", ParameterShape.Items) shouldBe
                ParameterReading.Present(items)
            cookie.readCookie("color=\"blue\"", ParameterShape.Text) shouldBe ParameterReading.Present(text)
            cookie.readCookie(null, ParameterShape.Text) shouldBe ParameterReading.Absent
            cookie.readCookie("other=1", ParameterShape.Text) shouldBe ParameterReading.Absent
        }

        test("are percent-decoded in the form style and refused when malformed or too many") {
            val form = format(ParameterStyle.Form)

            form.readCookie(
                "color=a%20b",
                ParameterShape.Text,
            ) shouldBe ParameterReading.Present(ParameterValue.Text("a b"))
            form.readCookie("color=%zz", ParameterShape.Text) shouldBe ParameterReading.Malformed
            form.readCookie("a=1;".repeat(MANY), ParameterShape.Text) shouldBe ParameterReading.Malformed
        }
    }

    context("querystring") {
        test("keeps a form as it is and encodes every other media type as a whole") {
            QueryString.encode(FORM_QUERY, MediaType.FormUrlEncoded) shouldBe FORM_QUERY
            QueryString.encode("""{"a":[1, 2]}""", MediaType.Json) shouldBe "%7B%22a%22%3A%5B1%2C%202%5D%7D"
        }

        test("reads the raw query back as one value") {
            QueryString.read(FORM_QUERY, MediaType.FormUrlEncoded) shouldBe
                ParameterReading.Present(ParameterValue.Text(FORM_QUERY))
            QueryString.read("%7B%22a%22%3A1%7D", MediaType.Json) shouldBe
                ParameterReading.Present(ParameterValue.Text("""{"a":1}"""))
            QueryString.read(null, MediaType.Json) shouldBe ParameterReading.Absent
            QueryString.read("%zz", MediaType.Json) shouldBe ParameterReading.Malformed
        }
    }

    context("the values") {
        test("are equal by their contents and show their texts escaped") {
            ParameterValue.Items(listOf("a")) shouldBe ParameterValue.Items(listOf("a"))
            ParameterValue.Items(listOf("a")).hashCode() shouldBe listOf("a").hashCode()
            ParameterValue.Fields(mapOf("a" to "1")).hashCode() shouldBe mapOf("a" to "1").hashCode()
            ParameterValue.Text("a").hashCode() shouldBe "a".hashCode()
            ParameterValue.Text("a\nb").toString() shouldNotContain "\n"
            ParameterValue.Items(listOf("a\nb")).toString() shouldNotContain "\n"
            ParameterValue.Fields(mapOf("a\n" to "b")).toString() shouldNotContain "\n"
            ParameterEntry(name = "a\n", value = "b").toString() shouldNotContain "\n"
            ParameterEntry(
                name = "a",
                value = "b",
            ).hashCode() shouldBe ParameterEntry(name = "a", value = "b").hashCode()
            format(ParameterStyle.Form) shouldBe format(ParameterStyle.Form)
            format(ParameterStyle.Form).hashCode() shouldBe format(ParameterStyle.Form).hashCode()
            format(ParameterStyle.Form) shouldNotBe format(ParameterStyle.Form, isReservedAllowed = true)
            format(ParameterStyle.Form).toString() shouldBe
                "ParameterFormat(name=color, style=Form, isExploded=false, isReservedAllowed=false)"
            ParameterReading.Present(text).hashCode() shouldBe text.hashCode()
            listOf(ParameterReading.Absent, ParameterReading.Malformed).map(ParameterReading::toString) shouldBe
                listOf("Absent", "Malformed")
            ParameterReading.Present(text).toString() shouldBe "Present(Text(blue))"
            listOf(
                ParameterValue.Text("a"),
                ParameterEntry(name = "a", value = "b"),
                ParameterReading.Present(text),
            ).forEach { value ->
                value.equals("a") shouldBe false
            }
        }
    }
})
