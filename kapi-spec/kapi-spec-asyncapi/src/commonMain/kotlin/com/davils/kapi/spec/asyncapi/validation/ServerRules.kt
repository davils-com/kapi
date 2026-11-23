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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.model.server.ServerVariable
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

internal object ServerRules {
    fun check(scope: RuleScope) {
        scope.all<Server>().forEach { server -> server(scope, server) }
        scope.all<ServerVariable>().forEach { variable -> variable(scope, variable) }
    }

    private fun server(scope: RuleScope, server: Server) {
        val written = listOfNotNull(server.host, server.pathname)
        val templates = written.map { text -> text to Templates.names(text) }
        templates.filter { (_, names) -> names == null }.forEach { (text, _) ->
            val message = "${shown(text)} is no valid template, every { needs a name and a closing }"
            scope.report(DiagnosticCodes.SERVER_TEMPLATE, message, server)
        }
        val named = templates.flatMap { (_, names) -> names.orEmpty() }
        val undeclared = OrderedSet(named).filterNot { name -> name in server.variables }
        undeclared.forEach { name ->
            val message = "The server names the variable ${shown(name)}, which it does not declare"
            scope.report(DiagnosticCodes.SERVER_VARIABLE_UNDECLARED, message, server)
        }
    }

    private fun variable(scope: RuleScope, variable: ServerVariable) {
        val default = variable.default ?: return
        if (variable.enumeration.isEmpty() || default in variable.enumeration) return
        val message = "The default ${shown(default)} of the server variable is none of the values it allows"
        scope.report(DiagnosticCodes.SERVER_VARIABLE_DEFAULT, message, variable)
    }
}
