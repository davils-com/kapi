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

package com.davils.kapi.codegen.gradle

import com.davils.kapi.codegen.core.launch.LaunchKind
import com.davils.kapi.codegen.core.launch.LaunchRequest
import com.davils.kapi.codegen.core.launch.LaunchVerdict
import com.davils.kapi.codegen.core.launch.Launcher
import com.davils.kore.core.annotation.DavilsInternal
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.workers.WorkAction
import java.nio.file.Path

private val actions: Map<String, (LaunchRequest) -> LaunchVerdict> = mapOf(
    DocumentTask.GENERATE to Launcher::run,
    DocumentTask.LINT to Launcher::lint,
    DocumentTask.COMPARE to Launcher::compare,
)

private val verbs: Map<String, String> = mapOf(
    DocumentTask.GENERATE to "generate",
    DocumentTask.LINT to "pass the lint",
    DocumentTask.COMPARE to "stay compatible with its baseline",
)

internal abstract class LaunchAction : WorkAction<LaunchParameters> {
    override fun execute() {
        val request = requestOf(parameters)
        val mode = parameters.mode.get()
        val verdict = actions.getValue(mode)(request)
        val logger = Logging.getLogger(LaunchAction::class.java)
        verdict.lines.forEach(logger::warn)
        if (verdict.isAccepted) return
        val failures = verdict.failures.joinToString(separator = "\n")
        val verb = verbs.getValue(mode)
        throw GradleException("The API document ${request.specification} does not $verb:\n$failures")
    }

    private fun requestOf(parameters: LaunchParameters): LaunchRequest {
        val document = parameters.document
        val options = parameters.options
        val outputs = parameters.outputs
        val root = pathOf(document.root)
        return LaunchRequest(
            specification = pathOf(document.specification),
            root = root,
            overlays = document.overlays.get().map { overlay -> root.resolve(overlay) },
            commonKotlin = pathOf(outputs.commonKotlin),
            jvmKotlin = pathOf(outputs.jvmKotlin),
            resources = pathOf(outputs.resources),
            report = pathOf(outputs.report),
            sarif = pathOf(outputs.sarif),
            packageName = options.packageName.get(),
            apiName = options.apiName.get(),
            typePrefix = options.typePrefix.get(),
            artifacts = options.artifacts.get(),
            visibility = options.visibility.get(),
            generatorVersion = options.generatorVersion.get(),
            acknowledgements = options.acknowledgements.get(),
            kind = LaunchKind.valueOf(document.kind.get()),
            baseline = document.baseline.orNull?.run { asFile.toPath() },
            breaking = document.breaking.getOrElse(CompatibilitySide.Clients.name),
        )
    }

    private fun pathOf(directory: Provider<Directory>): Path {
        val file = directory.get().asFile
        return file.toPath()
    }

    @JvmName("pathOfFile")
    private fun pathOf(file: Provider<RegularFile>): Path {
        val found = file.get().asFile
        return found.toPath()
    }
}
