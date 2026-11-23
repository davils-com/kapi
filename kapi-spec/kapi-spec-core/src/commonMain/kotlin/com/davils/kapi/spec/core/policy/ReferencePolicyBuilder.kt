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

package com.davils.kapi.spec.core.policy

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Builds a [ReferencePolicy].
 *
 * A builder is not safe to share between threads; the policy it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ReferencePolicyBuilder : ValidatingBuilder<ReferencePolicy>() {
    override val subject: String get() = "reference policy"

    /**
     * The folder every referenced document must lie in, ending with a slash, such as `file:///work/api/`; the folder
     * of the root document when `null`.
     *
     * @since 1.0.0
     */
    public var root: DocumentUri? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether references may lead to other documents at all; `true` by default.
     *
     * @since 1.0.0
     */
    public var isExternalAllowed: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): ReferencePolicy = ReferencePolicy(this)
}
