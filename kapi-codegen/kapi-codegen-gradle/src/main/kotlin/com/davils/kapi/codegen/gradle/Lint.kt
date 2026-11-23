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

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

/**
 * Reads and audits one API document in an isolated worker process without writing code: `kapiLint<Name>`.
 *
 * A lint finds every diagnostic a generation of the same document and options would find, so it suits a check
 * that should not compile anything, and writes only the JSON report and the SARIF log.
 *
 * @since 1.0.0
 */
@CacheableTask
public abstract class Lint : DocumentTask() {
    init {
        description = "Reads and audits an API document without generating code."
    }

    /**
     * Lints.
     *
     * @throws org.gradle.api.GradleException if the document has errors or unacknowledged warnings, or an
     * acknowledgement has no reason or no matching warning.
     * @since 1.0.0
     */
    @TaskAction
    public fun audit() {
        val unused = temporaryDir
        launch(isLint = true) { outputs ->
            outputs.commonKotlin.set(unused)
            outputs.jvmKotlin.set(unused)
            outputs.resources.set(unused)
        }
    }
}
