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

package com.davils.kapi.codegen.core.asyncapi.emit

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

private const val CLIENT: String = "com.davils.kapi.runtime.client"

private const val CLIENT_CHANNEL: String = "$CLIENT.channel"

private const val CLIENT_CALL: String = "$CLIENT.call"

private const val SERVER: String = "com.davils.kapi.runtime.server"

private const val SERVER_CHANNEL: String = "$SERVER.channel"

private const val SERVER_OPERATION: String = "$SERVER.operation"

private const val PARAMETER: String = "com.davils.kapi.runtime.core.parameter"

private const val SERIALIZATION: String = "kotlinx.serialization"

private const val JSON: String = "$SERIALIZATION.json"

private const val DESCRIPTORS: String = "$SERIALIZATION.descriptors"

private const val ENCODING: String = "$SERIALIZATION.encoding"

internal object Runtime {
    val apiClient: ClassName = ClassName(CLIENT, "ApiClient")

    val apiCall: MemberName = MemberName(packageName = CLIENT_CALL, simpleName = "apiCall")

    val securityUse: ClassName = ClassName(CLIENT_CALL, "SecurityUse")

    val credentialPlacement: ClassName = ClassName(CLIENT_CALL, "CredentialPlacement")

    val channel: MemberName = MemberName(packageName = CLIENT_CHANNEL, simpleName = "channel", isExtension = true)

    val channelSpec: ClassName = ClassName(CLIENT_CHANNEL, "ChannelSpec")

    val clientSession: ClassName = ClassName(CLIENT_CHANNEL, "ChannelSession")

    val channelOutcome: ClassName = ClassName(CLIENT_CHANNEL, "ChannelOutcome")

    val channelReply: ClassName = ClassName(CLIENT_CHANNEL, "ChannelReply")

    val channelSettingsBuilder: ClassName = ClassName(CLIENT_CHANNEL, "ChannelSettingsBuilder")

    val channelSettings: MemberName = MemberName(packageName = CLIENT_CHANNEL, simpleName = "channelSettings")

    val correlation: ClassName = ClassName(CLIENT_CHANNEL, "Correlation")

    val closeKind: ClassName = ClassName(CLIENT_CHANNEL, "CloseKind")

    val parameterFormat: ClassName = ClassName(PARAMETER, "ParameterFormat")

    val parameterStyle: ClassName = ClassName(PARAMETER, "ParameterStyle")

    val parameterValue: ClassName = ClassName(PARAMETER, "ParameterValue")

    val channelEndpoint: ClassName = ClassName(SERVER_CHANNEL, "ChannelEndpoint")

    val serverSession: ClassName = ClassName(SERVER_CHANNEL, "ChannelSession")

    val messageCodec: ClassName = ClassName(SERVER_CHANNEL, "MessageCodec")

    val channelLimits: ClassName = ClassName(SERVER_CHANNEL, "ChannelLimits")

    val serverCorrelation: ClassName = ClassName(SERVER_CHANNEL, "Correlation")

    val serverChannelReply: ClassName = ClassName(SERVER_CHANNEL, "ChannelReply")

    val reply: ClassName = ClassName("$SERVER.reply", "Reply")

    val routes: ClassName = ClassName(SERVER_OPERATION, "Routes")

    val operation: ClassName = ClassName(SERVER_OPERATION, "Operation")

    val schemeUse: ClassName = ClassName(SERVER_OPERATION, "SchemeUse")

    val credentialSource: ClassName = ClassName(SERVER_OPERATION, "CredentialSource")

    val request: ClassName = ClassName("$SERVER.request", "Request")

    val serializable: ClassName = ClassName(SERIALIZATION, "Serializable")

    val kSerializer: ClassName = ClassName(SERIALIZATION, "KSerializer")

    val serializationException: ClassName = ClassName(SERIALIZATION, "SerializationException")

    val experimentalSerialization: ClassName = ClassName(SERIALIZATION, "ExperimentalSerializationApi")

    val serializer: MemberName = MemberName(packageName = SERIALIZATION, simpleName = "serializer")

    val nothingSerializer: MemberName =
        MemberName(packageName = "$SERIALIZATION.builtins", simpleName = "NothingSerializer")

    val serialDescriptor: ClassName = ClassName(DESCRIPTORS, "SerialDescriptor")

    val buildClassSerialDescriptor: MemberName =
        MemberName(packageName = DESCRIPTORS, simpleName = "buildClassSerialDescriptor")

    val encoder: ClassName = ClassName(ENCODING, "Encoder")

    val decoder: ClassName = ClassName(ENCODING, "Decoder")

    val json: ClassName = ClassName(JSON, "Json")

    val jsonDecoder: ClassName = ClassName(JSON, "JsonDecoder")

    val jsonElement: ClassName = ClassName(JSON, "JsonElement")

    val jsonObject: ClassName = ClassName(JSON, "JsonObject")

    val jsonArray: ClassName = ClassName(JSON, "JsonArray")

    val jsonPrimitive: ClassName = ClassName(JSON, "JsonPrimitive")

    val contentOrNull: MemberName = MemberName(packageName = JSON, simpleName = "contentOrNull", isExtension = true)

    val flow: ClassName = ClassName("kotlinx.coroutines.flow", "Flow")

    val duration: ClassName = ClassName("kotlin.time", "Duration")

    val optIn: ClassName = ClassName("kotlin", "OptIn")

    val generated: ClassName = ClassName("com.davils.kore.core.annotation", "DavilsGenerated")
}
