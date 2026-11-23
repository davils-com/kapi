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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

internal class RuleScope(
    val document: Document,
    val limits: ReadLimits,
    private val findings: Findings,
    documents: List<LoadedDocument>,
) {
    private val loaded: Map<DocumentUri, LoadedDocument> = documents.associateBy(LoadedDocument::uri)

    val family: VersionFamily get() = document.version.family

    val elements: List<Element> by lazy {
        val walked = document.walk().toList()
        walked.distinctBy { element -> element.location ?: element }
    }

    inline fun <reified T : Element> all(): List<T> = elements.filterIsInstance<T>()

    fun report(code: ViolationCode, message: String, element: Element) {
        val location = element.location
        val snippet = location?.let { place -> loaded[place.document]?.snippet(place) }
        findings.report(code = code, message = message, location = location, snippet = snippet)
    }
}
