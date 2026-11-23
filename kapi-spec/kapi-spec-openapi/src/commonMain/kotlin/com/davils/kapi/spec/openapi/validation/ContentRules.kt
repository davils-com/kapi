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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.Link
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.info.Tag
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.path.Operation

internal object ContentRules {
    fun check(scope: RuleScope) {
        val tags = scope.document.tags
        val named = tags.groupBy(Tag::name)
        val repeated = named.values.flatMap { same -> same.drop(1) }
        repeated.forEach { tag ->
            scope.report(DiagnosticCodes.TAG_DUPLICATE, "The tag ${tag.name} is declared more than once", tag)
        }
        links(scope)
        examples(scope)
        contents(scope)
    }

    private fun links(scope: RuleScope) {
        val operations = scope.all<Operation>().mapNotNull(Operation::operationId)
        scope.all<Link>().forEach { link ->
            val isNamed = link.operationId != null
            val isReferred = link.operationReference != null
            val hasOne = isNamed != isReferred
            val isKnown = link.operationId?.let { id -> id in operations } != false
            if (!hasOne || !isKnown) {
                val message = "A link names an operation that exists, by operationId or by operationRef"
                scope.report(DiagnosticCodes.LINK_TARGET, message, link)
            }
        }
    }

    private fun examples(scope: RuleScope) {
        val doubled = scope.all<Example>().filter(::isDoubled)
        val parameters = scope.all<Parameter>().filter { parameter ->
            parameter.example != null && parameter.examples.isNotEmpty()
        }
        val headers = scope.all<Header>().filter { header ->
            header.example != null && header.examples.isNotEmpty()
        }
        val media = scope.all<MediaType>().filter { type ->
            type.example != null && type.examples.isNotEmpty()
        }
        (doubled + parameters + headers + media).forEach { element ->
            val message = "An example is given once: as a value or elsewhere, and as example or as examples"
            scope.report(DiagnosticCodes.EXAMPLE_SHAPE, message, element)
        }
    }

    private fun isDoubled(example: Example): Boolean {
        val hasValue = example.value != null
        val hasExternal = example.externalValue != null
        val hasSerialized = example.serializedValue != null
        val conflicts = listOf(
            hasValue && hasExternal,
            hasValue && example.dataValue != null,
            hasSerialized && (hasValue || hasExternal),
        )
        return true in conflicts
    }

    private fun contents(scope: RuleScope) {
        scope.all<Parameter>().forEach { parameter -> content(scope, parameter, parameter.content.keys) }
        scope.all<Header>().forEach { header -> content(scope, header, header.content.keys) }
        scope.all<RequestBody>().forEach { body -> content(scope, body, body.content.keys) }
        scope.all<Response>().forEach { response -> content(scope, response, response.content.keys) }
    }

    private fun content(scope: RuleScope, owner: Element, types: Set<String>) {
        val invalid = types.filterNot(MediaTypeSyntax::isValid)
        invalid.forEach { type ->
            val message = "'$type' is no media type such as text/plain"
            scope.report(DiagnosticCodes.MEDIA_TYPE_INVALID, message, owner)
        }
    }
}
