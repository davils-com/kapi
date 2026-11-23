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

import com.davils.kapi.codegen.core.GeneratedPlatform
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.asyncapi.fixture.Compiler
import com.davils.kapi.codegen.core.asyncapi.fixture.filesOf
import com.davils.kapi.codegen.core.asyncapi.fixture.generationOf
import com.davils.kapi.codegen.core.asyncapi.fixture.resource
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.codegen.core.options.Visibility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val FEATURES: String = "features.yaml"

private val expected: List<String> = listOf(
    """kind: String = "news"""",
    """kind !in setOf("news", "sport")""",
    "limit: Int",
    "ratio: Double? = null",
    "fresh: Boolean? = null",
    "big: Long? = null",
    "tier: String? = null",
    "xTenant: String",
    """xTenant !in setOf("acme")""",
    """tier != null && tier !in setOf("gold", "silver")""",
    "limitText?.toIntOrNull()",
    "ratioText?.toDoubleOrNull()",
    "freshText?.toBooleanStrictOrNull()",
    "CredentialSource.Certificate",
    """CredentialPlacement.Query("key")""",
    """CredentialPlacement.Cookie("sid")""",
    "CredentialPlacement.Basic",
    """SchemeUse("oauth", CredentialSource.Bearer, setOf("read"))""",
    """SchemeUse("oidc", CredentialSource.Bearer, setOf("openid"))""",
    "NothingSerializer()",
    "ChannelSpec<Nothing, List<String>>",
    "ChannelSpec<JsonElement, Long>",
    """path("/feed/")""",
    """"created" to { json, element ->""",
)

class FeaturesTest : FunSpec({
    test("every parameter, scheme, schema format and direction compiles") {
        val compilation = Compiler.compile(filesOf(FEATURES))

        compilation.messages shouldBe ""
        compilation.isSuccessful shouldBe true
    }

    test("the parameters, schemes and directions are written as the document declares them") {
        val kotlin = filesOf(FEATURES).joinToString("\n") { file -> file.content }

        expected.forEach { text -> kotlin shouldContain text }
        kotlin shouldNotContain "authorization:"
    }

    test("the handshake fields the WebSocket writes itself are reported and skipped") {
        val generation = generationOf(name = FEATURES, text = resource(FEATURES))
        val codes = generation.diagnostics.map { diagnostic -> diagnostic.code }

        codes shouldContain GenerationCodes.HANDSHAKE_RESERVED
        codes shouldNotContain GenerationCodes.MESSAGE_INDISTINCT
    }

    test("the models alone are common code without client or server") {
        val generation = generationOf(name = FEATURES, text = resource(FEATURES)) { artifacts = setOf(Artifact.Models) }
        val files = generation.shouldBeInstanceOf<Generation.Generated>().files
        val paths = files.map { file -> file.path }

        files.map { file -> file.platform }.toSet() shouldBe setOf(GeneratedPlatform.Common)
        paths.none { path -> "/client/" in path || "/server/" in path } shouldBe true
        paths shouldContainAll listOf("com/example/features/channel/FeaturesEventsServerMessage.kt")
    }

    test("an internal generation declares nothing public at the top level") {
        val generation = generationOf(name = FEATURES, text = resource(FEATURES)) {
            visibility = Visibility.Internal
            typePrefix = "My"
        }
        val files = generation.shouldBeInstanceOf<Generation.Generated>().files
        val kotlin = files.filterNot { file -> "/model/" in file.path }.joinToString("\n") { file -> file.content }

        kotlin shouldContain "internal class MyFeaturesFeedChannel("
        kotlin shouldContain "internal class MyFeaturesRoutes("
        kotlin shouldNotContain "\npublic class"
        Compiler.compile(files).isSuccessful shouldBe true
    }
})
