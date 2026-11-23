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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

private const val MAXIMUM_HEAP: String = "512m"

/**
 * Generates Kotlin from one API document in an isolated worker process, then judges the diagnostics: every error and
 * every warning without an acknowledgement fails the task. The task is cacheable and relocatable: it reads the
 * document and every file below its root by their relative paths.
 *
 * @since 1.0.0
 */
@CacheableTask
public abstract class Generate @Inject constructor(
    private val workers: WorkerExecutor,
    private val files: FileSystemOperations,
) : DefaultTask() {
    /**
     * The document to read.
     *
     * @since 1.0.0
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    public abstract val specification: RegularFileProperty

    /**
     * The folder the document and every document it references must stay inside.
     *
     * @since 1.0.0
     */
    @get:Internal
    public abstract val root: DirectoryProperty

    /**
     * Every file below [root], which the document may reference.
     *
     * @since 1.0.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    @get:IgnoreEmptyDirectories
    public abstract val sources: ConfigurableFileCollection

    /**
     * The class path of the generator.
     *
     * @since 1.0.0
     */
    @get:Classpath
    public abstract val generatorClasspath: ConfigurableFileCollection

    /**
     * The Java launcher of the worker, or the one Gradle runs on when absent.
     *
     * @since 1.0.0
     */
    @get:Nested
    @get:Optional
    public abstract val launcher: Property<JavaLauncher>

    /**
     * The overlays to apply, relative to [root], in order.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val overlays: ListProperty<String>

    /**
     * The package of the generated code.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val packageName: Property<String>

    /**
     * The name of the API.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val apiName: Property<String>

    /**
     * The prefix of every generated type.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val typePrefix: Property<String>

    /**
     * The names of the artifacts to generate.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val artifacts: SetProperty<String>

    /**
     * The name of the visibility of the generated code.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val visibility: Property<String>

    /**
     * The version of kapi that generates.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val generatorVersion: Property<String>

    /**
     * The codes of warnings the build accepts, each with its reason.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val acknowledgements: MapProperty<String, String>

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
     * The folder of the resources.
     *
     * @since 1.0.0
     */
    @get:OutputDirectory
    public abstract val resources: DirectoryProperty

    /**
     * The JSON report of the run.
     *
     * @since 1.0.0
     */
    @get:OutputFile
    public abstract val report: RegularFileProperty

    /**
     * The SARIF 2.1.0 log of the diagnostics.
     *
     * @since 1.0.0
     */
    @get:OutputFile
    public abstract val sarif: RegularFileProperty

    init {
        group = GROUP
        description = "Generates Kotlin from an API document."
    }

    /**
     * Generates.
     *
     * @throws org.gradle.api.GradleException if the document has errors or unacknowledged warnings.
     * @since 1.0.0
     */
    @TaskAction
    public fun generate() {
        files.delete { spec -> spec.delete(commonKotlin, jvmKotlin, resources) }
        val queue = workers.processIsolation { isolation ->
            isolation.classpath.from(generatorClasspath)
            isolation.forkOptions { fork ->
                fork.maxHeapSize = MAXIMUM_HEAP
                launcher.orNull?.let { chosen -> fork.executable(chosen.executablePath.asFile) }
            }
        }
        queue.submit(GenerateAction::class.java) { parameters ->
            parameters.specification.set(this@Generate.specification)
            parameters.root.set(this@Generate.root)
            parameters.overlays.set(this@Generate.overlays)
            parameters.commonKotlin.set(this@Generate.commonKotlin)
            parameters.jvmKotlin.set(this@Generate.jvmKotlin)
            parameters.resources.set(this@Generate.resources)
            parameters.report.set(this@Generate.report)
            parameters.sarif.set(this@Generate.sarif)
            parameters.packageName.set(this@Generate.packageName)
            parameters.apiName.set(this@Generate.apiName)
            parameters.typePrefix.set(this@Generate.typePrefix)
            parameters.artifacts.set(this@Generate.artifacts)
            parameters.visibility.set(this@Generate.visibility)
            parameters.generatorVersion.set(this@Generate.generatorVersion)
            parameters.acknowledgements.set(this@Generate.acknowledgements)
        }
    }

    internal companion object {
        const val GROUP: String = "kapi"
    }
}
