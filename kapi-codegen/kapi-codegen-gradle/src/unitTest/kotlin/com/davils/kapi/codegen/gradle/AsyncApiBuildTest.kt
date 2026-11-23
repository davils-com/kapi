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

private const val COMMON: String = "build/generated/kapi/chat/common/kotlin/com/example/chat"

private const val JVM: String = "build/generated/kapi/chat/jvm/kotlin/com/example/chat"

class AsyncApiBuildTest : FunSpec({
    test("an AsyncAPI chat generates and compiles the models, the client and the server of its channels") {
        val consumer = Consumer.create()
        consumer.write(path = "build.gradle", content = fixture("chat.gradle"))
        consumer.write(path = "src/main/asyncapi/chat.yaml", content = fixture("chat.yaml"))

        val result = consumer.build("compileKotlin", "kapiLint")

        result.outcomeOf(":kapiGenerateChat") shouldBe TaskOutcome.SUCCESS
        result.outcomeOf(":kapiLintChat") shouldBe TaskOutcome.SUCCESS
        result.outcomeOf(":compileKotlin") shouldBe TaskOutcome.SUCCESS
        consumer.files(COMMON).any { path -> path.startsWith("client/") } shouldBe true
        consumer.files(COMMON).any { path -> path.startsWith("model/") } shouldBe true
        consumer.files(JVM).any { path -> path.startsWith("server/") } shouldBe true
        consumer.read("build/reports/kapi/chat/lint.json") shouldContain """"outcome": "Audited""""
        consumer.exists("build/reports/kapi/chat/lint.sarif") shouldBe true
    }
})
