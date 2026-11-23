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

package com.davils.kapi.spec.openapi.read.server

import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kapi.spec.openapi.model.server.ServerBuilder
import com.davils.kapi.spec.openapi.model.server.ServerVariable
import com.davils.kapi.spec.openapi.model.server.ServerVariableBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.mapped
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object ServerReader {
    fun server(cursor: Cursor): Server = cursor.read(Shapes.server, ServerBuilder()) { fields ->
        url = fields.text(Keys.URL).orEmpty()
        description = fields.text(Keys.DESCRIPTION)
        name = fields.text(Keys.NAME)
        variables.putAll(fields.entries(Keys.VARIABLES).mapped(::variable))
    }

    private fun variable(cursor: Cursor): ServerVariable =
        cursor.read(Shapes.serverVariable, ServerVariableBuilder()) { fields ->
            default = fields.text(Keys.DEFAULT).orEmpty()
            enumeration.addAll(fields.texts(Keys.ENUM))
            description = fields.text(Keys.DESCRIPTION)
        }
}
