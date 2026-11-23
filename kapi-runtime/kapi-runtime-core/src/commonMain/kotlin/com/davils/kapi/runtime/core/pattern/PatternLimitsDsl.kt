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

/**
 * Builds [PatternLimits], such as `patternLimits { maximumSourceLength = 256; maximumStates = 2_000 }`.
 *
 * Every limit left out keeps its default from [PatternLimitsBuilder]. The function is pure and safe to call from any
 * thread; [configure] runs once, on the calling thread.
 *
 * @param configure sets the limits that differ from the defaults.
 * @return the validated limits.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is below 1 or above its hard ceiling,
 * listing every such limit.
 * @since 1.0.0
 */
public fun patternLimits(configure: PatternLimitsBuilder.() -> Unit): PatternLimits {
    val builder = PatternLimitsBuilder()
    builder.configure()
    return builder.build()
}
