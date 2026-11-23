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
import com.davils.kapi.spec.asyncapi.model.security.OAuth2Scheme
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

internal object SecurityRules {
    fun check(scope: RuleScope) {
        scope.all<OAuth2Scheme>().forEach { scheme -> scopes(scope, scheme) }
    }

    private fun scopes(scope: RuleScope, scheme: OAuth2Scheme) {
        val flows = scheme.flows
        val declared = listOfNotNull(flows.implicit, flows.password, flows.clientCredentials, flows.authorizationCode)
        val available = OrderedSet(declared.flatMap { flow -> flow.availableScopes.keys })
        scheme.scopes.filterNot { name -> name in available }.forEach { name ->
            val message = "The security scheme requires the scope ${shown(name)}, which none of its flows offers"
            scope.report(DiagnosticCodes.SECURITY_SCOPES, message, scheme)
        }
    }
}
