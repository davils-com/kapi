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

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.ElementBuilder

/**
 * A schema object: the JSON Schema of a value, which is a [FixedSchema] when it is written as `true` or `false` and
 * a [SchemaDefinition] otherwise.
 *
 * OpenAPI 3.0 uses an extended subset of JSON Schema draft 5; OpenAPI 3.1 and 3.2 use JSON Schema 2020-12 with the
 * vocabulary of OpenAPI. Every schema is immutable and safe to share between threads. The model holds the schema as
 * written; `kapi-spec-schema` validates values against it.
 *
 * @param builder the builder whose state the schema takes over.
 * @since 1.0.0
 */
public sealed class Schema(
    builder: ElementBuilder<*>,
) : Element(builder)
