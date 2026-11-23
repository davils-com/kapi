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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.openapi.read

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.fixture.configuration
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.core.load.LoadedDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.ByteString
import com.davils.kore.core.annotation.DavilsInternal

private const val PHOTO: String = "aGVsbG8="

class ValuesTest : FunSpec({
    context("a scalar with a tag JSON does not know") {
        test("is kept as its text and reported") {
            val configuration = configuration()
            val findings = Findings(configuration.severities, configuration.limits.maximumDiagnostics)
            val scope = ReadScope(configuration, VersionFamily.V31, findings)
            val node = yamlTagged(PHOTO, YamlTag.BINARY)
            val source = SourceDocument(petstoreUri, PHOTO.length, ByteString())
            val cursor = Cursor(
                scope = scope,
                document = LoadedDocument(PHOTO, node, source),
                pointer = JsonPointer.ROOT,
                node = node,
            )

            Values.of(cursor) shouldBe Value.Text(PHOTO)
            findings.sorted(emptyMap()).map(Diagnostic::code) shouldBe
                listOf(DiagnosticCodes.SOURCE_NON_JSON_VALUE)
        }
    }
})
