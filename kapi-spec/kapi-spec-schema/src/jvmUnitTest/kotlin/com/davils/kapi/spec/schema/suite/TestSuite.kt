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

package com.davils.kapi.spec.schema.suite

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.schema.Compilation
import com.davils.kapi.spec.schema.Compiler
import com.davils.kapi.spec.schema.JsonSchema
import com.davils.kapi.spec.schema.OutputFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo

object TestSuite {
    const val COMMIT: String = "245a45abc90190c0bff04d97e6d2b541b4bb9f12"

    private const val REMOTE_BASE: String = "http://localhost:1234/"

    private val OPTIONAL: List<String> = listOf("bignum.json", "ecmascript-regex.json", "float-overflow.json")

    private val root: Path = Paths.get(
        checkNotNull(TestSuite::class.java.getResource("/json-schema-test-suite")).toURI(),
    )

    private val remotes: List<Pair<DocumentUri, JsonElement>> = Files.walk(root.resolve("remotes")).use { paths ->
        paths.filter { it.isRegularFile() && it.extension == "json" }.toList().map { path ->
            val relative = path.relativeTo(root.resolve("remotes")).invariantSeparatorsPathString
            DocumentUri(REMOTE_BASE + relative) to Json.parseToJsonElement(path.readText())
        }
    }

    fun compiler(draft: SuiteDraft): Compiler = JsonSchema.compiler {
        defaultDialect = draft.dialect
        remotes.forEach { (uri, json) -> schema(uri, json) }
    }

    fun files(draft: SuiteDraft): List<Path> {
        val folder = root.resolve("tests").resolve(draft.folder)
        val required = Files.list(folder).use { paths -> paths.filter { it.isRegularFile() }.toList() }
        val optional = OPTIONAL.map { name -> folder.resolve("optional").resolve(name) }.filter { it.isRegularFile() }
        return (required + optional).sortedBy { it.relativeTo(folder).invariantSeparatorsPathString }
    }

    fun nameOf(draft: SuiteDraft, file: Path): String =
        file.relativeTo(root.resolve("tests").resolve(draft.folder)).invariantSeparatorsPathString

    suspend fun run(compiler: Compiler, draft: SuiteDraft, file: Path): SuiteOutcome {
        val groups = Json.parseToJsonElement(file.readText()) as JsonArray
        val unexpected = ArrayList<String>()
        val stale = ArrayList<String>()
        var passed = 0
        var skipped = 0
        groups.forEach { element ->
            val group = element as JsonObject
            val compilation = compiler.compile(checkNotNull(group["schema"]))
            val tests = group["tests"] as JsonArray
            tests.forEach { test ->
                val case = test as JsonObject
                val key = listOf(
                    draft.folder,
                    nameOf(draft, file),
                    textOf(group["description"]),
                    textOf(case["description"]),
                ).joinToString(" / ")
                val expected = (case["valid"] as JsonPrimitive).content == "true"
                val schema = (compilation as? Compilation.Compiled)?.schema
                val actual = schema?.validate(checkNotNull(case["data"]))
                val flag = schema?.validate(checkNotNull(case["data"]), OutputFormat.Flag)
                val isExpected = actual?.isValid == expected && flag?.isValid == expected
                val reason = SkipList.reasonOf(
                    nameOf(draft, file),
                    textOf(group["description"]),
                    textOf(case["description"]),
                )
                skipped += 1.takeIf { reason != null } ?: 0
                passed += 1.takeIf { reason == null && isExpected } ?: 0
                if (reason != null && isExpected) stale += key
                if (reason == null &&
                    !isExpected
                ) {
                    unexpected += "$key: expected valid=$expected, got ${actual ?: compilation}"
                }
            }
        }
        return SuiteOutcome(passed, skipped, unexpected, stale)
    }

    private fun textOf(element: JsonElement?): String = (element as JsonPrimitive).content
}
