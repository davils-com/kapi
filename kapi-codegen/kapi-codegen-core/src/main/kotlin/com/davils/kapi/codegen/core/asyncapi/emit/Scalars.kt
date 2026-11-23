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

package com.davils.kapi.codegen.core.asyncapi.emit

import com.davils.kapi.codegen.core.asyncapi.plan.ScalarType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

internal object Scalars {
    fun typeOf(type: ScalarType): TypeName = when (type) {
        ScalarType.Text -> STRING
        ScalarType.Int -> INT
        ScalarType.Long -> LONG
        ScalarType.Double -> DOUBLE
        ScalarType.Flag -> BOOLEAN
    }

    fun parser(type: ScalarType): String? = when (type) {
        ScalarType.Text -> null
        ScalarType.Int -> "toIntOrNull"
        ScalarType.Long -> "toLongOrNull"
        ScalarType.Double -> "toDoubleOrNull"
        ScalarType.Flag -> "toBooleanStrictOrNull"
    }
}
