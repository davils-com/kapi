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

package com.davils.kapi.runtime.testing.contract

import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.schema.SchemaLimitsBuilder
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a [ContractCheck] in the block of [contractCheck].
 *
 * A builder is not thread-safe; it is meant for one block of the DSL.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ContractCheckBuilder internal constructor(internal val document: Document) :
    ValidatingBuilder<ContractCheck>() {
    override val subject: String get() = "contract check"

    /**
     * The path in front of the path templates of the document, such as `/v1`; empty by default. The paths of the
     * servers of the document that name no variable are tried as well.
     *
     * @since 1.0.0
     */
    public var basePath: String = ""

    /**
     * Whether `format` asserts, so that a string that is no valid date, for instance, is a finding; `false` by
     * default, as JSON Schema 2020-12 asks.
     *
     * @since 1.0.0
     */
    public var isFormatAsserted: Boolean = false

    internal var limits: SchemaLimitsBuilder.() -> Unit = {}
        private set

    /**
     * Configures the limits of compiling and validating schemas; the defaults of kapi-spec-schema when not called.
     *
     * @param block the limits.
     * @throws IllegalStateException if the check was built already.
     * @since 1.0.0
     */
    public fun limits(block: SchemaLimitsBuilder.() -> Unit) {
        checkOpen()
        limits = block
    }

    override fun assemble(): ContractCheck = ContractCheck(this)
}
