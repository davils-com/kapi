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

package com.davils.kapi.runtime.testing.contract.internal

import com.davils.kapi.runtime.testing.contract.ContractCodes
import com.davils.kapi.runtime.testing.contract.ExchangeSide
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.resolve

internal class CheckContext(val document: Document, val schemas: SchemaCache) {
    val shapes: Shapes = Shapes(document)

    val values: ParameterValues = ParameterValues(shapes)

    inline fun <reified T : Element> resolved(
        referable: Referable<T>,
        findings: Findings,
        side: ExchangeSide,
        subject: String,
    ): T? = when (referable) {
        is Referable.Inline -> {
            referable.value
        }

        is Referable.Referenced -> {
            referenced(reference = referable.reference, findings = findings, side = side, subject = subject)
        }
    }

    inline fun <reified T : Element> referenced(
        reference: Reference,
        findings: Findings,
        side: ExchangeSide,
        subject: String,
    ): T? {
        val found = document.resolve(reference) as? T
        if (found == null) unresolved(reference = reference, findings = findings, side = side, subject = subject)
        return found
    }

    fun unresolved(reference: Reference, findings: Findings, side: ExchangeSide, subject: String) {
        findings.add(
            code = ContractCodes.REFERENCE_UNRESOLVED,
            side = side,
            subject = subject,
            message = "The reference ${reference.text} does not resolve",
            location = reference.location,
        )
    }
}
