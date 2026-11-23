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

package com.davils.kapi.spec.openapi.upgrade.fixture

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.load.YamlSettings
import com.davils.kapi.spec.openapi.load.fixture.Loading
import com.davils.kapi.spec.openapi.policy.ReadLimits
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val SWAGGER: String = "swagger: '2.0'"

internal class Upgraded(
    val root: YamlNode,
    val diagnostics: List<Diagnostic>,
) {
    val codes: List<ViolationCode> get() = diagnostics.map(Diagnostic::code)

    val notes: List<Diagnostic> get() = diagnostics.drop(1)
}

internal suspend fun upgraded(vararg lines: String): Upgraded = upgradedOf(lines.asList())

internal suspend fun upgradedOf(lines: List<String>): Upgraded {
    val loading = Loading(Source.Text(yamlOf(listOf(SWAGGER) + lines), petstoreUri))
    val document = loading.loader.root().shouldNotBeNull()
    return Upgraded(document.root, loading.diagnostics)
}

internal fun nodeOf(vararg lines: String): YamlNode = nodeOfLines(lines.asList())

internal fun nodeOfLines(lines: List<String>): YamlNode {
    val yaml = YamlSettings.yamlOf(ReadLimits.Default, hasMergeKeys = false)
    val parsed = yaml.parse(yamlOf(lines)).shouldBeInstanceOf<YamlParseResult.Parsed>()
    return parsed.stream.single
        ?.root
        .shouldNotBeNull()
}
