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

package com.davils.kapi.spec.openapi.read.reference

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kore.core.annotation.DavilsInternal

internal object SelfBase {
    fun adopt(root: Cursor) {
        val uri = root.document.uri
        if (root.scope.isAdopted(uri)) return
        val version = field(root, Keys.OPENAPI)?.text?.let(Version::parseOrNull)
        val isCurrent = version?.family?.isAtLeast(VersionFamily.V32) == true
        val written = field(root, Keys.SELF)?.takeIf { isCurrent }
        root.scope.adopt(uri, written?.let(::baseOf))
    }

    private fun field(root: Cursor, name: String): Cursor? {
        val entry = root.entries().firstOrNull { candidate -> candidate.key == name }
        return entry?.value
    }

    private fun baseOf(written: Cursor): DocumentUri? {
        val text = written.text ?: return null
        val base = UriResolution.selfOf(written.document.uri, text)
        if (base == null) {
            val message = "'$text' is no URI reference without a fragment that names this document"
            written.report(DiagnosticCodes.DOCUMENT_SELF, message)
        }
        return base
    }
}
