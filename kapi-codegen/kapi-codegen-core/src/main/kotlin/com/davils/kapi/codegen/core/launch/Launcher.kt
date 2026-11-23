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
import kotlinx.coroutines.runBlocking

/**
 * Runs the generator for the Gradle plugin of kapi, inside its isolated worker.
 *
 * A run reads the document of the request through a resolver confined to its root, generates the requested artifacts,
 * writes them, writes the JSON report and the SARIF log, and judges the result against the acknowledgements of the
 * request. It never throws for a problem of the document; only an output that cannot be written fails it with an
 * exception.
 *
 * @since 1.0.0
 */
@DavilsInternal
public object Launcher {
    /**
     * Runs the generation [request] asks for, blocking the calling thread until it is done.
     *
     * @param request what to generate and where.
     * @return the verdict, which the plugin turns into a passing or failing task.
     * @throws java.io.IOException if an output cannot be written.
     * @since 1.0.0
     */
    public fun run(request: LaunchRequest): LaunchVerdict = runBlocking { Launch(request, isLint = false).run() }

    /**
     * Lints the document of [request], blocking the calling thread until it is done.
     *
     * A lint reads the document and audits it with the generator exactly as [run] does, so it finds every diagnostic
     * a generation would, but it writes only the JSON report and the SARIF log: the folders of the generated code are
     * never touched. Its verdict judges the acknowledgements as a generation does.
     *
     * @param request what to lint and where the reports go.
     * @return the verdict, which the plugin turns into a passing or failing task.
     * @throws java.io.IOException if a report cannot be written.
     * @since 1.0.0
     */
    public fun lint(request: LaunchRequest): LaunchVerdict = runBlocking { Launch(request, isLint = true).run() }
}
