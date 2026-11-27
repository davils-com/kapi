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

import com.davils.kapi.runtime.core.codec.CodecsBuilder

/**
 * Builds a codec of `application/yaml`, `application/x-yaml` and every `+yaml` type.
 *
 * @param configure changes the codec from its defaults.
 * @return the codec.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit lies above its hard ceiling of
 * [YamlCodec].
 * @since 1.2.0
 */
public fun yamlCodec(configure: YamlCodecBuilder.() -> Unit = {}): YamlCodec {
    val builder = YamlCodecBuilder()
    builder.configure()
    return builder.build()
}

/**
 * Adds a codec of `application/yaml`, `application/x-yaml` and every `+yaml` type to these codecs, such as
 * `codecs { yaml() }`; it maps values with the [CodecsBuilder.json] set so far, unless [configure] sets another.
 *
 * @receiver the codecs of a client or a server.
 * @param configure changes the codec from its defaults.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit lies above its hard ceiling of
 * [YamlCodec].
 * @throws IllegalStateException if the codecs were built already.
 * @since 1.2.0
 */
public fun CodecsBuilder.yaml(configure: YamlCodecBuilder.() -> Unit = {}) {
    val shared = json
    codec(
        yamlCodec {
            json = shared
            configure()
        },
    )
}
