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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Generates Kotlin from one API document in an isolated worker process: `kapiGenerate<Name>`.
 *
 * It empties its output folders first, so a file the document no longer yields never survives, and writes nothing
 * but the reports when the document does not pass.
 *
 * @since 1.0.0
 */
@CacheableTask
public abstract class Generate : DocumentTask() {
    /**
     * The folder of the Kotlin for every platform.
     *
     * @since 1.0.0
     */
    @get:OutputDirectory
    public abstract val commonKotlin: DirectoryProperty

    /**
     * The folder of the Kotlin for the JVM.
     *
     * @since 1.0.0
     */
    @get:OutputDirectory
    public abstract val jvmKotlin: DirectoryProperty

    /**
     * The folder of the resources, such as the provenance of the generation.
     *
     * @since 1.0.0
     */
    @get:OutputDirectory
    public abstract val resources: DirectoryProperty

    /**
     * The file system operations that empty the output folders.
     *
     * @since 1.0.0
     */
    @get:Inject
    protected abstract val files: FileSystemOperations

    init {
        description = "Generates Kotlin from an API document."
    }

    /**
     * Generates.
     *
     * @throws org.gradle.api.GradleException if the document has errors or unacknowledged warnings, or an
     * acknowledgement has no reason or no matching warning.
     * @since 1.0.0
     */
    @TaskAction
    public fun generateKotlin() {
        files.delete { spec -> spec.delete(commonKotlin, jvmKotlin, resources) }
        launch(isLint = false) { outputs ->
            outputs.commonKotlin.set(commonKotlin)
            outputs.jvmKotlin.set(jvmKotlin)
            outputs.resources.set(resources)
        }
    }
}
