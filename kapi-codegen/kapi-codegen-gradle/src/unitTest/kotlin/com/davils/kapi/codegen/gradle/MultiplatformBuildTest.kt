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
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome

private const val COMMON: String = "build/generated/kapi/petstore/common/kotlin/com/example/petstore"

private const val JVM: String = "build/generated/kapi/petstore/jvm/kotlin/com/example/petstore"

class MultiplatformBuildTest : FunSpec({
    test("a multiplatform project gets models and a client in commonMain and a server in jvmMain") {
        val consumer = Consumer.create()
        consumer.write(path = "build.gradle", content = fixture("multiplatform.gradle"))
        consumer.write(path = "src/main/openapi/petstore.yaml", content = fixture("petstore.yaml"))

        val result = consumer.build("compileKotlinJvm", "compileKotlinWasmJs")

        result.outcomeOf(":kapiGeneratePetstore") shouldBe TaskOutcome.SUCCESS
        result.outcomeOf(":compileKotlinJvm") shouldBe TaskOutcome.SUCCESS
        result.outcomeOf(":compileKotlinWasmJs") shouldBe TaskOutcome.SUCCESS
        consumer.files(COMMON) shouldContain "model/PetstorePet.kt"
        consumer.files(COMMON) shouldContain "client/PetstorePetsClient.kt"
        consumer.files(COMMON).none { path -> path.startsWith("server/") } shouldBe true
        consumer.files(JVM).any { path -> path.startsWith("server/") } shouldBe true
        consumer.files("build/classes/kotlin/jvm/main/com/example/petstore") shouldContain "model/PetstorePet.class"
        consumer.files("build/classes/kotlin/wasmJs/main").shouldNotBeEmpty()
    }
})
