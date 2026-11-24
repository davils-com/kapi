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

package com.davils.kapi.codegen.core.asyncapi.read

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.codegen.core.pipeline.place
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.core.model.UriResolution
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal

private const val REFERENCE: String = "\$ref"

private const val COMPONENTS: String = "components"

private const val SCHEMAS: String = "schemas"

private const val MOST_HOPS: Int = 32

internal class Targets(private val document: Document) {
    val origin: DocumentUri = document.place.document

    fun channel(key: String): Target = Target(origin, JsonPointer.of("channels", key))

    fun componentKey(target: Target): String? {
        val tokens = target.pointer.tokens
        val isLocal = target.document == origin && tokens.size == COMPONENT_DEPTH
        val isComponent = isLocal && tokens[0] == COMPONENTS && tokens[1] == SCHEMAS
        return tokens.last().takeIf { isComponent }
    }

    fun schemaAt(target: Target): Schema? {
        val key = componentKey(target)
        val schemas = document.components?.schemas.orEmpty()
        val component = key?.let(schemas::get)
        val declared = component?.let { referable -> document.resolve(referable) }
        return declared ?: document.targets[target] as? Schema
    }

    fun targetOf(referable: Referable<Schema>): Target? = when (referable) {
        is Referable.Referenced -> referable.reference.target
        is Referable.Inline -> referable.value.place.run { Target(document, pointer) }
    }

    fun referenceOf(value: Value): String? = Fields.text(value, REFERENCE)

    fun targetOf(text: String, base: DocumentUri): Target? =
        UriResolution.parse(text)?.let { reference -> UriResolution.target(base, reference) }

    fun resolved(value: Value?): Value? = value?.let { found -> resolvedFrom(found, hops = 0) }

    private fun resolvedFrom(value: Value, hops: Int): Value? {
        val text = referenceOf(value) ?: return value
        val base = value.location?.document ?: origin
        val next = targetOf(text, base)?.let(::schemaAt)?.definition
        return next?.takeIf { hops < MOST_HOPS }?.let { found -> resolvedFrom(found, hops + 1) }
    }

    private companion object {
        const val COMPONENT_DEPTH: Int = 3
    }
}
