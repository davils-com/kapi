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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the diagnostics that compiling a schema reports. The codes are stable API.
 *
 * @since 1.0.0
 */
public object DiagnosticCodes {
    /**
     * A value stands where a schema must be but is neither an object nor, where the dialect allows it, a boolean.
     *
     * @since 1.0.0
     */
    public val NOT_A_SCHEMA: ViolationCode = ViolationCode("schema.not-a-schema")

    /**
     * The value of a keyword has the wrong shape, such as a `minLength` that is negative.
     *
     * @since 1.0.0
     */
    public val KEYWORD_INVALID: ViolationCode = ViolationCode("schema.keyword.invalid")

    /**
     * An `$id` or `id` is no URI reference or has a fragment where the dialect forbids one.
     *
     * @since 1.0.0
     */
    public val ID_INVALID: ViolationCode = ViolationCode("schema.id.invalid")

    /**
     * Two schemas have the same identifier or the same anchor.
     *
     * @since 1.0.0
     */
    public val ID_DUPLICATE: ViolationCode = ViolationCode("schema.id.duplicate")

    /**
     * An `$anchor` or `$dynamicAnchor` is no valid name.
     *
     * @since 1.0.0
     */
    public val ANCHOR_INVALID: ViolationCode = ViolationCode("schema.anchor.invalid")

    /**
     * A reference leads to no schema that is known, registered or loaded.
     *
     * @since 1.0.0
     */
    public val REFERENCE_UNRESOLVED: ViolationCode = ViolationCode("schema.reference.unresolved")

    /**
     * The `$schema` names a meta-schema that is neither built in, registered nor loaded.
     *
     * @since 1.0.0
     */
    public val DIALECT_UNKNOWN: ViolationCode = ViolationCode("schema.dialect.unknown")

    /**
     * A meta-schema requires a vocabulary that kapi does not know.
     *
     * @since 1.0.0
     */
    public val VOCABULARY_UNKNOWN: ViolationCode = ViolationCode("schema.vocabulary.unknown")

    /**
     * The [SchemaLoader] failed.
     *
     * @since 1.0.0
     */
    public val LOADER_FAILED: ViolationCode = ViolationCode("schema.loader.failed")

    /**
     * A pattern is not supported by the pattern engine.
     *
     * @since 1.0.0
     */
    public val PATTERN_REFUSED: ViolationCode = ViolationCode("schema.pattern.refused")

    /**
     * The schema exceeds one of the [SchemaLimits].
     *
     * @since 1.0.0
     */
    public val LIMIT_EXCEEDED: ViolationCode = ViolationCode("schema.limit.exceeded")

    /**
     * A YAML schema holds a node that JSON cannot express.
     *
     * @since 1.0.0
     */
    public val NOT_JSON: ViolationCode = ViolationCode("schema.not-json")
}
