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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.Version
import com.davils.kapi.spec.asyncapi.diagnostic.Findings
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

internal class VersionReader(
    private val document: LoadedDocument,
    private val findings: Findings,
) {
    fun read(): Version? {
        val root = document.root
        if (root !is YamlNode.Mapping) {
            report(DiagnosticCodes.DOCUMENT_NOT_MAPPING, "An AsyncAPI document must be an object", root)
            return null
        }
        return declared(root)
    }

    private fun declared(root: YamlNode): Version? {
        val written = root[Keys.ASYNCAPI]
        val literal = (written as? YamlNode.Scalar)?.value
        val version = literal?.let(Version::parseOrNull)
        if (version == null) report(codeOf(literal), messageOf(literal), written ?: root)
        return version?.also { parsed -> patch(parsed, written) }
    }

    private fun patch(version: Version, written: YamlNode?) {
        if (version.hasPatch) return
        val message = "The version $version names no patch, write it in full such as ${version.literal}.0"
        report(DiagnosticCodes.VERSION_PATCH_MISSING, message, written)
    }

    private fun codeOf(literal: String?): ViolationCode {
        if (literal == null) return DiagnosticCodes.VERSION_MISSING
        return DiagnosticCodes.VERSION_UNSUPPORTED
    }

    private fun messageOf(literal: String?): String {
        if (literal == null) return "The document names no version in the field asyncapi"
        return "AsyncAPI ${shown(literal)} is not read, the reader knows AsyncAPI 3.0"
    }

    private fun report(code: ViolationCode, message: String, node: YamlNode?) {
        val pointer = JsonPointer.ROOT.takeIf { node === document.root } ?: JsonPointer.of(Keys.ASYNCAPI)
        val location = document.locate(pointer, node)
        findings.report(code = code, message = message, location = location, snippet = document.snippet(location))
    }
}
