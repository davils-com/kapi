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

package com.davils.kapi.spec.openapi.model.security

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

@DavilsDsl
public class MutualTlsSchemeBuilder internal constructor() :
    ElementBuilder<MutualTlsScheme>() {
    override val subject: String get() = "openapi mutual tls scheme"

    public var description: String? = null

    override fun assemble(): MutualTlsScheme = MutualTlsScheme(this)
}
