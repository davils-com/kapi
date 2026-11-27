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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.fixture.Golden
import com.davils.kapi.codegen.core.fixture.KotlinCompilation
import com.davils.kapi.codegen.core.fixture.KotlinStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path

private const val LINE: String = "\n"

private const val BROKEN: String = "class Broken { val missing: Missing = TODO() }"

private val documents: List<String> = listOf("models", "client", "server", "mocks", "hooks", "hostile", "xml")

class GoldenTest : FunSpec({
    context("the Kotlin of a document") {
        documents.forEach { name ->
            test("$name are written as the recorded Kotlin, byte for byte") {
                val actual = Golden.rendered(Golden.generated(name).files)
                val target = Path.of("build", "golden", "$name.txt")
                Files.createDirectories(target.parent)
                Files.writeString(target, actual)

                actual shouldBe Golden.text("$name.txt")
            }

            test("$name compile without a warning under the explicit API") {
                KotlinCompilation.problems(Golden.generated(name).files).shouldBeEmpty()
            }

            test("$name keep the rules of kapi: no comment, no else, no !!, no lateinit and one type per file") {
                KotlinStyle.problems(Golden.generated(name).files).shouldBeEmpty()
            }
        }
    }

    context("the style check") {
        test("finds every rule a file breaks") {
            val lines = listOf(
                "package com.example",
                "// a comment",
                "class First { lateinit var late: String }",
                "object Second { fun pick(text: String?): Int = if (text == null) 0 else text!!.length }",
                "",
            )
            val file = GeneratedFile(
                path = "com/example/Styled.kt",
                kind = GeneratedFileKind.Kotlin,
                content = lines.joinToString(LINE),
            )

            KotlinStyle.problems(listOf(file)) shouldBe listOf(
                "com/example/Styled.kt: a comment",
                "com/example/Styled.kt: else",
                "com/example/Styled.kt: !!",
                "com/example/Styled.kt: lateinit",
                "com/example/Styled.kt: 2 top-level types",
            )
        }
    }

    context("the compilation") {
        test("reports what does not compile and what misses its visibility") {
            val broken = GeneratedFile(
                path = "com/example/Broken.kt",
                kind = GeneratedFileKind.Kotlin,
                content = listOf("package com.example", "", BROKEN, "").joinToString(LINE),
            )
            val problems = KotlinCompilation.problems(listOf(broken)).joinToString(LINE)

            problems shouldContain "Broken.kt:3"
            problems shouldContain "Unresolved reference"
        }
    }
})
