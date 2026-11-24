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

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

private const val MAXIMUM_HEAP: String = "512m"

/**
 * A task that runs the generator of kapi on one API document in an isolated worker process, then judges the
 * diagnostics: every error and every warning without an acknowledgement fails the task, and so does every
 * acknowledgement without a reason or without a matching warning. It writes the JSON report and the SARIF log of
 * the run either way.
 *
 * The task reads the document and every file below its root by their relative paths, so its results can be taken
 * from the build cache on another machine or in another folder.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Only the tasks that extend it know whether their outputs can be cached")
public abstract class DocumentTask : DefaultTask() {
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
     * The specification the document follows, `OpenApi` or `AsyncApi`.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val kind: Property<String>

    /**
     * The overlays to apply, relative to [root], in order; an AsyncAPI document takes none.
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
     * The names of the artifacts to generate, such as `Models` and `Client`.
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
     * The JSON report of the run.
     *
     * @since 1.0.0
     */
    @get:OutputFile
    public abstract val report: RegularFileProperty

    /**
     * The SARIF 2.1.0 log of the diagnostics, for code scanning.
     *
     * @since 1.0.0
     */
    @get:OutputFile
    public abstract val sarif: RegularFileProperty

    /**
     * The executor of the worker.
     *
     * @since 1.0.0
     */
    @get:Inject
    protected abstract val workers: WorkerExecutor

    init {
        group = GROUP
    }

    internal fun launch(
        mode: String,
        compared: Action<DocumentParameters> = Action { },
        folders: Action<OutputParameters>,
    ) {
        val queue = workers.processIsolation { isolation ->
            isolation.classpath.from(generatorClasspath)
            isolation.forkOptions { fork ->
                fork.maxHeapSize = MAXIMUM_HEAP
                launcher.orNull?.let { chosen -> fork.executable(chosen.executablePath.asFile) }
            }
        }
        queue.submit(LaunchAction::class.java) { parameters ->
            val document = parameters.document
            document.specification.set(specification)
            document.root.set(root)
            document.overlays.set(overlays)
            document.kind.set(kind)
            compared.execute(document)
            val options = parameters.options
            options.packageName.set(packageName)
            options.apiName.set(apiName)
            options.typePrefix.set(typePrefix)
            options.artifacts.set(artifacts)
            options.visibility.set(visibility)
            options.generatorVersion.set(generatorVersion)
            options.acknowledgements.set(acknowledgements)
            parameters.outputs.report.set(report)
            parameters.outputs.sarif.set(sarif)
            folders.execute(parameters.outputs)
            parameters.mode.set(mode)
        }
    }

    internal companion object {
        const val GROUP: String = "kapi"

        const val GENERATE: String = "Generate"

        const val LINT: String = "Lint"

        const val COMPARE: String = "Compare"
    }
}
