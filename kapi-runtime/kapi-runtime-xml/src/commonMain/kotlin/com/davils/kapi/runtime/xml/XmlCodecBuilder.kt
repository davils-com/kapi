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

package com.davils.kapi.runtime.xml

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.limit.XmlLimitsBuilder
import com.davils.kxml.core.limit.xmlLimits
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

private const val DEFAULT_DEPTH: Int = 64

private const val DEFAULT_INPUT_LENGTH: Int = 16 * 1_024 * 1_024

private val defaultLimits: XmlLimits = xmlLimits {
    maximumDepth = DEFAULT_DEPTH
    maximumInputLength = DEFAULT_INPUT_LENGTH
}

/**
 * Configures an [XmlCodec]: the limits of reading, whether unknown elements are skipped and the serializers module.
 *
 * Document type declarations, and with them entities and external resources, are always refused; no setting allows
 * them. A builder is not thread-safe; the codec it builds is.
 *
 * @since 1.2.0
 */
@DavilsDsl
public class XmlCodecBuilder internal constructor() : ValidatingBuilder<XmlCodec>() {
    override val subject: String get() = "xml codec"

    /**
     * The limits of kxml that every document is read within: the untrusted limits of kxml with at most 64 levels of
     * elements and 16 MiB of input by default. The depth may be at most [XmlCodec.HARD_DEPTH] and the input at most
     * [XmlCodec.HARD_INPUT_LENGTH].
     *
     * @since 1.2.0
     */
    public var limits: XmlLimits = defaultLimits
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether an element or an attribute the type does not declare is skipped, as the JSON codec of kapi skips
     * unknown keys; `true` by default. With `false`, such content is refused.
     *
     * @since 1.2.0
     */
    public var isUnknownElementIgnored: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The module of contextual and polymorphic serializers; empty by default.
     *
     * @since 1.2.0
     */
    public var serializersModule: SerializersModule = EmptySerializersModule()
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Changes [limits], starting from their current value, such as `limits { maximumElements = 10_000 }`.
     *
     * @param configure changes the limits.
     * @throws com.davils.kore.core.validation.report.ValidationException if a limit is not positive or lies above
     * the hard ceiling of kxml.
     * @since 1.2.0
     */
    public fun limits(configure: XmlLimitsBuilder.() -> Unit) {
        limits = xmlLimits(limits, configure)
    }

    override fun assemble(): XmlCodec = XmlCodec(this)
}
