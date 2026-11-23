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

package com.davils.kapi.spec.core.model

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kaml.document.pointer.JsonPointer

/**
 * The place a reference leads to: a document and a JSON Pointer into it.
 *
 * @property document the document.
 * @property pointer the place inside the document.
 * @since 1.0.0
 */
public class Target(
    public val document: DocumentUri,
    public val pointer: JsonPointer,
) {
    override fun equals(other: Any?): Boolean =
        other is Target && document == other.document && pointer == other.pointer

    override fun hashCode(): Int = 31 * document.hashCode() + pointer.hashCode()

    override fun toString(): String = "$document#$pointer"
}
