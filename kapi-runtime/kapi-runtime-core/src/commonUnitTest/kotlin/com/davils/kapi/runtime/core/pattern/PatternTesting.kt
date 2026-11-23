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

package com.davils.kapi.runtime.core.pattern

import io.kotest.matchers.types.shouldBeInstanceOf

internal fun compiled(source: String, limits: PatternLimits = PatternLimits.Default): Pattern {
    val compilation = Pattern.compile(source, limits)
    return compilation.shouldBeInstanceOf<PatternCompilation.Compiled>().pattern
}

internal fun refused(source: String, limits: PatternLimits = PatternLimits.Default): PatternCompilation.Refused {
    val compilation = Pattern.compile(source, limits)
    return compilation.shouldBeInstanceOf<PatternCompilation.Refused>()
}

internal fun matches(source: String, input: String): Boolean = compiled(source).containsMatchIn(input)
