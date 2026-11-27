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

import com.davils.kapi.runtime.core.codec.CodecsBuilder

/**
 * Builds a codec of `application/xml`, `text/xml` and every `+xml` type.
 *
 * @param configure changes the codec from its defaults.
 * @return the codec.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit lies above its hard ceiling of
 * [XmlCodec].
 * @since 1.2.0
 */
public fun xmlCodec(configure: XmlCodecBuilder.() -> Unit = {}): XmlCodec {
    val builder = XmlCodecBuilder()
    builder.configure()
    return builder.build()
}

/**
 * Adds a codec of `application/xml`, `text/xml` and every `+xml` type to these codecs, such as
 * `codecs { xml { limits { maximumElements = 10_000 } } }`.
 *
 * @receiver the codecs of a client or a server.
 * @param configure changes the codec from its defaults.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit lies above its hard ceiling of
 * [XmlCodec].
 * @throws IllegalStateException if the codecs were built already.
 * @since 1.2.0
 */
public fun CodecsBuilder.xml(configure: XmlCodecBuilder.() -> Unit = {}) {
    codec(xmlCodec(configure))
}
