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

package com.davils.kapi.runtime.yaml

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.YamlLimitsBuilder
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlinx.serialization.json.Json

private const val DEFAULT_DEPTH: Int = 64

private const val DEFAULT_INPUT_LENGTH: Int = 16 * 1_024 * 1_024

private val defaultLimits: YamlLimits = yamlLimits {
    maximumDepth = DEFAULT_DEPTH
    maximumInputLength = DEFAULT_INPUT_LENGTH
}

/**
 * Configures a [YamlCodec]: the limits of reading and the JSON configuration values are mapped with.
 *
 * A builder is not thread-safe; the codec it builds is.
 *
 * @since 1.2.0
 */
@DavilsDsl
public class YamlCodecBuilder internal constructor() : ValidatingBuilder<YamlCodec>() {
    override val subject: String get() = "yaml codec"

    /**
     * The limits of kaml that every document is read and written within: the untrusted limits of kaml, which bound
     * anchors and the expansion of aliases, with at most 64 levels of nesting and 16 MiB of input by default. The
     * depth may be at most [YamlCodec.HARD_DEPTH] and the input at most [YamlCodec.HARD_INPUT_LENGTH].
     *
     * @since 1.2.0
     */
    public var limits: YamlLimits = defaultLimits
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The JSON configuration that maps values to the data of a document and back, so that a value reads and writes
     * in YAML as it does in JSON; the JSON of [Codecs.Default] by default, and the JSON of the codecs when the codec
     * is added with [yaml][com.davils.kapi.runtime.yaml.yaml].
     *
     * @since 1.2.0
     */
    public var json: Json = Codecs.Default.json
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Changes [limits], starting from their current value, such as `limits { maximumAliasExpansion = 1_000 }`.
     *
     * @param configure changes the limits.
     * @throws com.davils.kore.core.validation.report.ValidationException if a limit is not positive or lies above
     * the hard ceiling of kaml.
     * @since 1.2.0
     */
    public fun limits(configure: YamlLimitsBuilder.() -> Unit) {
        limits = yamlLimits(limits, configure)
    }

    override fun assemble(): YamlCodec = YamlCodec(this)
}
