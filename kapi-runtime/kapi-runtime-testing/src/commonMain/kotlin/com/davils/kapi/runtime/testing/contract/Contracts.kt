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

import com.davils.kapi.runtime.testing.mock.MockApi
import com.davils.kapi.runtime.testing.mock.RecordedExchange
import com.davils.kapi.spec.openapi.Document

/**
 * Creates a check of exchanges against [document], configured by [configure].
 *
 * ```kotlin
 * val check = contractCheck(document) { basePath = "/v1" }
 * check.check(api.exchanges).assertConforms()
 * ```
 *
 * @param document the document, as read by `OpenApi.reader()`.
 * @param configure the base path and the assertion of formats.
 * @return the check, which compiles each schema once and is meant to be reused.
 * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is not valid, such as a base
 * path that does not start with a slash.
 * @since 1.0.0
 */
public fun contractCheck(document: Document, configure: ContractCheckBuilder.() -> Unit = {}): ContractCheck {
    val builder = ContractCheckBuilder(document)
    builder.configure()
    return builder.build()
}

/**
 * Checks [exchanges] and throws if one of them departs from the document; works with every test framework.
 *
 * @param exchanges the exchanges.
 * @return the report, which conforms.
 * @throws AssertionError with the rendered report if there is a finding.
 * @since 1.0.0
 */
public suspend fun ContractCheck.assertConforms(exchanges: List<RecordedExchange>): ContractReport {
    val report = check(exchanges)
    report.assertConforms()
    return report
}

/**
 * Checks every exchange of this mock API against [document] and throws if one of them departs from it.
 *
 * @param document the document.
 * @param configure the base path and the assertion of formats; the base path of the mock API by default.
 * @return the report, which conforms.
 * @throws AssertionError with the rendered report if there is a finding.
 * @since 1.0.0
 */
public suspend fun MockApi.assertConforms(
    document: Document,
    configure: ContractCheckBuilder.() -> Unit = {},
): ContractReport {
    val mockBase = basePath
    val check = contractCheck(document) {
        basePath = mockBase
        configure()
    }
    return check.assertConforms(exchanges)
}
