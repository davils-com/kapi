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

package com.davils.kapi.spec.openapi.model.schema

import com.davils.kapi.spec.core.value.NumberValue

/**
 * A lower or an upper limit of a number, from `minimum` and `exclusiveMinimum` or from `maximum` and
 * `exclusiveMaximum`.
 *
 * In OpenAPI 3.0 `exclusiveMinimum` and `exclusiveMaximum` are flags that make `minimum` and `maximum` exclusive; from
 * OpenAPI 3.1 on they are limits of their own, and a reading keeps the tighter of two limits written together. A
 * bound is immutable and safe to share between threads.
 *
 * @property value the limit.
 * @property isExclusive whether the limit itself is outside the allowed range.
 * @since 1.0.0
 */
public data class Bound(
    public val value: NumberValue,
    public val isExclusive: Boolean = false,
)
