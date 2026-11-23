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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.diagnostic.Findings
import com.davils.kapi.spec.core.collection.associateByKeyed
import com.davils.kapi.spec.core.collection.distinctByKeyed
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

private const val UNLOCATED: Char = '\u0000'

internal class RuleScope(
    val document: Document,
    private val findings: Findings,
    documents: List<LoadedDocument>,
) {
    private val loaded: Map<DocumentUri, LoadedDocument> = documents.associateByKeyed(LoadedDocument::uri)

    val root: DocumentUri = documents.first().uri

    val elements: List<Element> by lazy {
        val walked = document.walk().toList()
        val distinct = walked.withIndex().distinctByKeyed { (index, element) -> placeOf(index, element) }
        distinct.map(IndexedValue<Element>::value)
    }

    inline fun <reified T : Element> all(): List<T> = elements.filterIsInstance<T>()

    inline fun <reified T : Element> resolve(referable: Referable<T>?): T? = when (referable) {
        is Referable.Inline -> referable.value
        is Referable.Referenced -> document.targets[referable.reference.target] as? T
        null -> null
    }

    fun report(code: ViolationCode, message: String, element: Element) {
        report(code, message, element.location)
    }

    fun report(code: ViolationCode, message: String, referable: Referable<*>) {
        val location = when (referable) {
            is Referable.Inline -> referable.value.location
            is Referable.Referenced -> referable.reference.location
        }
        report(code, message, location)
    }

    fun report(code: ViolationCode, message: String, location: Location?) {
        val snippet = location?.let { place -> loaded[place.document]?.snippet(place) }
        findings.report(code = code, message = message, location = location, snippet = snippet)
    }

    private fun placeOf(index: Int, element: Element): String {
        val location = element.location ?: return "$UNLOCATED$index"
        return "${location.document.value}#${location.pointer.text}"
    }
}
