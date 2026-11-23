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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.asyncapi.DocumentBuilder
import com.davils.kapi.spec.asyncapi.diagnostic.Findings
import com.davils.kapi.spec.asyncapi.fixture.apiUri
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.channel.ChannelBuilder
import com.davils.kapi.spec.asyncapi.model.operation.OperationBuilder
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString

private const val TEXT: String = "asyncapi: 3.0.0\n"

private const val MAXIMUM_DIAGNOSTICS: Int = 10

private fun loaded(): LoadedDocument {
    val parsed = Yaml.Default.parse(TEXT).shouldBeInstanceOf<YamlParseResult.Parsed>()
    val root = parsed.stream.single.shouldNotBeNull()
    return LoadedDocument(TEXT, root.root, SourceDocument(apiUri, TEXT.length, ByteString()))
}

class RuleScopeTest : FunSpec({
    context("the elements a rule sees") {
        test("keep every element built in code, which has no place to tell it apart") {
            val builder = DocumentBuilder()
            builder.channels["first"] = Referable.Inline(ChannelBuilder().build())
            builder.channels["second"] = Referable.Inline(ChannelBuilder().build())
            val findings = Findings(SeverityPolicy.Default, MAXIMUM_DIAGNOSTICS)

            val scope = RuleScope(document = builder.build(), findings = findings, documents = listOf(loaded()))

            scope.all<Channel>().size shouldBe 2
        }

        test("let the keys of operations pass an operation without a place or a target") {
            val builder = DocumentBuilder()
            builder.operations["inline"] = Referable.Inline(OperationBuilder().build())
            val nowhere = Reference("#/nowhere", Target(apiUri, JsonPointer.ROOT))
            builder.operations["dangling"] = Referable.Referenced(nowhere)
            val findings = Findings(SeverityPolicy.Default, MAXIMUM_DIAGNOSTICS)
            val scope = RuleScope(document = builder.build(), findings = findings, documents = listOf(loaded()))

            OperationRules.check(scope)

            findings.sorted(mapOf(apiUri to 0)).shouldBeEmpty()
        }
    }
})
