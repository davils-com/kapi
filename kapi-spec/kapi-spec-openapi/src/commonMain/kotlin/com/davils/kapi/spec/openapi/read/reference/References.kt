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
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.core.model.UriResolution
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.Entry
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference
import com.davils.kore.core.validation.violation.ViolationCode

internal object References {
    private val overrides: Set<String> = setOf(Keys.SUMMARY, Keys.DESCRIPTION)

    fun isReference(cursor: Cursor): Boolean = cursor.entries().any { entry -> entry.key == Keys.REFERENCE }

    fun strict(cursor: Cursor, kind: ReferenceKind): Reference? {
        siblings(cursor, cursor.entries())
        return read(cursor, kind)
    }

    fun read(cursor: Cursor, kind: ReferenceKind): Reference? {
        val entries = cursor.entries()
        val entry = entries.first { candidate -> candidate.key == Keys.REFERENCE }
        val text = entry.value.text
        if (text == null) entry.value.report(DiagnosticCodes.REFERENCE_SYNTAX, "A reference must be a string")
        val target = text?.let { written -> target(entry.value, written) }
        val reference = target?.let { found -> reference(cursor, entries, found) }
        reference?.let { found -> cursor.scope.references.request(PendingReference(found, kind, entry.value)) }
        return reference
    }

    private fun reference(cursor: Cursor, entries: List<Entry>, target: Pair<String, Target>) = Reference(
        text = target.first,
        target = target.second,
        summary = override(cursor, entries, Keys.SUMMARY),
        description = override(cursor, entries, Keys.DESCRIPTION),
        location = cursor.location,
    )

    private fun target(written: Cursor, text: String): Pair<String, Target>? {
        val scope = written.scope
        val base = scope.baseOf(written.document.uri)
        val uri = UriResolution.parse(text)
        val resolved = uri?.let { parsed -> UriResolution.target(base, parsed) }
        val target = resolved?.let { found -> Target(scope.retrievalOf(found.document), found.pointer) }
        if (target == null) {
            val message = "'$text' names no document and JSON pointer the reader can follow"
            written.report(codeOf(base, uri), message)
        }
        return target?.let { found -> text to found }
    }

    private fun codeOf(base: DocumentUri, uri: UriReference?): ViolationCode {
        val failures = listOf(
            DiagnosticCodes.REFERENCE_SYNTAX to (uri == null),
            DiagnosticCodes.REFERENCE_POINTER to (uri?.let(UriResolution::pointerOf) == null),
            DiagnosticCodes.REFERENCE_NO_BASE to (uri?.let { UriResolution.documentOf(base, it) } == null),
        )
        return failures.first { (_, isFailed) -> isFailed }.first
    }

    private fun override(cursor: Cursor, entries: List<Entry>, name: String): String? {
        val entry = entries.firstOrNull { candidate -> candidate.key == name }
        val text = entry?.run { value.text }
        return text.takeIf { cursor.scope.family.isAtLeast(VersionFamily.V31) }
    }

    private fun siblings(cursor: Cursor, entries: List<Entry>) {
        val overriding = overrides.takeIf { cursor.scope.family.isAtLeast(VersionFamily.V31) }
        val allowed = overriding.orEmpty() + Keys.REFERENCE
        val ignored = entries.filterNot { entry -> entry.key in allowed }
        ignored.forEach { entry ->
            val message = "${entry.key} stands beside a reference and is ignored"
            entry.report(DiagnosticCodes.REFERENCE_SIBLING, message)
        }
    }
}
