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

package com.davils.kapi.spec.openapi.policy

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kore.core.validation.violation.ViolationCode

@DavilsDsl
public class SeverityPolicyBuilder internal constructor() :
    ValidatingBuilder<SeverityPolicy>() {
    private val declared: MutableMap<String, String> = linkedMapOf()

    override val subject: String get() = "openapi severity policy"

    public var areWarningsErrors: Boolean = false

    internal val downgrades: Map<String, String> get() = declared

    public fun downgrade(code: ViolationCode, reason: String) {
        declared[code.value] = reason
    }

    override fun assemble(): SeverityPolicy = SeverityPolicy(this)
}
