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

package com.davils.kapi.spec.openapi.overlay

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.patch.JsonPatch
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.patch.JsonPatchResult
import com.davils.kaml.document.path.JsonPathMatch
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kore.core.failure.contained

private val deepestFirst: Comparator<JsonPathMatch> = compareBy<JsonPathMatch>(
    { match -> match.location.tokens.size },
    { match -> match.location.last?.toIntOrNull() ?: 0 },
).reversed()

internal class OverlayApplication(private val findings: Findings, private val limits: ReadLimits) {
    fun applied(root: YamlNode, actions: List<OverlayAction>): YamlNode =
        actions.fold(root) { current, action -> bounded(current, action) }

    private fun bounded(root: YamlNode, action: OverlayAction): YamlNode {
        val result = contained { applied(root, action) }
        val changed = result.getOrNull()
        if (changed == null) {
            refuse(action, "The action of '${action.target}' goes past the limits of an overlay and is left out")
            return root
        }
        val isBounded = NodeBudget.isWithin(
            root = changed,
            maximumNodes = limits.maximumNodes,
            maximumDepth = limits.maximumDepth,
        )
        if (!isBounded) {
            val message = "The action of '${action.target}' grows the document past ${limits.maximumNodes} nodes or " +
                "a depth of ${limits.maximumDepth} and is left out"
            refuse(action, message)
        }
        return changed.takeIf { isBounded } ?: root
    }

    private fun applied(root: YamlNode, action: OverlayAction): YamlNode {
        val matches = action.matches(root)
        if (!isAcceptable(action, matches)) return root
        val updates = action.update?.let { update -> updates(matches, update, action) }
        return patched(root, updates ?: removals(matches, action), action)
    }

    private fun isAcceptable(action: OverlayAction, matches: List<JsonPathMatch>): Boolean {
        if (matches.isEmpty()) {
            val message = "The target '${action.target}' selects nothing in the document"
            action.report(findings, DiagnosticCodes.OVERLAY_UNMATCHED, message)
            return false
        }
        val growth = action.update?.let { update -> NodeBudget.count(update, limits.maximumNodes) }
        val isTooLarge = growth != null && growth * matches.size > limits.maximumNodes
        if (isTooLarge) {
            refuse(action, "The update of '${action.target}' would add more than ${limits.maximumNodes} nodes")
        }
        return !isTooLarge
    }

    private fun updates(
        matches: List<JsonPathMatch>,
        update: YamlNode,
        action: OverlayAction,
    ): List<JsonPatchOperation> = matches.mapNotNull { match ->
        val merged = OverlayMerge.merged(match.node, update)
        if (merged == null) refuse(action, "The update of '${action.target}' cannot change ${match.location}")
        merged?.let { value -> JsonPatchOperation.Replace(match.location, value) }
    }

    private fun removals(matches: List<JsonPathMatch>, action: OverlayAction): List<JsonPatchOperation> {
        val removable = matches.filterNot { match -> match.location.isRoot }
        if (removable.size < matches.size) refuse(action, "The target '${action.target}' removes the whole document")
        val ordered = removable.sortedWith(deepestFirst)
        return ordered.map { match -> JsonPatchOperation.Remove(match.location) }
    }

    private fun patched(root: YamlNode, operations: List<JsonPatchOperation>, action: OverlayAction): YamlNode {
        val result = JsonPatch(operations).apply(root)
        val failure = result as? JsonPatchResult.Failed
        if (failure != null) refuse(action, messageOf(operations, failure))
        return (result as? JsonPatchResult.Applied)?.node ?: root
    }

    private fun messageOf(operations: List<JsonPatchOperation>, failure: JsonPatchResult.Failed): String {
        val place = operations.getOrNull(failure.index)?.path ?: JsonPointer.ROOT
        return "The action cannot change $place: ${failure.reason}"
    }

    private fun refuse(action: OverlayAction, message: String) {
        action.report(findings, DiagnosticCodes.OVERLAY_UPDATE, message)
    }
}
