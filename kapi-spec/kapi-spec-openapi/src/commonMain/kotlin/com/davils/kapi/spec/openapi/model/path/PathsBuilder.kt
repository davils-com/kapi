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

package com.davils.kapi.spec.openapi.model.path

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [Paths], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the paths it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PathsBuilder internal constructor() : ElementBuilder<Paths>() {
    override val subject: String get() = "openapi paths"

    internal val items: MutableMap<PathTemplate, PathItem> = linkedMapOf()

    override fun assemble(): Paths = Paths(this)
}
