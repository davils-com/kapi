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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.problem.ProblemViolation
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.validate

private const val REQUIRED: String = "is required"

private const val MALFORMED: String = "is not what the document declares"

internal class InputFindings {
    private val found: MutableList<ProblemViolation> = mutableListOf()

    var refusal: Refusal? = null
        private set

    val violations: List<ProblemViolation> get() = found.toList()

    val isValid: Boolean get() = found.isEmpty() && refusal == null

    fun <T> absent(path: String, isRequired: Boolean): T? {
        if (isRequired) found += ProblemViolation(path = path, code = ServerCodes.REQUIRED, message = REQUIRED)
        return null
    }

    fun <T> violated(path: String, code: String = ServerCodes.MALFORMED, message: String = MALFORMED): T? {
        found += ProblemViolation(path = path, code = code, message = message)
        return null
    }

    fun <T> refuse(status: Int, code: String, headers: List<Pair<String, String>> = emptyList()): T? {
        refusal = refusal ?: Refusal(status, code, headers)
        return null
    }

    fun <T : Any> checked(value: T, path: String): T {
        val validatable = value as? Validatable ?: return value
        val report = validatable.validate(path)
        found += report.errors.map { violation ->
            ProblemViolation(path = "$path.${violation.path}", code = violation.code.value, message = violation.message)
        }
        return value
    }

    fun <T : Any> decoded(path: String, decoding: Decoding<T>): T? = when (decoding) {
        is Decoding.Failed -> violated(path)
        is Decoding.Decoded -> checked(decoding.value, path)
    }

    data class Refusal(val status: Int, val code: String, val headers: List<Pair<String, String>>)
}
