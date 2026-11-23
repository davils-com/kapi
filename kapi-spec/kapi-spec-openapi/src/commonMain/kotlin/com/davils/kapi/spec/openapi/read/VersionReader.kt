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

package com.davils.kapi.spec.openapi.read

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.load.LoadedDocument
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kore.core.validation.violation.ViolationCode

internal class VersionReader(
    private val document: LoadedDocument,
    private val findings: Findings,
) {
    fun read(): Version? {
        val root = document.root
        if (root !is YamlNode.Mapping) {
            report(DiagnosticCodes.DOCUMENT_NOT_MAPPING, "An OpenAPI document must be an object", root)
            return null
        }
        return declared(root)
    }

    private fun declared(root: YamlNode): Version? {
        val written = root[Keys.OPENAPI]
        val literal = (written as? YamlNode.Scalar)?.value
        val version = literal?.let(Version::parseOrNull)
        if (version == null) report(codeOf(root, literal), messageOf(root, literal), written ?: root)
        return version?.also { parsed -> patch(parsed, written) }
    }

    private fun patch(version: Version, written: YamlNode?) {
        if (version.hasPatch) return
        val message = "The version $version names no patch, write it in full such as ${version.literal}.0"
        report(DiagnosticCodes.VERSION_PATCH_MISSING, message, written)
    }

    private fun codeOf(root: YamlNode, literal: String?): ViolationCode {
        val isSwagger = root[Keys.SWAGGER] != null
        val missing = DiagnosticCodes.VERSION_MISSING.takeIf { literal == null }
        val swagger = DiagnosticCodes.VERSION_SWAGGER.takeIf { isSwagger }
        return swagger ?: missing ?: DiagnosticCodes.VERSION_UNSUPPORTED
    }

    private fun messageOf(root: YamlNode, literal: String?): String {
        val isSwagger = root[Keys.SWAGGER] != null
        val unsupported = literal?.let { written -> "OpenAPI $written is not read, the reader knows 3.0, 3.1 and 3.2" }
        val declared = (root[Keys.SWAGGER] as? YamlNode.Scalar)?.value.orEmpty()
        val swagger = "Swagger $declared is not read, only Swagger 2.0 is upgraded to OpenAPI 3.1".takeIf { isSwagger }
        return swagger ?: unsupported ?: "The document names no version in the field openapi"
    }

    private fun report(code: ViolationCode, message: String, node: YamlNode?) {
        val pointer = JsonPointer.ROOT.takeIf { node === document.root } ?: JsonPointer.of(Keys.OPENAPI)
        val location = document.locate(pointer, node)
        findings.report(code = code, message = message, location = location, snippet = document.snippet(location))
    }
}
