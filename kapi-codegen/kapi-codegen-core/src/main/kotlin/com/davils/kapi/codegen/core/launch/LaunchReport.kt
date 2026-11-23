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


@file:OptIn(DavilsInternal::class)

package com.davils.kapi.codegen.core.launch

import com.davils.kore.core.annotation.DavilsInternal

/**
 * What a run of the generator did.
 *
 * @property outcome how the run ended.
 * @property reason why a failed run failed, or `null`.
 * @property diagnostics the diagnostics of reading and generating.
 * @property files the generated files, relative to their output folders.
 * @since 1.0.0
 */
@DavilsInternal
public class LaunchReport(
    public val outcome: LaunchOutcome,
    public val reason: String?,
    public val diagnostics: List<LaunchDiagnostic>,
    public val files: List<String>,
) {
    override fun equals(other: Any?): Boolean = other is LaunchReport && outcome == other.outcome &&
        reason == other.reason && diagnostics == other.diagnostics && files == other.files

    override fun hashCode(): Int = listOf(outcome, reason, diagnostics, files).hashCode()

    override fun toString(): String = "LaunchReport($outcome, ${diagnostics.size} diagnostics, ${files.size} files)"
}
