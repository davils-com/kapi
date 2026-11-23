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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

private const val GENERATE: String = "kapiGeneratePetstore"

private const val COMMON: String = "build/generated/kapi/petstore/common/kotlin/com/example/petstore"

private const val JVM: String = "build/generated/kapi/petstore/jvm/kotlin/com/example/petstore"

private const val CONFIGURATION_CACHE: String = "--configuration-cache"

private const val BUILD_CACHE: String = "--build-cache"

private const val TASK: String = ":$GENERATE"

private fun petstore(buildFile: String, isInProcess: Boolean): Consumer {
    val consumer = Consumer.create(isInProcess)
    consumer.write(path = buildFile, content = fixture(buildFile.replace(oldValue = "build", newValue = "jvm")))
    consumer.write(path = "src/main/openapi/petstore.yaml", content = fixture("petstore.yaml"))
    return consumer
}

class JvmBuildTest : FunSpec({
    test("a JVM project generates a client and a server from OpenAPI in a worker and compiles them") {
        val consumer = petstore(buildFile = "build.gradle", isInProcess = true)

        val result = consumer.build("compileKotlin", "--info")

        result.outcomeOf(TASK) shouldBe TaskOutcome.SUCCESS
        result.outcomeOf(":compileKotlin") shouldBe TaskOutcome.SUCCESS
        result.output shouldContain "Gradle worker daemon"
        consumer.files(COMMON) shouldContain "model/PetstorePet.kt"
        consumer.files(COMMON) shouldContain "client/PetstorePetsClient.kt"
        consumer.files(JVM).any { path -> path.startsWith("server/") } shouldBe true
        consumer.files("build/classes/kotlin/main/com/example/petstore") shouldContain "model/PetstorePet.class"
        consumer.read("build/reports/kapi/petstore/report.json") shouldContain """"outcome": "Generated""""
        consumer.read("build/reports/kapi/petstore/report.sarif") shouldContain """"version": "2.1.0""""
    }

    test("a second run is up to date, reuses the configuration cache and the build cache relocates the outputs") {
        val consumer = petstore(buildFile = "build.gradle.kts", isInProcess = false)
        consumer.build(GENERATE, CONFIGURATION_CACHE, BUILD_CACHE)

        val second = consumer.build(GENERATE, CONFIGURATION_CACHE, BUILD_CACHE)
        val moved = consumer.copy()
        val relocated = moved.build(GENERATE, CONFIGURATION_CACHE, BUILD_CACHE)

        second.output shouldContain "Reusing configuration cache."
        second.outcomeOf(TASK) shouldBe TaskOutcome.UP_TO_DATE
        relocated.outcomeOf(TASK) shouldBe TaskOutcome.FROM_CACHE
        moved.files(COMMON) shouldBe consumer.files(COMMON)
    }
})
