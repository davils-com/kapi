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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.path.Placeholders
import com.davils.kapi.spec.openapi.model.server.Server

internal object ServerRules {
    fun check(scope: RuleScope) {
        scope.all<Server>().forEach { server -> server(scope, server) }
    }

    private fun server(scope: RuleScope, server: Server) {
        val named = Placeholders.names(text = server.url, stops = Placeholders.SERVER_STOPS)
        val undeclared = named.filterNot { name -> name in server.variables }
        undeclared.forEach { name ->
            val message = "The address of the server names the variable $name, which the server does not declare"
            scope.report(DiagnosticCodes.SERVER_VARIABLE_UNDECLARED, message, server)
        }
        val limited = server.variables.entries.filter { (_, declared) -> declared.enumeration.isNotEmpty() }
        val outside = limited.filter { (_, declared) -> declared.default !in declared.enumeration }
        outside.forEach { (name, declared) ->
            val message = "The default of the server variable $name is none of the values the variable allows"
            scope.report(DiagnosticCodes.SERVER_VARIABLE_DEFAULT, message, declared)
        }
    }
}
