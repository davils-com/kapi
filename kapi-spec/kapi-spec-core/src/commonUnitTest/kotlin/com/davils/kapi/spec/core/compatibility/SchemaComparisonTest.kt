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

import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PREFIX: String = "test.compatibility."

private const val TEN: Long = 10L

private const val FIVE: Long = 5L

private const val HUGE: Int = 20_000

private const val STRING: String = "string"

private const val NAME: String = "name"

private const val TYPE_NARROWED: String = "type-narrowed"

private const val TYPE_WIDENED: String = "type-widened"

private const val INTEGER: String = "integer"

private const val INCOMPLETE: String = "incomplete"

private const val THREE: Int = 3

private const val NUMBER: String = "number"

private const val STAMP: String = "stamp"

private const val PROPERTY_REMOVED: String = "property-removed"

private val narrowedInRequest: Pair<Impact, Impact> = Impact.Breaking to Impact.NonBreaking

private val widenedInRequest: Pair<Impact, Impact> = Impact.NonBreaking to Impact.Breaking

private fun Pair<SchemaView, SchemaView>.compared(
    direction: Direction = Direction.Request,
    configure: ComparisonBuilder.() -> Unit = {},
): Comparison {
    val builder = ComparisonBuilder()
    builder.configure()
    val log = ChangeLog("test", builder.build())
    SchemaComparison(log).compare(baseline = first, candidate = second, direction = direction)
    return log.comparison()
}

private fun Comparison.rules(): List<String> = changes.map { change -> change.code.value.removePrefix(PREFIX) }

private fun Comparison.impacts(): List<Pair<Impact, Impact>> =
    changes.map { change -> change.client to change.provider }

private fun Comparison.impactSet(): Set<Pair<Impact, Impact>> = impacts().toSet()

private fun Comparison.firstImpacts(): Pair<Impact, Impact> = impacts().first()

private fun Comparison.onlyMessage(): String = changes.single().message

private fun Comparison.firstMessage(): String = changes.first().message

private fun texts(vararg values: String): List<Value> = values.map { text -> Value.Text(text) }

class SchemaComparisonTest : FunSpec({
    context("the types of a schema") {
        test("narrow, widen or change") {
            val string = schema { types = setOf(STRING) }
            val any = schema()
            val both = schema { types = setOf(STRING, INTEGER) }

            (any to string).compared().rules() shouldBe listOf(TYPE_NARROWED)
            (string to any).compared().rules() shouldBe listOf(TYPE_WIDENED)
            (both to string).compared().rules() shouldBe listOf(TYPE_NARROWED)
            (string to both).compared().rules() shouldBe listOf(TYPE_WIDENED)
            (string to schema { types = setOf(INTEGER) }).compared().rules() shouldBe listOf("type-changed")
            (string to schema { types = setOf(STRING) }).compared().changes.shouldBeEmpty()
        }

        test("count an integer as a number") {
            val integer = schema { types = setOf(INTEGER) }
            val number = schema { types = setOf(NUMBER) }

            (integer to number).compared().rules() shouldBe listOf(TYPE_WIDENED)
            (number to integer).compared().rules() shouldBe listOf(TYPE_NARROWED)
        }

        test("tell null apart") {
            val nullable = schema { types = setOf(STRING, "null") }
            val string = schema { types = setOf(STRING) }

            (nullable to string).compared().rules() shouldBe listOf("nullable-removed")
            (string to nullable).compared().rules() shouldBe listOf("nullable-added")
        }

        test("leave alone what only differs in a type a number covers") {
            val numbers = schema { types = setOf(NUMBER, INTEGER) }
            val number = schema { types = setOf(NUMBER) }
            val nullable = schema { types = setOf(STRING, "null") }

            (numbers to number).compared().changes.shouldBeEmpty()
            (nullable to schema { types = setOf(STRING, INTEGER) }).compared().rules() shouldBe listOf("type-changed")
        }

        test("judge by the direction of the data") {
            val any = schema()
            val string = schema { types = setOf(STRING) }

            (any to string).compared(Direction.Request).impacts() shouldBe listOf(narrowedInRequest)
            (any to string).compared(Direction.Response).impacts() shouldBe listOf(widenedInRequest)
            (string to any).compared(Direction.Response).impacts() shouldBe listOf(narrowedInRequest)
        }
    }

    context("the bounds of a schema") {
        test("narrow when a maximum drops or a minimum rises and widen the other way") {
            val loose = schema {
                maxLength = number(TEN)
                minLength = number(1)
                maximum = number(TEN)
            }
            val tight = schema {
                maxLength = number(FIVE)
                minLength = number(2)
                maximum = number(FIVE)
            }

            (loose to tight).compared().rules() shouldBe listOf("max-length", "min-length", "maximum")
            (loose to tight).compared().impactSet() shouldBe setOf(narrowedInRequest)
            (tight to loose).compared().impactSet() shouldBe setOf(widenedInRequest)
        }

        test("narrow when they are added and widen when they are removed") {
            val bounded = schema {
                minimum = number(1)
                maxItems = number(TEN)
                minItems = number(1)
                maxProperties = number(TEN)
                minProperties = number(1)
            }

            (schema() to bounded).compared().impactSet() shouldBe setOf(narrowedInRequest)
            (bounded to schema()).compared().rules() shouldBe
                listOf("minimum", "max-items", "min-items", "max-properties", "min-properties")
            (bounded to schema()).compared().impactSet() shouldBe setOf(widenedInRequest)
        }

        test("narrow when the bound itself becomes excluded") {
            val inclusive = schema {
                minimum = number(1)
                maximum = number(TEN)
            }
            val exclusive = schema {
                minimum = number(1)
                isMinimumExclusive = true
                maximum = number(TEN)
                isMaximumExclusive = true
            }

            (inclusive to exclusive).compared().impacts() shouldBe listOf(narrowedInRequest, narrowedInRequest)
            (exclusive to inclusive).compared().impacts() shouldBe listOf(widenedInRequest, widenedInRequest)
            (exclusive to inclusive).compared().firstMessage() shouldContain "from 10 exclusive to 10"
        }
    }

    context("the texts of a schema") {
        test("narrow when added, widen when removed and break both sides when changed") {
            val plain = schema()
            val patterned = schema {
                pattern = "^a"
                format = "email"
                multipleOf = number(2)
            }
            val other = schema {
                pattern = "^b"
                format = "uri"
                multipleOf = number(FIVE)
            }

            (plain to patterned).compared().rules() shouldBe listOf("pattern", "format", "multiple-of")
            (plain to patterned).compared().impactSet() shouldBe setOf(narrowedInRequest)
            (patterned to plain).compared().impactSet() shouldBe setOf(widenedInRequest)
            (patterned to other).compared().impactSet() shouldBe setOf(Impact.Breaking to Impact.Breaking)
            (plain to patterned).compared().firstMessage() shouldContain "from nothing to \"^a\""
        }

        test("of unique items narrow and widen") {
            (schema() to schema { hasUniqueItems = true }).compared().impacts() shouldBe listOf(narrowedInRequest)
            (schema { hasUniqueItems = true } to schema()).compared().impacts() shouldBe listOf(widenedInRequest)
        }

        test("of a discriminator break both sides and a deprecation informs") {
            (schema { discriminator = "kind" } to schema { discriminator = "type" }).compared().rules() shouldBe
                listOf("discriminator")
            val deprecated = schema { isDeprecated = true }
            (schema() to deprecated).compared().impacts() shouldBe listOf(Impact.Info to Impact.Info)
            (schema { isDeprecated = true } to schema()).compared().changes.shouldBeEmpty()
        }
    }

    context("the values of a schema") {
        test("narrow when an enum is added or loses values and widen when it is removed or gains values") {
            val abc = schema { enumeration = texts("a", "b", "c") }
            val ab = schema { enumeration = texts("a", "b") }

            (schema() to abc).compared().rules() shouldBe listOf("enum")
            (abc to schema()).compared().impacts() shouldBe listOf(widenedInRequest)
            (abc to ab).compared().rules() shouldBe listOf("enum-value-removed")
            (ab to abc).compared().rules() shouldBe listOf("enum-value-added")
            (ab to abc).compared().onlyMessage() shouldContain "\"c\""
        }

        test("name at most five values of a change") {
            val many = schema { enumeration = texts("a", "b", "c", "d", "e", "f", "g") }

            (schema { enumeration = emptyList() } to many).compared().onlyMessage() shouldContain "and 2 more"
        }

        test("compare huge enums within the steps of the limits") {
            val values = (1..HUGE).map { index -> Value.Text("v$index") }
            val before = schema { enumeration = values }
            val after = schema { enumeration = values.drop(1) }

            (before to after).compared().rules() shouldBe listOf("enum-value-removed")
            (before to after).compared { limits { maximumNodes = HUGE.toLong() } }.rules() shouldBe listOf(INCOMPLETE)
        }

        test("of a const narrow, widen or break both sides") {
            val a = schema { constant = Value.Text("a") }

            (schema() to a).compared().impacts() shouldBe listOf(narrowedInRequest)
            (a to schema()).compared().impacts() shouldBe listOf(widenedInRequest)
            val b = schema { constant = Value.Text("b") }
            (a to b).compared().impacts() shouldBe listOf(Impact.Breaking to Impact.Breaking)
        }
    }

    context("a boolean or opaque schema") {
        test("narrows towards refusing and widens towards accepting") {
            val all = schema { isAccepting = true }
            val none = schema { isAccepting = false }
            val some = schema { types = setOf(STRING) }

            (all to some).compared().impacts() shouldBe listOf(narrowedInRequest)
            (some to none).compared().impacts() shouldBe listOf(narrowedInRequest)
            (none to all).compared().impacts() shouldBe listOf(widenedInRequest)
            (all to schema { isAccepting = true }).compared().changes.shouldBeEmpty()
            (all to some).compared().onlyMessage() shouldContain "accepting everything to a schema object"
        }

        test("breaks both sides when it becomes a schema of a format the comparison does not read") {
            (schema() to schema { opaque = Value.Text("int") }).compared().rules() shouldBe listOf("opaque")
        }

        test("breaks both sides when it changes in a format the comparison does not read") {
            val avro = schema { opaque = Value.Text("int") }

            (avro to schema { opaque = Value.Text("long") }).compared().rules() shouldBe listOf("opaque")
            (avro to schema { opaque = Value.Text("int") }).compared().changes.shouldBeEmpty()
        }
    }

    context("the properties of an object") {
        test("report what is removed, added, required and made optional") {
            val before = schema {
                properties = mapOf(NAME to schema(), "age" to schema(), "nick" to schema())
                required = listOf("nick")
            }
            val after = schema {
                properties = mapOf(
                    NAME to schema(),
                    "nick" to schema(),
                    "mail" to schema(),
                    "tag" to schema(),
                )
                required = listOf(NAME, "mail", "id")
            }

            (before to after).compared().rules() shouldBe listOf(
                PROPERTY_REMOVED,
                "required-property-added",
                "property-added",
                "property-required",
                "property-optional",
                "property-required",
            )
        }

        test("leave read-only properties out of requests and write-only properties out of responses") {
            val secret = schema { isWriteOnly = true }
            val stamp = schema { target = schema { isReadOnly = true } }
            val entity = schema { properties = mapOf("secret" to secret, STAMP to stamp) }

            (entity to schema()).compared(Direction.Request).rules() shouldBe listOf(PROPERTY_REMOVED)
            (entity to schema()).compared(Direction.Response).rules() shouldBe listOf(PROPERTY_REMOVED)
        }

        test("leave out what a request cannot send, also when it is added or becomes required") {
            val stamp = schema { isReadOnly = true }
            val before = schema {
                properties = mapOf(STAMP to stamp, "kept" to schema())
                required = listOf("kept", "loose")
            }
            val after = schema {
                properties = mapOf(STAMP to stamp, "kept" to schema(), "made" to stamp)
                required = listOf(STAMP, "kept", "loose", "made")
            }

            (before to after).compared().changes.shouldBeEmpty()
            (after to before).compared(Direction.Request).changes.shouldBeEmpty()
        }

        test("report whether they may hold what they do not name") {
            val open = schema()
            val closed = schema { additionalProperties = schema { isAccepting = false } }
            val typed = schema { additionalProperties = schema { types = setOf(STRING) } }

            (open to closed).compared().impacts() shouldBe listOf(narrowedInRequest)
            (closed to open).compared().impacts() shouldBe listOf(widenedInRequest)
            val alsoClosed = schema { additionalProperties = schema { isAccepting = false } }
            (closed to alsoClosed).compared().changes.shouldBeEmpty()
            (typed to schema { additionalProperties = schema() }).compared().rules() shouldBe listOf(TYPE_WIDENED)
        }
    }

    context("arrays and alternatives") {
        test("compare their items") {
            val strings = schema { items = schema { types = setOf(STRING) } }

            (schema() to strings).compared().rules() shouldBe listOf("items")
            (strings to schema()).compared().impacts() shouldBe listOf(widenedInRequest)
            (strings to schema { items = schema() }).compared().rules() shouldBe listOf(TYPE_WIDENED)
        }

        test("count their alternatives and compare them in order") {
            val one = listOf(schema { types = setOf(STRING) })
            val two = listOf(schema(), schema())

            val allOf = schema { allOf = one } to schema { allOf = two }
            allOf.compared().rules() shouldBe listOf("all-of", TYPE_WIDENED)
            (schema { anyOf = one } to schema { anyOf = two }).compared().firstImpacts() shouldBe widenedInRequest
            (schema { oneOf = two } to schema { oneOf = one }).compared().firstImpacts() shouldBe narrowedInRequest
        }
    }

    context("references") {
        test("are followed to the schemas they lead to") {
            val before = schema { target = schema { types = setOf(STRING) } }
            val after = schema { target = schema { target = schema() } }

            (before to after).compared().rules() shouldBe listOf(TYPE_WIDENED)
        }

        test("that lead nowhere or too far make the comparison incomplete") {
            val nowhere = schema { isUnresolved = true }
            val far = schema { target = schema { target = schema { target = schema() } } }

            (nowhere to schema()).compared().rules() shouldBe listOf(INCOMPLETE)
            (schema() to nowhere).compared().rules() shouldBe listOf(INCOMPLETE)
            (far to schema()).compared { limits { maximumReferenceHops = 2 } }.rules() shouldBe listOf(INCOMPLETE)
            (far to schema()).compared { limits { maximumReferenceHops = THREE } }.changes.shouldBeEmpty()
        }

        test("that form a cycle end") {
            val before = schema { location = at("/before") }
            before.properties = mapOf("next" to schema { target = before })
            val after = schema {
                location = at("/after")
                types = setOf("object")
            }
            after.properties = mapOf("next" to schema { target = after })

            (before to after).compared().rules() shouldBe listOf(TYPE_NARROWED)
            val unlocated = schema { types = setOf(STRING) }
            (before to unlocated).compared().rules() shouldBe listOf(TYPE_NARROWED, PROPERTY_REMOVED)
        }

        test("that nest deeper than the limits make the comparison incomplete") {
            val before = schema()
            val after = schema()
            var lastBefore = before
            var lastAfter = after
            repeat(TEN.toInt()) {
                val nextBefore = schema()
                val nextAfter = schema()
                lastBefore.items = nextBefore
                lastAfter.items = nextAfter
                lastBefore = nextBefore
                lastAfter = nextAfter
            }

            (before to after).compared { limits { maximumDepth = FIVE.toInt() } }.rules() shouldBe listOf(INCOMPLETE)
            (before to after).compared().changes.shouldBeEmpty()
        }
    }
})
