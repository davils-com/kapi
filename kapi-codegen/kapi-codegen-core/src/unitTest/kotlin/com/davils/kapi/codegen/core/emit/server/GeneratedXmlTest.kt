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

package com.davils.kapi.codegen.core.emit.server

import com.davils.kapi.codegen.core.fixture.ServedScenario
import com.davils.kapi.codegen.core.fixture.ServedScenarios
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.codecs
import com.davils.kapi.runtime.xml.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val DOCUMENT: String = "xml"

private const val PLACED: String = "placed 1:open:b-1x2[gift, red]:dry & <cool>:true as application/xml"

private const val UNWRITTEN: String = "failed ParameterRefused(body)"

private const val NOT_FOUND: String = "not found"

private const val UNREAD: String = "unexpected 415"

private const val RECEIPT: String = "receipt <receipt>1</receipt>"

private val withXml: Codecs = codecs { xml() }

private val served: List<String> = listOf(
    PLACED,
    "conflict",
    "unexpected 400",
    "found 1:open:b-1x2[gift, red]:dry & <cool>:true as application/json",
    NOT_FOUND,
    "noted 1:open:b-1x2[gift, red]:ring twice:true",
    "noted 1:open:b-1x2[gift, red]:null:true",
    NOT_FOUND,
    RECEIPT,
)

private suspend fun played(server: Codecs, client: Codecs): Any? = ServedScenarios.run(
    ServedScenario(
        document = DOCUMENT,
        server = { codecs = server },
        client = { codecs = client },
    ),
)

class GeneratedXmlTest : FunSpec({
    context("a generated client calling a generated server over XML") {
        test("writes and reads every XML body with the XML codec, and JSON where the document prefers it") {
            played(server = withXml, client = withXml) shouldBe served
        }

        test("does not send a body of XML that no codec of the client writes") {
            played(server = withXml, client = Codecs.Default) shouldBe listOf(
                UNWRITTEN,
                UNWRITTEN,
                UNWRITTEN,
                NOT_FOUND,
                NOT_FOUND,
                UNWRITTEN,
                NOT_FOUND,
                UNWRITTEN,
                RECEIPT,
            )
        }

        test("is refused a body of XML that no codec of the server reads") {
            played(server = Codecs.Default, client = withXml) shouldBe listOf(
                UNREAD,
                UNREAD,
                UNREAD,
                NOT_FOUND,
                NOT_FOUND,
                UNREAD,
                NOT_FOUND,
                UNREAD,
                RECEIPT,
            )
        }
    }
})
