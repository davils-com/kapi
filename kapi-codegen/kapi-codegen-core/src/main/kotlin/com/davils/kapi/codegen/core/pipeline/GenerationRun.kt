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

package com.davils.kapi.codegen.core.pipeline

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.MediaTypes
import com.davils.kapi.codegen.core.ModelCatalog
import com.davils.kapi.codegen.core.emit.ModelEmitter
import com.davils.kapi.codegen.core.emit.client.ClientEmitter
import com.davils.kapi.codegen.core.emit.client.ClientKind
import com.davils.kapi.codegen.core.emit.mock.MockEmitter
import com.davils.kapi.codegen.core.emit.server.ServerEmitter
import com.davils.kapi.codegen.core.emit.server.ServerKind
import com.davils.kapi.codegen.core.emit.server.ServerSupport
import com.davils.kapi.codegen.core.extension.ExtensionAudit
import com.davils.kapi.codegen.core.model.ModelCatalogs
import com.davils.kapi.codegen.core.model.ModelPatches
import com.davils.kapi.codegen.core.model.ModelPlanner
import com.davils.kapi.codegen.core.model.ModelSplit
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.naming.NameTable
import com.davils.kapi.codegen.core.naming.SchemaNames
import com.davils.kapi.codegen.core.operation.HookPlanner
import com.davils.kapi.codegen.core.operation.OperationPlanner
import com.davils.kapi.codegen.core.operation.XmlBodies
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.BodyPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ResponsePlan
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.codegen.core.options.GeneratorOptions
import com.davils.kapi.codegen.core.sample.MockPlanner
import com.davils.kapi.codegen.core.sample.SamplePlanner
import com.davils.kapi.codegen.core.sample.Samples
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.openapi.Document

private val served: Set<Artifact> = setOf(Artifact.Client, Artifact.Server)

private val delivered: Set<Artifact> = setOf(Artifact.Senders, Artifact.Receivers)

internal class GenerationRun(
    private val options: GeneratorOptions,
    private val catalogs: List<ModelCatalog>,
) {
    private val emitter: ModelEmitter = ModelEmitter(options)

    fun run(document: Document, sources: List<SourceDocument>): Generation {
        val findings = Findings()
        ExtensionAudit(findings).audit(document)
        val table = NameTable(findings)
        val scope = PlanScope(
            document = document,
            names = SchemaNames(options, table).name(document),
            findings = findings,
            table = table,
            catalogs = catalogs,
        )
        val planned = planned(scope)
        if (findings.hasErrors) return Generation.Refused(findings.sorted())
        val models = (planned.models + planned.patches).sortedBy(ModelPlan::name)
        val fields = fieldsOf(models)
        val sides = clients(scope, planned.operations) + servers(scope, planned.operations, fields)
        val hooks = senders(scope, planned.hooks) + receivers(scope, planned.hooks, fields)
        val files = emitter.emit(models) + sides + hooks + mocks(scope, planned)
        val provenance = Provenances.of(options, sources, files)
        return Generation.Generated(
            files = files + Provenances.file(provenance),
            diagnostics = findings.sorted(),
            provenance = provenance,
            catalog = ModelCatalogs.of(scope, emitter.modelPackage, planned.models),
        )
    }

    private fun planned(scope: PlanScope): Planned {
        if (scope.findings.hasErrors) return Planned(emptyList(), emptyList())
        val models = ModelPlanner(scope)
        models.plan()
        val planner = OperationPlanner(scope, options, models)
        val operations = operations(planner)
        val hooks = hooks(scope, planner)
        val split = ModelSplit(scope)
        val divided = split.split(scope.plans)
        val written = operations.map { operation -> written(split, operation) }
        val roots = rootsOf(written + hooks)
        val shaped = divided.map { plan -> rooted(plan, roots) }
        val patches = ModelPatches(scope)
        val bodies = (written + hooks).mapNotNull { operation -> operation.body }.filter(BodyPlan::isPatch)
        audit(scope, written, hooks)
        return Planned(
            models = shaped,
            operations = written.map { operation -> patched(patches, operation) },
            patches = patches.plan(shaped, bodies.map(BodyPlan::type)),
            samples = samples(scope, shaped),
            hooks = hooks.map { operation -> patched(patches, operation) },
        )
    }

    private fun rootsOf(operations: List<OperationPlan>): Set<String> {
        val bodies = operations.mapNotNull(OperationPlan::body)
        val requests = bodies.filter { body -> body.kind == BodyKind.Xml }
        val responses = operations.flatMap(OperationPlan::responses).filter { response -> isXml(response) }
        val types = requests.map(BodyPlan::type) + responses.mapNotNull(ResponsePlan::body)
        return types.mapNotNull { type -> (type.shape as? TypeShape.Named)?.name }.toSet()
    }

    private fun isXml(response: ResponsePlan): Boolean {
        val type = response.accept.firstOrNull().takeIf { response.body != null }
        return type != null && MediaTypes.isXml(type)
    }

    private fun rooted(plan: ModelPlan, roots: Set<String>): ModelPlan {
        val root = (plan as? ModelPlan.ObjectPlan)?.takeIf { candidate -> candidate.name in roots } ?: return plan
        return root.copy(root = XmlBodies.nameOf(root.origin))
    }

    private fun audit(scope: PlanScope, operations: List<OperationPlan>, hooks: List<OperationPlan>) {
        val support = ServerSupport(scope.findings)
        if (Artifact.Server in options.artifacts) support.audit(operations)
        if (Artifact.Receivers in options.artifacts) support.audit(hooks)
    }

    private fun samples(scope: PlanScope, models: List<ModelPlan>): Samples? {
        if (Artifact.Mocks !in options.artifacts) return null
        return SamplePlanner(scope, options.typePrefix + options.apiName).plan(models)
    }

    private fun mocks(scope: PlanScope, planned: Planned): List<GeneratedFile> {
        val samples = planned.samples ?: return emptyList()
        val emitter = MockEmitter(
            options = options,
            origin = scope.documentName,
            samples = samples,
            planner = MockPlanner(samples, scope.findings),
        )
        return emitter.emit(planned.operations)
    }

    private fun operations(planner: OperationPlanner): List<OperationPlan> {
        if (served.none { artifact -> artifact in options.artifacts }) return emptyList()
        return planner.plan()
    }

    private fun hooks(scope: PlanScope, planner: OperationPlanner): List<OperationPlan> {
        if (delivered.none { artifact -> artifact in options.artifacts }) return emptyList()
        return HookPlanner(scope, planner, options.typePrefix + options.apiName).plan()
    }

    private fun written(split: ModelSplit, operation: OperationPlan): OperationPlan {
        val body = operation.body ?: return operation
        val fields = body.fields.map { field -> field.copy(type = split.written(field.type)) }
        return operation.copy(body = body.copy(type = split.written(body.type), fields = fields))
    }

    private fun patched(patches: ModelPatches, operation: OperationPlan): OperationPlan {
        val body = operation.body?.takeIf(BodyPlan::isPatch) ?: return operation
        return operation.copy(body = body.copy(type = patches.patched(body.type)))
    }

    private fun clients(scope: PlanScope, operations: List<OperationPlan>): List<GeneratedFile> {
        if (Artifact.Client !in options.artifacts) return emptyList()
        return ClientEmitter(options, scope.documentName).emit(operations)
    }

    private fun senders(scope: PlanScope, hooks: List<OperationPlan>): List<GeneratedFile> {
        if (Artifact.Senders !in options.artifacts) return emptyList()
        return ClientEmitter(options, scope.documentName, ClientKind.Sender).emit(hooks)
    }

    private fun receivers(
        scope: PlanScope,
        hooks: List<OperationPlan>,
        fields: Map<String, List<String>>,
    ): List<GeneratedFile> {
        if (Artifact.Receivers !in options.artifacts) return emptyList()
        val emitter = ServerEmitter(
            options = options,
            origin = scope.documentName,
            fields = fields,
            kind = ServerKind.Receiver,
        )
        return emitter.emit(hooks)
    }

    private fun servers(
        scope: PlanScope,
        operations: List<OperationPlan>,
        fields: Map<String, List<String>>,
    ): List<GeneratedFile> {
        if (Artifact.Server !in options.artifacts) return emptyList()
        return ServerEmitter(options, scope.documentName, fields).emit(operations)
    }

    private fun fieldsOf(models: List<ModelPlan>): Map<String, List<String>> {
        val objects = models.filterIsInstance<ModelPlan.ObjectPlan>()
        return objects.associate { plan -> plan.name to plan.properties.map(PropertyPlan::jsonName) }
    }
}
