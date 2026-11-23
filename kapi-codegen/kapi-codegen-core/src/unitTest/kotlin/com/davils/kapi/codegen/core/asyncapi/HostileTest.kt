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

import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.asyncapi.fixture.Compiler
import com.davils.kapi.codegen.core.asyncapi.fixture.filesOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val HOSTILE: String = "hostile.yaml"

private const val REVERSED: String = "‮"

class HostileTest : FunSpec({
    test("a document with comment ends, templates, keywords and bidi controls yields code that compiles") {
        val compilation = Compiler.compile(filesOf(HOSTILE))

        compilation.messages shouldBe ""
        compilation.isSuccessful shouldBe true
    }

    test("no text of the document leaves its KDoc or its string literal") {
        val files = filesOf(HOSTILE).filter { file -> file.kind == GeneratedFileKind.Kotlin }
        val channels = files.filterNot { file -> "/model/" in file.path }
        val kotlin = channels.joinToString("\n") { file -> file.content }

        channels.shouldNotBeEmpty()
        kotlin shouldNotContain "*/ class Evil"
        kotlin shouldNotContain "*/ fun x()"
        kotlin shouldNotContain REVERSED
        kotlin shouldContain "*&#47; class Evil"
        kotlin shouldContain "\\u202e"
        kotlin shouldContain "&#64;param x"
    }

    test("keywords and invalid characters become identifiers") {
        val kotlin = filesOf(HOSTILE).joinToString("\n") { file -> file.content }

        kotlin shouldContain "public class Object("
        kotlin shouldContain "public class CompanionMessage("
        kotlin shouldContain "`in`: String"
        kotlin shouldContain "forX: String"
        kotlin shouldContain "x: Long? = null"
    }
})
