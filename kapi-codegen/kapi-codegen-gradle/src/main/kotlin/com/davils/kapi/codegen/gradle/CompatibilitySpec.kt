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

package com.davils.kapi.codegen.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * The compatibility check of an API document against an earlier version of it, such as the one of the last release.
 *
 * Without a [baseline], the task `kapiCompatibility<Name>` is skipped. With one, both versions are read with the
 * confined resolver of the document, without overlays, and compared by the rules of their specification: every change
 * that breaks the [breaking] side fails the build unless it is acknowledged with a reason, and every other change is
 * a note of the reports. The acknowledgements of the check are its own, apart from those of the generation.
 *
 * @since 1.0.0
 */
public abstract class CompatibilitySpec {
    /**
     * The earlier version of the document, inside the `root` of the document; none by default.
     *
     * @since 1.0.0
     */
    public abstract val baseline: RegularFileProperty

    /**
     * For whom a change must not break; [CompatibilitySide.Clients] by default.
     *
     * @since 1.0.0
     */
    public abstract val breaking: Property<CompatibilitySide>

    /**
     * The codes of breaking changes the build accepts, each with its reason.
     *
     * @since 1.0.0
     */
    public abstract val acknowledgements: MapProperty<String, String>

    init {
        breaking.convention(CompatibilitySide.Clients)
    }

    /**
     * Accepts every breaking change of [code], such as `openapi.compatibility.operation-removed`, for [reason].
     *
     * An acknowledgement without a reason or one that matches no change fails the check, so none outlives the change
     * it was written for.
     *
     * @param code the code of the change.
     * @param reason why the change is accepted.
     * @since 1.0.0
     */
    public fun acknowledge(code: String, reason: String) {
        acknowledgements.put(code, reason)
    }
}
