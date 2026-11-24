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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.NormalizeLineEndings

/**
 * Compares one API document with its baseline in an isolated worker process: `kapiCompatibility<Name>`.
 *
 * The task is skipped while the document has no baseline. A change that breaks the [breaking] side fails it unless
 * the `compatibility` block of the document acknowledges its code with a reason; every other change is a note. It
 * writes only the JSON report and the SARIF log.
 *
 * @since 1.0.0
 */
@CacheableTask
public abstract class Compatibility : DocumentTask() {
    /**
     * The earlier version of the document, inside the root of the document.
     *
     * @since 1.0.0
     */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    public abstract val baseline: RegularFileProperty

    /**
     * For whom a change must not break: `Clients`, `Providers` or `Both`.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val breaking: Property<String>

    init {
        description = "Compares an API document with its baseline and fails on a change that breaks."
        onlyIf("the document has a baseline") { task -> (task as Compatibility).baseline.isPresent }
    }

    /**
     * Compares.
     *
     * @throws org.gradle.api.GradleException if a version cannot be read, a change breaks the chosen side without an
     * acknowledgement, or an acknowledgement has no reason or no matching change.
     * @since 1.0.0
     */
    @TaskAction
    public fun compare() {
        val unused = temporaryDir
        val compared = { document: DocumentParameters ->
            document.baseline.set(baseline)
            document.breaking.set(breaking)
        }
        launch(mode = COMPARE, compared = compared) { outputs ->
            outputs.commonKotlin.set(unused)
            outputs.jvmKotlin.set(unused)
            outputs.resources.set(unused)
        }
    }
}
