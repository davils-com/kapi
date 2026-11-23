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

package com.davils.kapi.spec.core.source

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.io.bytestring.ByteString

/**
 * What a [Resolver] answers for a document URI.
 *
 * @since 1.0.0
 */
public sealed interface Resolution {
    /**
     * The document was found.
     *
     * @property content the bytes of the document, which the reading decodes as strict UTF-8 within its limits.
     * @since 1.0.0
     */
    public class Found(public val content: ByteString) : Resolution {
        override fun equals(other: Any?): Boolean = other is Found && content == other.content

        override fun hashCode(): Int = content.hashCode()

        override fun toString(): String = "Found(${content.size} bytes)"
    }

    /**
     * There is no document at the URI.
     *
     * @since 1.0.0
     */
    public data object Missing : Resolution

    /**
     * The resolver refuses to load the document, such as one outside its root or one it does not serve.
     *
     * @property refusal why the resolver refuses.
     * @since 1.0.0
     */
    public class Refused(public val refusal: Refusal) : Resolution {
        override fun equals(other: Any?): Boolean = other is Refused && refusal == other.refusal

        override fun hashCode(): Int = refusal.hashCode()

        override fun toString(): String = "Refused($refusal)"
    }

    /**
     * Loading the document failed, such as on an I/O error.
     *
     * @property reason why loading failed, in words of the resolver; it is shown through [SafeText] only.
     * @since 1.0.0
     */
    public class Failed(public val reason: String) : Resolution {
        override fun equals(other: Any?): Boolean = other is Failed && reason == other.reason

        override fun hashCode(): Int = reason.hashCode()

        override fun toString(): String = "Failed(${SafeText.quoted(reason)})"
    }
}
