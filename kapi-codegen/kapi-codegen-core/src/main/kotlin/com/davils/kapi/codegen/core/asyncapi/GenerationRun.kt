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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.ModelCatalog
import com.davils.kapi.codegen.core.asyncapi.emit.ClientEmitter
import com.davils.kapi.codegen.core.asyncapi.emit.CorrelationEmitter
import com.davils.kapi.codegen.core.asyncapi.emit.Emission
import com.davils.kapi.codegen.core.asyncapi.emit.MessageEmitter
import com.davils.kapi.codegen.core.asyncapi.emit.RoutesEmitter
import com.davils.kapi.codegen.core.asyncapi.emit.ServerEmitter
import com.davils.kapi.codegen.core.asyncapi.plan.ChannelPlan
import com.davils.kapi.codegen.core.asyncapi.plan.PayloadSource
import com.davils.kapi.codegen.core.asyncapi.read.ChannelPlanner
import com.davils.kapi.codegen.core.asyncapi.schema.ModelRun
import com.davils.kapi.codegen.core.asyncapi.schema.PayloadTypes
import com.davils.kapi.codegen.core.asyncapi.schema.SchemaHoist
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.core.source.SourceDocument

internal class GenerationRun(private val options: GeneratorOptions) {
    suspend fun run(document: Document, sources: List<SourceDocument>): Generation {
        val report = Report()
        val emitted = emitted(document, report)
        if (emitted == null || report.hasErrors) return Generation.Refused(report.sorted())
        val (files, catalog) = emitted
        val provenance = Provenances.of(options, sources, files)
        return Generation.Generated(
            files = files + Provenances.file(provenance),
            diagnostics = report.sorted(),
            provenance = provenance,
            catalog = catalog,
        )
    }

    private suspend fun emitted(document: Document, report: Report): Pair<List<GeneratedFile>, ModelCatalog>? {
        val plans = ChannelPlanner(document, report, options.typePrefix + options.apiName).plan()
        val messages = plans.flatMap { plan -> plan.upstream.messages + plan.downstream.messages }
        val hoist = SchemaHoist(document, report)
        val hoisted = hoist.hoist(messages.mapNotNull { message -> message.payload as? PayloadSource.Declared })
        val isPlanned = !report.hasErrors
        val models = hoisted.takeIf { isPlanned }?.let { found -> ModelRun(options, report).run(document, found) }
            ?: return null
        val payloads = PayloadTypes(hoist = hoist, hoisted = hoisted, catalog = models.catalog, report = report)
        messages.forEach(payloads::typeOf)
        val uri = document.location?.run { this.document.value }.orEmpty()
        val origin = uri.substringAfterLast('/')
        val emission = Emission(options, origin, payloads)
        val files = models.files + channels(emission, plans) + clients(emission, plans) + servers(emission, plans)
        return files to models.catalog
    }

    private fun channels(emission: Emission, plans: List<ChannelPlan>): List<GeneratedFile> {
        val emitter = MessageEmitter(emission)
        val correlations = CorrelationEmitter(emission)
        return plans.flatMap { plan -> emitter.emit(plan) + correlations.emit(plan) }
    }

    private fun clients(emission: Emission, plans: List<ChannelPlan>): List<GeneratedFile> {
        if (Artifact.Client !in options.artifacts) return emptyList()
        val emitter = ClientEmitter(emission, CorrelationEmitter(emission))
        return plans.flatMap(emitter::emit)
    }

    private fun servers(emission: Emission, plans: List<ChannelPlan>): List<GeneratedFile> {
        if (Artifact.Server !in options.artifacts) return emptyList()
        val emitter = ServerEmitter(emission)
        val routes = RoutesEmitter(emission, emitter, CorrelationEmitter(emission))
        return plans.flatMap(emitter::emit) + routes.emit(options.typePrefix + options.apiName + "Routes", plans)
    }
}
