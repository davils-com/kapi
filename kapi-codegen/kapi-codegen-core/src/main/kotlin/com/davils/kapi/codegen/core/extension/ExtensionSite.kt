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

package com.davils.kapi.codegen.core.extension

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import kotlin.reflect.KClass
import com.davils.kapi.spec.openapi.model.content.MediaType as MediaTypeElement
import com.davils.kapi.spec.openapi.model.parameter.Header as HeaderElement
import com.davils.kapi.spec.openapi.model.parameter.Parameter as ParameterElement
import com.davils.kapi.spec.openapi.model.path.Operation as OperationElement
import com.davils.kapi.spec.openapi.model.path.PathItem as PathItemElement

internal enum class ExtensionSite {
    Schema,
    Operation,
    Parameter,
    Header,
    PathItem,
    MediaType,
    Other,
    ;

    companion object {
        private val sites: Map<KClass<out Element>, ExtensionSite> = mapOf(
            SchemaDefinition::class to Schema,
            OperationElement::class to Operation,
            ParameterElement::class to Parameter,
            HeaderElement::class to Header,
            PathItemElement::class to PathItem,
            MediaTypeElement::class to MediaType,
        )

        fun of(element: Element): ExtensionSite = sites[element::class] ?: Other
    }
}
