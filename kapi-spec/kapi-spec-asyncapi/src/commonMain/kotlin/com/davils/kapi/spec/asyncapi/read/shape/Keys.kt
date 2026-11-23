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

package com.davils.kapi.spec.asyncapi.read.shape

internal object Keys {
    const val ACTION: String = "action"
    const val ADDRESS: String = "address"
    const val ASYNCAPI: String = "asyncapi"
    const val AUTHORIZATION_CODE: String = "authorizationCode"
    const val AUTHORIZATION_URL: String = "authorizationUrl"
    const val AVAILABLE_SCOPES: String = "availableScopes"
    const val BEARER_FORMAT: String = "bearerFormat"
    const val BINDINGS: String = "bindings"
    const val BINDING_VERSION: String = "bindingVersion"
    const val CHANNEL: String = "channel"
    const val CHANNELS: String = "channels"
    const val CHANNEL_BINDINGS: String = "channelBindings"
    const val CLIENT_CREDENTIALS: String = "clientCredentials"
    const val COMPONENTS: String = "components"
    const val CONTACT: String = "contact"
    const val CONTENT_TYPE: String = "contentType"
    const val CORRELATION_ID: String = "correlationId"
    const val CORRELATION_IDS: String = "correlationIds"
    const val DEFAULT: String = "default"
    const val DEFAULT_CONTENT_TYPE: String = "defaultContentType"
    const val DESCRIPTION: String = "description"
    const val EMAIL: String = "email"
    const val ENUM: String = "enum"
    const val EXAMPLES: String = "examples"
    const val EXTERNAL_DOCS: String = "externalDocs"
    const val FLOWS: String = "flows"
    const val HEADERS: String = "headers"
    const val HOST: String = "host"
    const val ID: String = "id"
    const val IMPLICIT: String = "implicit"
    const val IN: String = "in"
    const val INFO: String = "info"
    const val LICENSE: String = "license"
    const val LOCATION: String = "location"
    const val MESSAGE: String = "message"
    const val MESSAGES: String = "messages"
    const val MESSAGE_BINDINGS: String = "messageBindings"
    const val MESSAGE_ID: String = "messageId"
    const val MESSAGE_TRAITS: String = "messageTraits"
    const val METHOD: String = "method"
    const val NAME: String = "name"
    const val OPEN_ID_CONNECT_URL: String = "openIdConnectUrl"
    const val OPERATIONS: String = "operations"
    const val OPERATION_BINDINGS: String = "operationBindings"
    const val OPERATION_ID: String = "operationId"
    const val OPERATION_TRAITS: String = "operationTraits"
    const val PARAMETERS: String = "parameters"
    const val PASSWORD: String = "password"
    const val PATHNAME: String = "pathname"
    const val PAYLOAD: String = "payload"
    const val PROTOCOL: String = "protocol"
    const val PROTOCOL_VERSION: String = "protocolVersion"
    const val PUBLISH: String = "publish"
    const val QUERY: String = "query"
    const val REFERENCE: String = "${'$'}ref"
    const val REFRESH_URL: String = "refreshUrl"
    const val REPLIES: String = "replies"
    const val REPLY: String = "reply"
    const val REPLY_ADDRESSES: String = "replyAddresses"
    const val SCHEMA: String = "schema"
    const val SCHEMAS: String = "schemas"
    const val SCHEMA_FORMAT: String = "schemaFormat"
    const val SCHEME: String = "scheme"
    const val SCOPES: String = "scopes"
    const val SECURITY: String = "security"
    const val SECURITY_SCHEMES: String = "securitySchemes"
    const val SERVERS: String = "servers"
    const val SERVER_BINDINGS: String = "serverBindings"
    const val SERVER_VARIABLES: String = "serverVariables"
    const val SUBSCRIBE: String = "subscribe"
    const val SUMMARY: String = "summary"
    const val TAGS: String = "tags"
    const val TERMS_OF_SERVICE: String = "termsOfService"
    const val TITLE: String = "title"
    const val TOKEN_URL: String = "tokenUrl"
    const val TRAITS: String = "traits"
    const val TYPE: String = "type"
    const val URL: String = "url"
    const val VARIABLES: String = "variables"
    const val VERSION: String = "version"
    const val WEB_SOCKET: String = "ws"

    val protocols: List<String> = listOf(
        "http",
        WEB_SOCKET,
        "kafka",
        "anypointmq",
        "amqp",
        "amqp1",
        "mqtt",
        "mqtt5",
        "nats",
        "jms",
        "sns",
        "solace",
        "sqs",
        "stomp",
        "redis",
        "mercure",
        "ibmmq",
        "googlepubsub",
        "pulsar",
    )
}
