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
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.info.Info
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.message.MessageTrait
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationTrait
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.core.collection.groupByKeyed
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsInternal

internal object TagRules {
    fun check(scope: RuleScope) {
        val lists: List<Pair<Element, List<Referable<Tag>>>> = scope.all<Info>().map { info -> info to info.tags } +
            scope.all<Server>().map { server -> server to server.tags } +
            scope.all<Channel>().map { channel -> channel to channel.tags } +
            scope.all<Operation>().map { operation -> operation to operation.tags } +
            scope.all<OperationTrait>().map { trait -> trait to trait.tags } +
            scope.all<Message>().map { message -> message to message.tags } +
            scope.all<MessageTrait>().map { trait -> trait to trait.tags }
        lists.forEach { (element, tags) -> duplicates(scope, element, tags) }
    }

    private fun duplicates(scope: RuleScope, element: Element, tags: List<Referable<Tag>>) {
        val names = tags.mapNotNull { tag -> scope.resolve(tag)?.name }
        val groups = names.groupByKeyed { name -> name }
        val repeated = groups.entries.filter { (_, same) -> same.size > 1 }
        repeated.forEach { (name, _) ->
            scope.report(DiagnosticCodes.TAG_DUPLICATE, "The tag ${shown(name)} is listed more than once", element)
        }
    }
}
