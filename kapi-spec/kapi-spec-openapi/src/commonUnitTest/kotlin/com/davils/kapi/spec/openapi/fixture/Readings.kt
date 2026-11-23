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

package com.davils.kapi.spec.openapi.fixture

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.ReaderBuilder
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.openapi.testing.MemoryResolver
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.matchers.types.shouldBeInstanceOf

internal val petstoreUri: DocumentUri = DocumentUri("memory:/api/petstore.yaml")

internal val head31: List<String> = listOf("openapi: 3.1.0", "info:", "  title: Petstore", "  version: 1.0.0")

internal const val EMPTY_PATHS: String = "paths: {}"

internal val head30: List<String> = listOf("openapi: 3.0.3") + head31.drop(1) + EMPTY_PATHS

private val sections: List<String> = listOf("paths:", "components:", "webhooks:")

internal suspend fun reading(lines: List<String>, configure: ReaderBuilder.() -> Unit = {}): Reading {
    val reader = OpenApi.reader(configure)
    return reader.read(Source.Text(yamlOf(lines), petstoreUri))
}

internal suspend fun readingWith(
    documents: Map<DocumentUri, String>,
    lines: List<String>,
    configure: ReaderBuilder.() -> Unit = {},
): Reading = reading(lines) {
    resolver = MemoryResolver(documents)
    configure()
}

internal suspend fun readingOf(resource: String): Reading {
    val reader = OpenApi.reader()
    return reader.read(Source.Text(Resources.text(resource), petstoreUri))
}

internal suspend fun reading31(vararg lines: String): Reading = reading31(lines.asList())

internal suspend fun reading30(vararg lines: String): Reading = reading(head30 + lines)

internal suspend fun reading31(lines: List<String>): Reading {
    val hasSection = lines.any { line -> sections.any(line::startsWith) }
    val completion = listOf(EMPTY_PATHS).filterNot { hasSection }
    return reading(head31 + lines + completion)
}

internal fun Reading.errors(): List<ViolationCode> {
    val errors = diagnostics.filter(Diagnostic::isError)
    return errors.map(Diagnostic::code)
}

internal suspend fun reading30(lines: List<String>): Reading = reading(head30 + lines)

internal fun Reading.codes(): List<ViolationCode> = diagnostics.map(Diagnostic::code)

internal fun Reading.document(): Document {
    val read = shouldBeInstanceOf<Reading.Read>()
    return read.document
}

internal fun Reading.partial(): Document {
    val invalid = shouldBeInstanceOf<Reading.Invalid>()
    return invalid.partial
}
