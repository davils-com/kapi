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

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kapi.spec.openapi.model.schema.Schema

internal data class ParameterShape(
    val name: String,
    val placement: ParameterPlacement,
    val isRequired: Boolean,
    val style: ParameterStyle,
    val isExploded: Boolean,
    val schema: Schema?,
    val content: Map<String, Referable<MediaType>>,
    val location: Location?,
    val subject: String,
) {
    companion object {
        fun of(parameter: Parameter): ParameterShape {
            val style = parameter.style ?: ParameterStyle.defaultFor(parameter.placement) ?: ParameterStyle.Simple
            return ParameterShape(
                name = parameter.name,
                placement = parameter.placement,
                isRequired = parameter.isRequired,
                style = style,
                isExploded = parameter.isExploded ?: (style == ParameterStyle.Form),
                schema = parameter.schema,
                content = parameter.content,
                location = parameter.location,
                subject = "${parameter.placement.keyword} parameter ${parameter.name}",
            )
        }

        fun of(name: String, header: Header): ParameterShape = ParameterShape(
            name = name,
            placement = ParameterPlacement.Header,
            isRequired = header.isRequired,
            style = ParameterStyle.Simple,
            isExploded = header.isExploded == true,
            schema = header.schema,
            content = header.content,
            location = header.location,
            subject = "header $name",
        )
    }
}
