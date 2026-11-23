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

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.openapi.ReaderViolationCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val DIRECTORY_END: String = "/"

private const val DOCUMENT_FOLDER: String = "the folder of the document"

public class ReferencePolicy internal constructor(
    builder: ReferencePolicyBuilder,
) : Validatable {
    public val root: DocumentUri? = builder.root

    public val isExternalAllowed: Boolean = builder.isExternalAllowed

    override fun ValidationScope.validate() {
        val directory = root ?: return
        if (directory.value.endsWith(DIRECTORY_END)) return
        fail(
            ::root,
            "must name a directory and end with a slash, but was '$directory'",
            ReaderViolationCodes.ROOT_NOT_DIRECTORY,
        )
    }

    override fun toString(): String {
        val confinement = root?.value ?: DOCUMENT_FOLDER
        return "ReferencePolicy(root=$confinement, isExternalAllowed=$isExternalAllowed)"
    }

    public companion object {
        public val Default: ReferencePolicy = ReferencePolicyBuilder().build()
    }
}
