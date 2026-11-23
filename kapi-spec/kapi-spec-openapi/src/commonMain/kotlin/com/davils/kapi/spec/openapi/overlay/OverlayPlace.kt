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

package com.davils.kapi.spec.openapi.overlay

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.annotation.DavilsInternal

internal class OverlayPlace(
    val document: LoadedDocument,
    val pointer: JsonPointer,
    val node: YamlNode?,
) {
    fun child(token: String, node: YamlNode?): OverlayPlace = OverlayPlace(document, pointer.child(token), node)

    fun report(findings: Findings, code: ViolationCode, message: String) {
        val location = document.locate(pointer, node)
        findings.report(code = code, message = message, location = location, snippet = document.snippet(location))
    }
}
