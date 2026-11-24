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

private const val COMPATIBILITY: String = "kapiCompatibilityPetstore"

private const val TASK: String = ":$COMPATIBILITY"

private const val REMOVED: String = "openapi.compatibility.path-removed"

private const val REPORTS: String = "build/reports/kapi/petstore"

private const val REASON: String = "The extra endpoint was never released"

private const val PETSTORE: String = "petstore.yaml"

private const val EXTRA: String = """paths:
  /extra:
    get:
      operationId: getExtra
      responses:
        '204':
          description: Nothing
"""

private fun consumerOf(compatibility: String): Consumer {
    val consumer = Consumer.create()
    val script = fixture("diagnostics.gradle")
    val content = script.replace(oldValue = "ACKNOWLEDGEMENTS", newValue = compatibility)
    val document = fixture(PETSTORE)
    consumer.write(path = "build.gradle", content = content)
    consumer.write(path = "src/main/openapi/$PETSTORE", content = document)
    val baseline = document.replace(oldValue = "paths:\n", newValue = EXTRA)
    consumer.write(path = "src/main/openapi/petstore-1.0.0.yaml", content = baseline)
    return consumer
}

private fun baseline(acknowledgements: String = ""): String =
    """compatibility { baseline = file("src/main/openapi/petstore-1.0.0.yaml"); $acknowledgements }"""

class CompatibilityBuildTest : FunSpec({
    test("a document without a baseline skips its comparison") {
        val consumer = consumerOf(compatibility = "")

        consumer.build(COMPATIBILITY).outcomeOf(TASK) shouldBe TaskOutcome.SKIPPED
    }

    test("a change that breaks the clients fails the comparison and check with its code and writes both reports") {
        val consumer = consumerOf(compatibility = baseline())

        val comparison = consumer.fail(COMPATIBILITY)
        val check = consumer.fail("check")

        comparison.output shouldContain "warning: [$REMOVED]"
        comparison.output shouldContain "does not stay compatible with its baseline"
        check.outcomeOf(TASK) shouldBe TaskOutcome.FAILED
        consumer.read("$REPORTS/compatibility.json") shouldContain """"outcome": "Compared""""
        consumer.read("$REPORTS/compatibility.sarif") shouldContain REMOVED
    }

    test("a breaking change that is acknowledged with a reason passes, apart from the acknowledgements of generation") {
        val accepted = baseline(acknowledgements = """acknowledge("$REMOVED", "$REASON")""")
        val consumer = consumerOf(compatibility = accepted)

        val result = consumer.build(COMPATIBILITY, "kapiGeneratePetstore")

        result.outcomeOf(TASK) shouldBe TaskOutcome.SUCCESS
        consumer.read("$REPORTS/compatibility.json") shouldContain """"accepted": true"""
    }
})
