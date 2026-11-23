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

package com.davils.kapi.runtime.core.codec

import com.davils.kapi.runtime.core.codec.form.FormLimits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlinx.serialization.json.Json

private val defaultJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

/**
 * Configures [Codecs]: the JSON of the application, codecs of its own and the limits of decoding.
 *
 * A builder is not thread-safe; the codecs it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class CodecsBuilder internal constructor() : ValidatingBuilder<Codecs>() {
    private val added: MutableList<Codec> = mutableListOf()

    override val subject: String get() = "codecs"

    /**
     * The JSON configuration that the JSON, CBOR, text, form and octet-stream codecs share; by default it ignores
     * unknown keys, leaves out nulls and writes defaults.
     *
     * @since 1.0.0
     */
    public var json: Json = defaultJson
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most bytes of content [Codecs.decode] reads, at most [Codecs.HARD_CONTENT_BYTES]; 16 MiB by default.
     *
     * @since 1.0.0
     */
    public var maxContentBytes: Int = DEFAULT_CONTENT_BYTES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The deepest nesting of arrays and objects that JSON and CBOR content may have, at most [Codecs.HARD_DEPTH]; 64
     * by default.
     *
     * @since 1.0.0
     */
    public var maxDepth: Int = DEFAULT_DEPTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The limits of the codec of `application/x-www-form-urlencoded`.
     *
     * @since 1.0.0
     */
    public var formLimits: FormLimits = FormLimits.Default
        set(value) {
            checkOpen()
            field = value
        }

    internal val codecs: List<Codec> get() = added

    /**
     * Adds a codec of the application, which is asked before the built-in ones and before codecs added later.
     *
     * @param codec the codec.
     * @since 1.0.0
     */
    public fun codec(codec: Codec) {
        checkOpen()
        added += codec
    }

    override fun assemble(): Codecs = Codecs(this)

    private companion object {
        const val DEFAULT_CONTENT_BYTES: Int = 16 * 1_024 * 1_024
        const val DEFAULT_DEPTH: Int = 64
    }
}

/**
 * Builds the codecs of a client or a server.
 *
 * @param configure changes the codecs from their defaults.
 * @return the codecs.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is not positive or lies above its
 * hard ceiling.
 * @since 1.0.0
 */
public fun codecs(configure: CodecsBuilder.() -> Unit = {}): Codecs {
    val builder = CodecsBuilder()
    builder.configure()
    return builder.build()
}
