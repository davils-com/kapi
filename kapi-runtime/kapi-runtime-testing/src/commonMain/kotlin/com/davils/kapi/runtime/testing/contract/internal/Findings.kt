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

package com.davils.kapi.runtime.testing.contract.internal

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.runtime.testing.contract.ContractFinding
import com.davils.kapi.runtime.testing.contract.ExchangeSide
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kore.core.validation.violation.ViolationCode

internal class Findings(private val exchange: Int, private val operation: String) {
    private val found: MutableList<ContractFinding> = mutableListOf()

    val all: List<ContractFinding> get() = found.toList()

    fun add(
        code: ViolationCode,
        side: ExchangeSide,
        subject: String,
        message: String,
        location: Location?,
        instance: JsonPointer? = null,
    ) {
        found += ContractFinding(
            code = code,
            message = message,
            exchange = exchange,
            side = side,
            operation = operation,
            subject = subject,
            location = location,
            instance = instance,
        )
    }
}
