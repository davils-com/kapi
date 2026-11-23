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

package com.davils.kapi.spec.core.policy

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val DIRECTORY_END: String = "/"

private const val DOCUMENT_FOLDER: String = "the folder of the document"

/**
 * Which documents references may lead to.
 *
 * A reading loads a referenced document only if it lies inside [root], has the scheme of the root document and is not
 * remote; it refuses `..` segments and their encoded forms, so no reference climbs out of the folder. The documents
 * are loaded by the resolver of the reading, which may confine them further. A policy is immutable and safe to share
 * between threads.
 *
 * @param builder the builder whose values the policy takes over.
 * @since 1.0.0
 */
public class ReferencePolicy internal constructor(
    builder: ReferencePolicyBuilder,
) : Validatable {
    /**
     * The folder every referenced document must lie in, or `null` for the folder of the root document.
     *
     * @since 1.0.0
     */
    public val root: DocumentUri? = builder.root

    /**
     * Whether references may lead to other documents.
     *
     * @since 1.0.0
     */
    public val isExternalAllowed: Boolean = builder.isExternalAllowed

    override fun ValidationScope.validate() {
        val directory = root ?: return
        if (directory.value.endsWith(DIRECTORY_END)) return
        fail(
            ::root,
            "must name a directory and end with a slash, but was ${SafeText.quoted(directory.value)}",
            PolicyCodes.ROOT_NOT_DIRECTORY,
        )
    }

    override fun toString(): String {
        val confinement = root?.value ?: DOCUMENT_FOLDER
        return "ReferencePolicy(root=$confinement, isExternalAllowed=$isExternalAllowed)"
    }

    /**
     * Holds the default policy.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * References may lead to documents in the folder of the root document.
         *
         * @since 1.0.0
         */
        public val Default: ReferencePolicy = ReferencePolicyBuilder().build()
    }
}
