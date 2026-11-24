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

package com.davils.kapi.spec.asyncapi.read.reference

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.read.Cursor
import com.davils.kapi.spec.asyncapi.read.Entry
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.core.model.UriResolution
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference
import com.davils.kore.core.validation.violation.ViolationCode

internal object References {
    fun isReference(cursor: Cursor): Boolean = cursor.entries().any { entry -> entry.key == Keys.REFERENCE }

    fun strict(cursor: Cursor, kind: ReferenceKind): Reference? {
        val ignored = cursor.entries().filterNot { entry -> entry.key == Keys.REFERENCE }
        ignored.forEach { entry ->
            val message = "${shown(entry.key)} stands beside a reference and is ignored"
            entry.report(DiagnosticCodes.REFERENCE_SIBLING, message)
        }
        return read(cursor, kind)
    }

    fun required(cursor: Cursor, kind: ReferenceKind, subject: String): Reference? {
        if (!isReference(cursor)) {
            val message = "$subject must be a reference to ${kind.label}, such as {${'$'}ref: '#/channels/chat'}"
            cursor.report(DiagnosticCodes.REFERENCE_REQUIRED, message)
            return null
        }
        return strict(cursor, kind)
    }

    fun read(cursor: Cursor, kind: ReferenceKind): Reference? {
        val entry = cursor.entries().first { candidate -> candidate.key == Keys.REFERENCE }
        val text = entry.value.text
        if (text == null) entry.value.report(DiagnosticCodes.REFERENCE_SYNTAX, "A reference must be a string")
        val reference = text?.let { written -> referenceOf(cursor, entry.value, written) }
        reference?.let { found -> cursor.scope.references.request(PendingReference(found, kind, entry.value)) }
        return reference
    }

    fun isNested(entry: Entry): Boolean = entry.key == Keys.REFERENCE && entry.value.text != null

    private fun referenceOf(cursor: Cursor, written: Cursor, text: String): Reference? {
        val target = target(written, text) ?: return null
        return Reference(text = text, target = target, location = cursor.location)
    }

    private fun target(written: Cursor, text: String): Target? {
        val base = written.document.uri
        val uri = UriResolution.parse(text)
        val target = uri?.let { parsed -> UriResolution.target(base, parsed) }
        if (target == null) {
            val message = "${shown(text)} names no document and JSON pointer the reader can follow"
            written.report(codeOf(uri), message)
        }
        return target
    }

    private fun codeOf(uri: UriReference?): ViolationCode {
        val pointer = uri?.let(UriResolution::pointerOf)
        val code = DiagnosticCodes.REFERENCE_NO_BASE.takeIf { pointer != null } ?: DiagnosticCodes.REFERENCE_POINTER
        return code.takeIf { uri != null } ?: DiagnosticCodes.REFERENCE_SYNTAX
    }
}
