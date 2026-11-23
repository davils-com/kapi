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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.testing.contract

import com.davils.kapi.runtime.testing.contract.internal.ExchangeCheck
import com.davils.kapi.runtime.testing.mock.RecordedExchange
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.schema.SchemaLimitsBuilder
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * Checks recorded exchanges against an OpenAPI [document]: that the operation exists by method and path template,
 * that the parameters and bodies validate against the schemas of the operation, that the status is documented, that
 * the required headers are present and that every content type is declared.
 *
 * Schemas are validated by the `JsonSchema` compiler of kapi-spec-schema: with the dialect of OpenAPI 3.0 for a 3.0
 * document and, otherwise, with the `jsonSchemaDialect` of the document if kapi knows it or the base dialect of
 * OpenAPI 3.1. Every schema of the document that a reference names is registered once, and every schema an exchange
 * needs is compiled once, the first time it is needed, and kept for the life of the check, so a check is meant to be
 * reused for many exchanges. Validation stops at the limits of [ContractCheckBuilder.limits], which is reported, never
 * guessed.
 *
 * Parameters are read in their style and explosion and converted by the types of their schemas: a number where the
 * schema allows a number, a boolean where it allows a boolean, an array or an object where it asks for one; a value
 * that cannot be converted stays a string, so that the schema reports it. JSON bodies, of `application/json` and every
 * `+json` type, are validated; records of NDJSON and JSON Lines are validated one by one against an `itemSchema`;
 * other bodies are checked only for their content type. The headers `Accept`, `Content-Type` and `Authorization` are
 * not parameters, as OpenAPI says.
 *
 * A check is safe to use from several coroutines at once.
 *
 * @since 1.0.0
 */
public class ContractCheck internal constructor(builder: ContractCheckBuilder) : Validatable {
    /**
     * The document.
     *
     * @since 1.0.0
     */
    public val document: Document = builder.document

    /**
     * The path in front of the path templates.
     *
     * @since 1.0.0
     */
    public val basePath: String = builder.basePath

    /**
     * Whether `format` asserts.
     *
     * @since 1.0.0
     */
    public val isFormatAsserted: Boolean = builder.isFormatAsserted

    private val limits: SchemaLimitsBuilder.() -> Unit = builder.limits

    private val exchanges: ExchangeCheck = ExchangeCheck(document = document, basePath = basePath) {
        isFormatAsserted = this@ContractCheck.isFormatAsserted
        limits(this@ContractCheck.limits)
    }

    /**
     * Checks [exchanges] in order.
     *
     * @param exchanges the exchanges, such as [com.davils.kapi.runtime.testing.mock.MockApi.exchanges].
     * @return the report, whose findings name the position of their exchange.
     * @since 1.0.0
     */
    public suspend fun check(exchanges: List<RecordedExchange>): ContractReport {
        val findings = exchanges.withIndex().flatMap { (index, exchange) -> this.exchanges.findings(index, exchange) }
        return ContractReport(findings = findings, exchanges = exchanges.size)
    }

    /**
     * Checks one exchange.
     *
     * @param exchange the exchange.
     * @return the report.
     * @since 1.0.0
     */
    public suspend fun check(exchange: RecordedExchange): ContractReport = check(listOf(exchange))

    override fun ValidationScope.validate() {
        val isBasePath = basePath.isEmpty() || (basePath.startsWith('/') && !basePath.endsWith('/'))
        if (!isBasePath) fail(::basePath, "is neither empty nor a path such as /v1 without a slash at its end")
    }

    override fun toString(): String = "ContractCheck(basePath=${SafeText.quoted(basePath)}, " +
        "isFormatAsserted=$isFormatAsserted)"
}
