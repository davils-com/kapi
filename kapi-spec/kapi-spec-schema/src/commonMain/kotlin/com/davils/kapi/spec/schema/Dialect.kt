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

package com.davils.kapi.spec.schema

/**
 * A dialect of JSON Schema that kapi compiles: the keywords a schema may use and what each of them means.
 *
 * A schema names its dialect with `$schema`; a schema without `$schema` uses the default dialect of the
 * [Compiler]. OpenAPI 3.0 has no `$schema`, so its Schema Objects are compiled with [OpenApi30] as the default
 * dialect.
 *
 * @property uri the URI of the meta-schema that names this dialect in `$schema`, or `null` for [OpenApi30], which
 * has none.
 * @since 1.0.0
 */
public enum class Dialect(public val uri: String?) {
    /**
     * JSON Schema draft-04: `id`, `definitions`, the array form of `items` with `additionalItems`, `dependencies`
     * and the boolean `exclusiveMaximum` and `exclusiveMinimum`. A `$ref` replaces its sibling keywords, and only a
     * number without a fraction and an exponent is an integer.
     *
     * @since 1.0.0
     */
    Draft04("http://json-schema.org/draft-04/schema#"),

    /**
     * JSON Schema draft-06: `$id`, `const`, `contains`, `propertyNames`, boolean schemas and the numeric
     * `exclusiveMaximum` and `exclusiveMinimum`. A `$ref` replaces its sibling keywords.
     *
     * @since 1.0.0
     */
    Draft06("http://json-schema.org/draft-06/schema#"),

    /**
     * JSON Schema draft-07: draft-06 with `if`, `then` and `else`. A `$ref` replaces its sibling keywords.
     *
     * @since 1.0.0
     */
    Draft07("http://json-schema.org/draft-07/schema#"),

    /**
     * JSON Schema 2019-09 with its vocabularies: `$anchor`, `$recursiveRef`, `$recursiveAnchor`, `$defs`,
     * `dependentSchemas`, `dependentRequired`, `minContains`, `maxContains`, `unevaluatedItems` and
     * `unevaluatedProperties`.
     *
     * @since 1.0.0
     */
    Draft201909("https://json-schema.org/draft/2019-09/schema"),

    /**
     * JSON Schema 2020-12 with all of its vocabularies: `prefixItems`, `$dynamicRef` and `$dynamicAnchor` among
     * them.
     *
     * @since 1.0.0
     */
    Draft202012("https://json-schema.org/draft/2020-12/schema"),

    /**
     * The Schema Object of OpenAPI 3.0: a subset of draft-04 with `nullable`, a `type` that is a single string and
     * `discriminator`, `xml`, `externalDocs`, `example` and `deprecated` as annotations. A `$ref` replaces its sibling
     * keywords.
     *
     * @since 1.0.0
     */
    OpenApi30(null),

    /**
     * The base dialect of OpenAPI 3.1: JSON Schema 2020-12 with the OpenAPI vocabulary, whose `discriminator`, `xml`,
     * `externalDocs` and `example` are annotations.
     *
     * @since 1.0.0
     */
    OpenApi31("https://spec.openapis.org/oas/3.1/dialect/base"),
}
