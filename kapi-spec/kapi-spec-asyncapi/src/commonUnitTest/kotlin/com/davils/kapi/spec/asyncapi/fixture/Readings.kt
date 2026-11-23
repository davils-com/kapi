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

package com.davils.kapi.spec.asyncapi.fixture

import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.ReaderBuilder
import com.davils.kapi.spec.asyncapi.Reading
import com.davils.kapi.spec.asyncapi.model.component.Components
import com.davils.kapi.spec.asyncapi.testing.MemoryResolver
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

internal val apiUri: DocumentUri = DocumentUri("memory:/api/asyncapi.yaml")

internal val head: List<String> = listOf("asyncapi: 3.0.0", "info:", "  title: Chat", "  version: 1.0.0")

internal suspend fun reading(lines: List<String>, configure: ReaderBuilder.() -> Unit = {}): Reading {
    val reader = AsyncApi.reader(configure)
    return reader.read(Source.Text(yamlOf(lines), apiUri))
}

internal suspend fun readingOf(body: String, configure: ReaderBuilder.() -> Unit = {}): Reading =
    reading(head + body.trimIndent().lines(), configure)

internal suspend fun readingOf(lines: List<String>): Reading = reading(head + lines)

internal suspend fun readingWith(
    documents: Map<DocumentUri, String>,
    body: String,
    configure: ReaderBuilder.() -> Unit = {},
): Reading = readingOf(body) {
    resolver = MemoryResolver(documents)
    configure()
}

internal suspend fun readingOfText(text: String): Reading = AsyncApi.reader().read(Source.Text(text, apiUri))

internal fun Reading.codes(): List<ViolationCode> = diagnostics.map(Diagnostic::code)

internal fun Reading.errors(): List<ViolationCode> {
    val errors = diagnostics.filter(Diagnostic::isError)
    return errors.map(Diagnostic::code)
}

internal fun Reading.document(): Document {
    val read = shouldBeInstanceOf<Reading.Read>()
    return read.document
}

internal fun Reading.partial(): Document {
    val invalid = shouldBeInstanceOf<Reading.Invalid>()
    return invalid.partial
}

internal fun Document.parts(): Components = components.shouldNotBeNull()

internal fun Reading.parts(): Components = readable().parts()

internal fun Reading.readable(): Document {
    val read = (this as? Reading.Read)?.document
    val partial = (this as? Reading.Invalid)?.partial
    return checkNotNull(read ?: partial)
}

internal suspend fun Document.rewritten(): Document {
    val text = AsyncApi.write(this)
    return readingOfText(text).readable()
}
