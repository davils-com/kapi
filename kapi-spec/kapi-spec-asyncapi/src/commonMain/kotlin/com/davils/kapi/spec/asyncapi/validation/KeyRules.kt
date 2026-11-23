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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.component.Components
import com.davils.kapi.spec.core.model.Referable

internal object KeyRules {
    private const val COMPONENT: String = "letters, digits, dots, hyphens and underscores"

    private const val IDENTIFIER: String = "letters, digits, hyphens and underscores"

    fun check(scope: RuleScope) {
        val document = scope.document
        keys(scope = scope, subject = "server", map = document.servers, isIdentifier = true)
        keys(scope = scope, subject = "channel", map = document.channels, isIdentifier = false)
        keys(scope = scope, subject = "operation", map = document.operations, isIdentifier = false)
        document.components?.let { components -> components(scope, components) }
        scope.all<Channel>().forEach { channel ->
            keys(scope = scope, subject = "message", map = channel.messages, isIdentifier = false)
            keys(scope = scope, subject = "parameter", map = channel.parameters, isIdentifier = true)
        }
    }

    private fun components(scope: RuleScope, components: Components) {
        val maps = listOf(
            components.schemas,
            components.servers,
            components.channels,
            components.operations,
            components.messages,
            components.securitySchemes,
            components.serverVariables,
            components.parameters,
            components.correlationIds,
            components.replies,
            components.replyAddresses,
            components.externalDocs,
            components.tags,
            components.operationTraits,
            components.messageTraits,
            components.serverBindings,
            components.channelBindings,
            components.operationBindings,
            components.messageBindings,
        )
        maps.forEach { map -> keys(scope = scope, subject = "component", map = map, isIdentifier = false) }
    }

    private fun keys(scope: RuleScope, subject: String, map: Map<String, Referable<*>>, isIdentifier: Boolean) {
        val allowed = IDENTIFIER.takeIf { isIdentifier } ?: COMPONENT
        val invalid = map.entries.filter { (key, _) -> !isValid(key, isIdentifier) }
        invalid.forEach { (key, referable) ->
            val message = "The key ${shown(key)} of a $subject may only hold $allowed"
            scope.report(DiagnosticCodes.KEY_INVALID, message, referable)
        }
    }

    private fun isValid(key: String, isIdentifier: Boolean): Boolean {
        if (isIdentifier) return Grammars.isIdentifier(key)
        return Grammars.isComponentKey(key)
    }
}
