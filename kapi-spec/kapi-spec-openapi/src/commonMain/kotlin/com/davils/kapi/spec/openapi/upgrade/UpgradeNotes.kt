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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.annotation.DavilsInternal

private const val UPGRADED: String =
    "The document is Swagger ${SwaggerKeys.VERSION} and is read as OpenAPI ${SwaggerKeys.UPGRADED_VERSION}"

internal class UpgradeNotes(
    private val document: LoadedDocument,
    private val findings: Findings,
) {
    fun upgraded(root: YamlNode.Mapping) {
        val version = root[Keys.SWAGGER] ?: root
        report(DiagnosticCodes.VERSION_UPGRADED, UPGRADED, Located(version, JsonPointer.of(Keys.SWAGGER)))
    }

    fun lost(place: Located, message: String) {
        report(DiagnosticCodes.UPGRADE_LOSSY, message, place)
    }

    private fun report(code: ViolationCode, message: String, place: Located) {
        val location = document.locate(place.pointer, place.node)
        findings.report(code = code, message = message, location = location, snippet = document.snippet(location))
    }
}
