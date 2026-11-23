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

package com.davils.kapi.runtime.client.call

import com.davils.kapi.runtime.core.codec.form.FormUrlEncoding
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.annotation.DavilsDsl
import kotlinx.io.bytestring.encodeToByteString

/**
 * Adds the fields of an `application/x-www-form-urlencoded` body, encoded by the form encoder of the runtime core.
 *
 * A builder is not thread-safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FormBuilder internal constructor() {
    private val entries: MutableList<Pair<String, String>> = mutableListOf()

    /**
     * Adds the field [name] with [value].
     *
     * @param name the name.
     * @param value the value.
     * @since 1.0.0
     */
    public fun field(name: String, value: String) {
        entries += name to value
    }

    /**
     * Adds the field [name] once for every value of [values], in their order.
     *
     * @param name the name.
     * @param values the values.
     * @since 1.0.0
     */
    public fun fields(name: String, values: List<String>) {
        values.forEach { value -> field(name, value) }
    }

    internal fun build(): Body {
        val text = FormUrlEncoding.encode(entries.toList())
        return Body(MediaType.FormUrlEncoded, text.encodeToByteString())
    }
}
