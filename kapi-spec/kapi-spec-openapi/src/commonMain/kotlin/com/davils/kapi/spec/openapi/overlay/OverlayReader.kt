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

package com.davils.kapi.spec.openapi.overlay

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.boolean
import com.davils.kaml.core.node.detached
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.path.JsonPath
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kore.core.annotation.DavilsInternal

internal class OverlayReader(
    document: LoadedDocument,
    private val findings: Findings,
) {
    private val root: OverlayPlace = OverlayPlace(document, JsonPointer.ROOT, document.root)

    fun actions(): List<OverlayAction> {
        val mapping = root.node as? YamlNode.Mapping
        if (mapping == null) {
            shape(root, "An overlay must be an object")
            return emptyList()
        }
        version(mapping)
        fields(root, OverlayKeys.rootFields)
        info(root.child(OverlayKeys.INFO, mapping[OverlayKeys.INFO]))
        return actionsOf(root.child(OverlayKeys.ACTIONS, mapping[OverlayKeys.ACTIONS]))
    }

    private fun version(mapping: YamlNode.Mapping) {
        val place = root.child(OverlayKeys.OVERLAY, mapping[OverlayKeys.OVERLAY])
        val written = textOf(place.node).orEmpty()
        if (OverlayKeys.versionPattern.matches(written)) return
        val message = "The overlay names the version '$written', the reader knows Overlay 1.0"
        place.report(findings, DiagnosticCodes.OVERLAY_VERSION, message)
    }

    private fun info(place: OverlayPlace) {
        if (place.node !is YamlNode.Mapping) {
            shape(place, "info must be an object with a title and a version")
            return
        }
        fields(place, OverlayKeys.infoFields)
        val missing = OverlayKeys.infoFields.filter { field -> textOf(place.node[field]) == null }
        missing.forEach { field -> shape(place.child(field, null), "info needs $field as text") }
    }

    private fun actionsOf(place: OverlayPlace): List<OverlayAction> {
        val sequence = place.node as? YamlNode.Sequence
        if (sequence == null || sequence.items.isEmpty()) {
            shape(place, "actions must list at least one action")
            return emptyList()
        }
        return sequence.items.mapIndexedNotNull { index, item -> actionOf(place.child(index.toString(), item)) }
    }

    private fun actionOf(place: OverlayPlace): OverlayAction? {
        val node = place.node
        fields(place, OverlayKeys.actionFields)
        val target = targetOf(place.child(OverlayKeys.TARGET, node[OverlayKeys.TARGET]))
        val update = node[OverlayKeys.UPDATE]?.detached()
        val isRemoval = node[OverlayKeys.REMOVE].boolean() == true
        val isUpdate = update != null
        val isDecided = isUpdate != isRemoval
        if (!isDecided) {
            val message = "An action must either update or remove its target"
            place.report(findings, DiagnosticCodes.OVERLAY_ACTION, message)
        }
        return target?.takeIf { isDecided }?.let { path -> OverlayAction(path, update, place) }
    }

    private fun targetOf(place: OverlayPlace): JsonPath? {
        val expression = textOf(place.node)
        if (expression == null) {
            shape(place, "An action needs its target as a JSONPath query")
            return null
        }
        val path = JsonPath.parseOrNull(expression)
        val message = "The target '$expression' is not a JSONPath query"
        if (path == null) place.report(findings, DiagnosticCodes.OVERLAY_TARGET, message)
        return path
    }

    private fun fields(place: OverlayPlace, known: Set<String>) {
        val entries = (place.node as? YamlNode.Mapping)?.entries.orEmpty()
        val unknown = entries.filterNot { entry -> isAllowed(entry, known) }
        unknown.forEach { entry ->
            val name = entry.key.string().orEmpty()
            shape(place.child(name, entry.key), "$name is no field of an overlay")
        }
    }

    private fun isAllowed(entry: YamlEntry, known: Set<String>): Boolean {
        val key = entry.key.string().orEmpty()
        return key in known || key.startsWith(OverlayKeys.EXTENSION_PREFIX)
    }

    private fun textOf(node: YamlNode?): String? {
        val scalar = node as? YamlNode.Scalar ?: return null
        return scalar.value.takeIf { scalar.tag == YamlTag.STRING }
    }

    private fun shape(place: OverlayPlace, message: String) {
        place.report(findings, DiagnosticCodes.OVERLAY_SHAPE, message)
    }
}
