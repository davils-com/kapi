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
import com.davils.kapi.spec.asyncapi.model.channel.Parameter
import com.davils.kapi.spec.asyncapi.model.message.CorrelationId
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.message.MessageExample
import com.davils.kapi.spec.asyncapi.model.message.MessageTrait
import com.davils.kapi.spec.asyncapi.model.operation.ReplyAddress
import com.davils.kapi.spec.core.model.Element

internal object MessageRules {
    fun check(scope: RuleScope) {
        expressions(scope)
        contentTypes(scope)
        scope.all<Message>().forEach { message -> headers(scope, message, SchemaChecks.typeOf(scope, message.headers)) }
        scope.all<MessageTrait>().forEach { trait -> headers(scope, trait, SchemaChecks.typeOf(scope, trait.headers)) }
        scope.all<MessageExample>().forEach { example -> example(scope, example) }
    }

    private fun expressions(scope: RuleScope) {
        val correlations = scope.all<CorrelationId>().map { correlation -> correlation to correlation.expression }
        val addresses = scope.all<ReplyAddress>().map { address -> address to address.expression }
        val parameters = scope.all<Parameter>().mapNotNull { parameter ->
            parameter.expression?.let { expression -> parameter to expression }
        }
        val written: List<Pair<Element, String>> = correlations + addresses + parameters
        written.filterNot { (_, text) -> Grammars.isRuntimeExpression(text) }.forEach { (element, text) ->
            val message = "${shown(text)} is no runtime expression such as ${'$'}message.header#/correlationId"
            scope.report(DiagnosticCodes.RUNTIME_EXPRESSION, message, element)
        }
    }

    private fun contentTypes(scope: RuleScope) {
        val messages = scope.all<Message>().map { message -> message to message.contentType }
        val traits = scope.all<MessageTrait>().map { trait -> trait to trait.contentType }
        val document = listOf(scope.document to scope.document.defaultContentType)
        val written: List<Pair<Element, String?>> = document + messages + traits
        written.forEach { (element, text) ->
            val isValid = text == null || Grammars.isMediaType(text)
            if (!isValid) {
                val message = "${shown(text)} is no media type such as application/json"
                scope.report(DiagnosticCodes.MESSAGE_CONTENT_TYPE, message, element)
            }
        }
    }

    private fun headers(scope: RuleScope, element: Element, type: String?) {
        if (type == null || type == SchemaChecks.OBJECT) return
        val message = "The headers of a message are an object, but their schema has the type ${shown(type)}"
        scope.report(DiagnosticCodes.MESSAGE_HEADERS_TYPE, message, element)
    }

    private fun example(scope: RuleScope, example: MessageExample) {
        if (example.headers.isNotEmpty() || example.payload != null) return
        val message = "An example of a message needs its headers, its payload or both"
        scope.report(DiagnosticCodes.MESSAGE_EXAMPLE_EMPTY, message, example)
    }
}
