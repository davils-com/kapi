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

package com.davils.kapi.spec.openapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.read.shape.Keys

internal object ReferenceWriter {
    fun <T : Element> referable(referable: Referable<T>, write: (T) -> YamlNode): YamlNode = when (referable) {
        is Referable.Inline -> write(referable.value)
        is Referable.Referenced -> reference(referable.reference)
    }

    fun <T : Element> referables(write: (T) -> YamlNode): (Referable<T>) -> YamlNode =
        { referable -> referable(referable, write) }

    private fun reference(reference: Reference): YamlNode = yamlMapping {
        Keys.REFERENCE to reference.text
        reference.summary?.let { summary -> Keys.SUMMARY to summary }
        reference.description?.let { description -> Keys.DESCRIPTION to description }
    }
}
