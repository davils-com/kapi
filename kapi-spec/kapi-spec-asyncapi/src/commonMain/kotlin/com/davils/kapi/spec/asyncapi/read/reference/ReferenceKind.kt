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

package com.davils.kapi.spec.asyncapi.read.reference

internal enum class ReferenceKind(val label: String) {
    Schema("a schema"),
    Server("a server"),
    ServerVariable("a server variable"),
    Channel("a channel"),
    Operation("an operation"),
    Message("a message"),
    SecurityScheme("a security scheme"),
    Parameter("a parameter"),
    CorrelationId("a correlation id"),
    OperationReply("a reply"),
    ReplyAddress("a reply address"),
    ExternalDocs("external documentation"),
    Tag("a tag"),
    OperationTrait("an operation trait"),
    MessageTrait("a message trait"),
    ServerBindings("server bindings"),
    ChannelBindings("channel bindings"),
    OperationBindings("operation bindings"),
    MessageBindings("message bindings"),
}
