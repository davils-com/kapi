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

package com.davils.kapi.codegen.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

private const val GENERATE: String = "kapiGeneratePetstore"

private const val RESERVED: String = "codegen.parameter.reserved-header"

private const val REPORTS: String = "build/reports/kapi/petstore"

private const val UNKNOWN: String = "openapi.field.unknown"

private const val PETSTORE: String = "petstore.yaml"

private const val ACCEPT: String = """      operationId: listPets
      parameters:
        - name: Accept
          in: header
          schema:
            type: string
"""

private const val REASON: String = """acknowledge("codegen.parameter.reserved-header", "The server sets Accept")"""

private fun consumerOf(acknowledgements: String, document: String): Consumer {
    val consumer = Consumer.create()
    val script = fixture("diagnostics.gradle")
    val content = script.replace(oldValue = "ACKNOWLEDGEMENTS", newValue = acknowledgements)
    consumer.write(path = "build.gradle", content = content)
    consumer.write(path = "src/main/openapi/$PETSTORE", content = document)
    return consumer
}

private fun warned(): String {
    val document = fixture(PETSTORE)
    return document.replace(oldValue = "      operationId: listPets\n", newValue = ACCEPT)
}

class DiagnosticsBuildTest : FunSpec({
    test("an error fails the generation, the lint and check with the rendered lines and writes both reports") {
        val consumer = consumerOf(acknowledgements = "", document = fixture(PETSTORE) + "inof: x\n")

        val generation = consumer.fail(GENERATE)
        val check = consumer.fail("check")

        generation.output shouldContain "petstore.yaml:84:1: error: [$UNKNOWN] inof is no field of The document"
        check.outcomeOf(":kapiLintPetstore") shouldBe TaskOutcome.FAILED
        check.output shouldContain "does not pass the lint"
        consumer.read("$REPORTS/report.json") shouldContain """"outcome": "Refused""""
        consumer.read("$REPORTS/report.sarif") shouldContain UNKNOWN
        consumer.read("$REPORTS/lint.json") shouldContain """"accepted": false"""
        consumer.read("$REPORTS/lint.sarif") shouldContain UNKNOWN
    }

    test("a warning fails until an acknowledgement with a reason accepts it") {
        val open = consumerOf(acknowledgements = "", document = warned())
        val blank = consumerOf(acknowledgements = """acknowledge("$RESERVED", "")""", document = warned())

        val unacknowledged = open.fail(GENERATE)
        val unexplained = blank.fail(GENERATE)

        unacknowledged.output shouldContain "warning: [$RESERVED]"
        unacknowledged.output shouldContain "acknowledge it with a reason or fix the document"
        unexplained.output shouldContain "The acknowledgement of $RESERVED needs a reason"
    }

    test("an acknowledgement that matches no warning fails, and one that does passes") {
        val stale = """$REASON; acknowledge("openapi.gone", "Fixed long ago")"""
        val unused = consumerOf(acknowledgements = stale, document = warned())
        val passing = consumerOf(acknowledgements = REASON, document = warned())

        val refused = unused.fail(GENERATE)
        val accepted = passing.build(GENERATE, "kapiCheck")

        refused.output shouldContain "The acknowledgement of openapi.gone matches no warning and has to go"
        accepted.outcomeOf(":kapiLintPetstore") shouldBe TaskOutcome.SUCCESS
        passing.read("$REPORTS/lint.json") shouldContain """"accepted": true"""
    }

    test("a name that both an OpenAPI and an AsyncAPI document take fails the build") {
        val consumer = Consumer.create()
        consumer.write(path = "build.gradle", content = fixture("clash.gradle"))

        val result = consumer.fail("tasks")

        result.output shouldContain "The API document chat needs the tasks kapiGenerateChat, kapiLintChat"
    }
})
