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

import com.davils.kapi.codegen.core.launch.LaunchRequest
import com.davils.kapi.codegen.core.launch.Launcher
import com.davils.kore.core.annotation.DavilsInternal
import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction

internal abstract class GenerateAction : WorkAction<GenerateParameters> {
    override fun execute() {
        val request = requestOf(parameters)
        val verdict = Launcher.run(request)
        val logger = Logging.getLogger(GenerateAction::class.java)
        verdict.lines.forEach(logger::warn)
        if (verdict.isAccepted) return
        val failures = verdict.failures.joinToString(separator = "\n")
        throw GradleException("The API document ${request.specification} was not generated:\n$failures")
    }

    private fun requestOf(parameters: GenerateParameters): LaunchRequest {
        val root = parameters.root.get().asFile.toPath()
        return LaunchRequest(
            specification = parameters.specification.get().asFile.toPath(),
            root = root,
            overlays = parameters.overlays.get().map { overlay -> root.resolve(overlay) },
            commonKotlin = parameters.commonKotlin.get().asFile.toPath(),
            jvmKotlin = parameters.jvmKotlin.get().asFile.toPath(),
            resources = parameters.resources.get().asFile.toPath(),
            report = parameters.report.get().asFile.toPath(),
            sarif = parameters.sarif.get().asFile.toPath(),
            packageName = parameters.packageName.get(),
            apiName = parameters.apiName.get(),
            typePrefix = parameters.typePrefix.get(),
            artifacts = parameters.artifacts.get(),
            visibility = parameters.visibility.get(),
            generatorVersion = parameters.generatorVersion.get(),
            acknowledgements = parameters.acknowledgements.get(),
        )
    }
}
