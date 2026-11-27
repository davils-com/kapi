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

package com.davils.kapi.runtime.xml

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.codec.codecs
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.xml.fixture.FailingSerializer
import com.davils.kapi.runtime.xml.fixture.Node
import com.davils.kapi.runtime.xml.fixture.Order
import com.davils.kapi.runtime.xml.fixture.Spaced
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.modules.EmptySerializersModule

private const val XML: String = "application/xml"

private const val ORDER: String = "<Order><id>7</id><items>book</items><items>pen</items></Order>"

private const val DEPTH: Int = 64

private const val SMALL: Int = 100

private val order: Order = Order(id = 7, items = listOf("book", "pen"))

private val codec: XmlCodec = xmlCodec()

private fun decoded(text: String, codec: XmlCodec = com.davils.kapi.runtime.xml.codec): Decoding<Order> =
    codec.decode(Order.serializer(), text.encodeToByteString())

private fun nested(levels: Int): String = "<a>".repeat(levels) + "</a>".repeat(levels)

private fun chain(levels: Int): Node = (2..levels).fold(Node()) { inner, _ -> Node(inner) }

private fun failed(decoding: Decoding<*>): String = decoding.shouldBeInstanceOf<Decoding.Failed>().reason

class XmlCodecTest : FunSpec({
    context("the media types") {
        test("are application/xml, text/xml and every +xml type") {
            val codecs = codecs { xml() }

            codecs.supports(XML) shouldBe true
            codecs.supports("text/xml; charset=utf-8") shouldBe true
            codecs.supports("application/atom+xml") shouldBe true
            codecs.supports("application/json") shouldBe true
            codecs.supports("image/svg+xml") shouldBe false
            codecs.codecFor(XML).shouldBeInstanceOf<XmlCodec>()
        }

        test("are only read once the codec is registered") {
            Codecs.Default.supports(XML) shouldBe false
        }
    }

    context("writing") {
        test("writes a class as an element named after it, in UTF-8 with a declaration") {
            val text = codec.encode(Order.serializer(), order).decodeToString()

            text shouldStartWith "<?xml"
            text shouldContain ORDER
        }

        test("reads back what it wrote") {
            val bytes = codec.encode(Order.serializer(), order.copy(note = "gift"))

            codec.decode(Order.serializer(), bytes) shouldBe Decoding.Decoded(order.copy(note = "gift"))
        }

        test("throws for a value XML cannot carry") {
            shouldThrow<IllegalArgumentException> { codec.encode(Spaced.serializer(), Spaced("value")) }
            shouldThrow<IllegalStateException> { codec.encode(FailingSerializer, "value") }
        }
    }

    context("reading") {
        test("reads repeated elements as a list and leaves out what is absent") {
            decoded(ORDER) shouldBe Decoding.Decoded(order)
        }

        test("skips unknown elements by default and refuses them when asked") {
            val text = "<Order><id>7</id><extra>x</extra></Order>"
            val strict = xmlCodec { isUnknownElementIgnored = false }

            decoded(text) shouldBe Decoding.Decoded(Order(id = 7))
            decoded(text, strict).shouldBeInstanceOf<Decoding.Failed>()
        }

        test("refuses another root element and content that does not fit the type") {
            decoded("<Invoice><id>7</id></Invoice>").shouldBeInstanceOf<Decoding.Failed>()
            decoded("<Order><id>seven</id></Order>").shouldBeInstanceOf<Decoding.Failed>()
            decoded("<Order/>").shouldBeInstanceOf<Decoding.Failed>()
        }

        test("refuses truncated, malformed and empty content") {
            decoded("<Order><id>7</id>").shouldBeInstanceOf<Decoding.Failed>()
            decoded("<Order><id>7</Order>").shouldBeInstanceOf<Decoding.Failed>()
            decoded("not xml").shouldBeInstanceOf<Decoding.Failed>()
            decoded("").shouldBeInstanceOf<Decoding.Failed>()
            codec.decode(Order.serializer(), ByteString(byteArrayOf(-1, -1, 0))).shouldBeInstanceOf<Decoding.Failed>()
        }

        test("answers a serializer that throws with a failed decoding") {
            failed(codec.decode(FailingSerializer, "<Failing>x</Failing>".encodeToByteString())) shouldContain "XML"
        }

        test("decodes the charset the media type names, before the declaration") {
            val text = """<?xml version="1.0" encoding="UTF-8"?><Order><id>7</id><note>café</note></Order>"""
            val latin = text.map { char -> char.code.toByte() }.toByteArray()
            val type = MediaType.parse("application/xml; charset=iso-8859-1")

            codec.decode(Order.serializer(), ByteString(latin), type) shouldBe
                Decoding.Decoded(Order(id = 7, note = "café"))
            val unlabelled = codec.decode(Order.serializer(), ByteString(latin), MediaType.parse(XML))

            unlabelled.shouldBeInstanceOf<Decoding.Failed>()
        }

        test("names the first error, not a warning before it") {
            val text = """<?xml version="1.0" encoding="UTF-8"?><Order><id>7</id>"""
            val type = MediaType.parse("application/xml; charset=iso-8859-1")
            val reason = failed(codec.decode(Order.serializer(), text.encodeToByteString(), type))

            reason.isNotBlank() shouldBe true
            reason shouldNotContain "overrid"
        }

        test("refuses a charset kxml does not read") {
            val type = MediaType.parse("application/xml; charset=ebcdic")

            codec.decode(Order.serializer(), ORDER.encodeToByteString(), type).shouldBeInstanceOf<Decoding.Failed>()
        }
    }

    context("hostile content") {
        test("refuses every document type declaration, so no entity is expanded") {
            val internal = """<!DOCTYPE Order [<!ENTITY a "7">]><Order><id>&a;</id></Order>"""
            val external = """<!DOCTYPE Order [<!ENTITY a SYSTEM "file:///etc/passwd">]><Order><id>&a;</id></Order>"""
            val laughs = """<!DOCTYPE Order [<!ENTITY a "aaaa"><!ENTITY b "&a;&a;&a;&a;">]>""" +
                "<Order><id>&b;</id></Order>"

            listOf(internal, external, laughs).forEach { text -> decoded(text).shouldBeInstanceOf<Decoding.Failed>() }
        }

        test("reads elements nested at the depth limit and refuses one level more") {
            val atLimit = codec.decode(Node.serializer(), nested(DEPTH).encodeToByteString())
            val beyond = codec.decode(Node.serializer(), nested(DEPTH + 1).encodeToByteString())

            atLimit shouldBe Decoding.Decoded(chain(DEPTH))
            beyond.shouldBeInstanceOf<Decoding.Failed>()
        }

        test("refuses a document longer than its limit") {
            val small = xmlCodec { limits { maximumInputLength = SMALL } }
            val padding = " ".repeat(SMALL)

            decoded(ORDER, small) shouldBe Decoding.Decoded(order)
            decoded(ORDER + padding, small).shouldBeInstanceOf<Decoding.Failed>()
        }
    }

    context("the settings") {
        test("refuse limits above the ceilings of kapi") {
            shouldThrow<ValidationException> { xmlCodec { limits { maximumDepth = XmlCodec.HARD_DEPTH + 1 } } }
        }

        test("are kept and shown") {
            val configured = xmlCodec {
                isUnknownElementIgnored = false
                serializersModule = EmptySerializersModule()
            }

            configured.isUnknownElementIgnored shouldBe false
            configured.limits.maximumDepth shouldBe DEPTH
            configured.serializersModule shouldBe EmptySerializersModule()
            configured.toString() shouldContain "application/*+xml"
        }

        test("cannot change once the codec is built") {
            val builder = XmlCodecBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.isUnknownElementIgnored = false }
            shouldThrow<IllegalStateException> { builder.limits = builder.limits }
            shouldThrow<IllegalStateException> { builder.serializersModule = EmptySerializersModule() }
        }
    }
})
