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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.asyncapi.fixture.Compiler
import com.davils.kapi.codegen.core.asyncapi.fixture.filesOf
import com.davils.kapi.codegen.core.asyncapi.fixture.rendered
import com.davils.kapi.codegen.core.asyncapi.fixture.resource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

private val documents: List<String> = listOf("chat.yaml", "quotes.yaml")

class GoldenTest : FunSpec({
    documents.forEach { name ->
        val golden = name.replace(oldValue = ".yaml", newValue = ".txt")

        test("the files generated from $name are the golden ones, byte for byte") {
            val actual = rendered(filesOf(name))
            val expected = resource(golden)
            val target = Path.of("build", "golden", golden)
            Files.createDirectories(target.parent)
            Files.writeString(target, actual)

            actual shouldBe expected
        }

        test("the files generated from $name compile with explicit API and warnings as errors") {
            val compilation = Compiler.compile(filesOf(name))

            compilation.messages shouldBe ""
            compilation.isSuccessful shouldBe true
        }
    }
})
