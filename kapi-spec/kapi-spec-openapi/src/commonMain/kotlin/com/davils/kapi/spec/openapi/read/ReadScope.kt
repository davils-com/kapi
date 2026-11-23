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

package com.davils.kapi.spec.openapi.read

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.openapi.ReaderConfiguration
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.read.reference.ReferenceQueue
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kore.core.text.TextSnippet
import com.davils.kore.core.validation.violation.ViolationCode

internal class ReadScope(
    val configuration: ReaderConfiguration,
    val family: VersionFamily,
    private val findings: Findings,
) {
    val references: ReferenceQueue = ReferenceQueue()

    private val adopted: OrderedSet<DocumentUri> = OrderedSet()

    private val bases: MutableMap<DocumentUri, DocumentUri> = KeyedMap()

    private val retrievals: MutableMap<DocumentUri, DocumentUri> = KeyedMap()

    fun isAdopted(retrieval: DocumentUri): Boolean = retrieval in adopted

    fun adopt(retrieval: DocumentUri, self: DocumentUri?) {
        adopted.add(retrieval)
        self?.let { base ->
            bases[retrieval] = base
            retrievals.getOrPut(base) { retrieval }
        }
    }

    fun baseOf(retrieval: DocumentUri): DocumentUri = bases[retrieval] ?: retrieval

    fun retrievalOf(uri: DocumentUri): DocumentUri = retrievals[uri] ?: uri

    fun report(code: ViolationCode, message: String, location: Location, snippet: TextSnippet?) {
        findings.report(code = code, message = message, location = location, snippet = snippet)
    }
}
