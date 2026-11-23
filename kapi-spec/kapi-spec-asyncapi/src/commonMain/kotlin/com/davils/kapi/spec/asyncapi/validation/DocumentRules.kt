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
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference

internal object DocumentRules {
    private val rules: List<(RuleScope) -> Unit> = listOf(
        ::identity,
        KeyRules::check,
        ServerRules::check,
        ChannelRules::check,
        OperationRules::check,
        MessageRules::check,
        SecurityRules::check,
        BindingRules::check,
        TagRules::check,
    )

    fun check(scope: RuleScope) {
        rules.forEach { rule -> rule(scope) }
    }

    private fun identity(scope: RuleScope) {
        val document = scope.document
        val id = document.id ?: return
        val isAbsolute = UriReference.parseOrNull(id)?.isAbsolute == true
        if (isAbsolute) return
        val message = "The id ${shown(id)} of the document is no absolute URI such as urn:example:application"
        scope.report(DiagnosticCodes.DOCUMENT_ID, message, document)
    }
}
