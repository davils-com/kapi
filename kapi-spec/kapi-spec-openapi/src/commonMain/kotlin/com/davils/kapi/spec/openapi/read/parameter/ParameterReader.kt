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

package com.davils.kapi.spec.openapi.read.parameter

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.HeaderBuilder
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterBuilder
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.content.ContentReader
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.schema.SchemaReader
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object ParameterReader {
    fun parameter(cursor: Cursor): Parameter = cursor.read(Shapes.parameter, ParameterBuilder()) { fields ->
        name = fields.text(Keys.NAME).orEmpty()
        placement = placementOf(fields) ?: placement
        description = fields.text(Keys.DESCRIPTION)
        isRequired = fields.flag(Keys.REQUIRED)
        isDeprecated = fields.flag(Keys.DEPRECATED)
        isEmptyValueAllowed = fields.flag(Keys.ALLOW_EMPTY_VALUE)
        style = styleOf(fields)
        isExploded = fields.flagOrNull(Keys.EXPLODE)
        isReservedAllowed = fields.flag(Keys.ALLOW_RESERVED)
        schema = fields.cursor(Keys.SCHEMA)?.let(SchemaReader::read)
        content.putAll(fields.entries(Keys.CONTENT).mapped(ContentReader::mediaType))
        example = fields.value(Keys.EXAMPLE)
        examples.putAll(ContentReader.examples(fields))
    }

    fun header(cursor: Cursor): Header = cursor.read(Shapes.header, HeaderBuilder()) { fields ->
        description = fields.text(Keys.DESCRIPTION)
        isRequired = fields.flag(Keys.REQUIRED)
        isDeprecated = fields.flag(Keys.DEPRECATED)
        style = styleOf(fields)
        isExploded = fields.flagOrNull(Keys.EXPLODE)
        schema = fields.cursor(Keys.SCHEMA)?.let(SchemaReader::read)
        content.putAll(fields.entries(Keys.CONTENT).mapped(ContentReader::mediaType))
        example = fields.value(Keys.EXAMPLE)
        examples.putAll(ContentReader.examples(fields))
    }

    fun styleOf(fields: Fields): ParameterStyle? {
        val text = fields.text(Keys.STYLE) ?: return null
        val style = ParameterStyle.fromKeyword(text)
        if (style == null) {
            fields.report(Keys.STYLE, DiagnosticCodes.PARAMETER_STYLE, "'$text' is no style of a parameter")
        }
        return style
    }

    private fun placementOf(fields: Fields): ParameterPlacement? {
        val text = fields.text(Keys.IN) ?: return null
        val placement = ParameterPlacement.fromKeyword(text)
        if (placement == null) {
            val message = "'$text' is no place of a parameter, which is path, query, header or cookie"
            fields.report(Keys.IN, DiagnosticCodes.PARAMETER_LOCATION, message)
        }
        return placement
    }
}
