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

package com.davils.kapi.codegen.core.emit.server

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

internal object ServerTypes {
    private const val SERVER: String = "com.davils.kapi.runtime.server"

    private const val OPERATION: String = "$SERVER.operation"

    private const val REQUEST: String = "$SERVER.request"

    private const val REPLY: String = "$SERVER.reply"

    private const val CORE: String = "com.davils.kapi.runtime.core"

    private const val PARAMETER: String = "$CORE.parameter"

    private const val NEGOTIATION: String = "$CORE.negotiation"

    private const val CONDITIONAL: String = "$CORE.conditional"

    val routes: ClassName = ClassName(OPERATION, "Routes")

    val operation: ClassName = ClassName(OPERATION, "Operation")

    val schemeUse: ClassName = ClassName(OPERATION, "SchemeUse")

    val rateLimit: ClassName = ClassName(OPERATION, "OperationRateLimit")

    val rateLimitKey: ClassName = ClassName(OPERATION, "RateLimitKey")

    val duration: ClassName = ClassName("kotlin.time", "Duration")

    val source: ClassName = ClassName(OPERATION, "CredentialSource")

    val request: ClassName = ClassName(REQUEST, "Request")

    val place: ClassName = ClassName(REQUEST, "Place")

    val text: ClassName = ClassName(REQUEST, "ParameterType")

    val upload: ClassName = ClassName(REQUEST, "Upload")

    val reply: ClassName = ClassName(REPLY, "Reply")

    val replying: MemberName = MemberName(packageName = REPLY, simpleName = "reply")

    val conditional: MemberName = MemberName(packageName = REPLY, simpleName = "conditional", isExtension = true)

    val format: ClassName = ClassName(PARAMETER, "ParameterFormat")

    val style: ClassName = ClassName(PARAMETER, "ParameterStyle")

    val mediaType: ClassName = ClassName(NEGOTIATION, "MediaType")

    val mediaRange: ClassName = ClassName(NEGOTIATION, "MediaRange")

    val state: ClassName = ClassName(CONDITIONAL, "RepresentationState")

    val entityTag: ClassName = ClassName(CONDITIONAL, "EntityTag")

    val httpDate: ClassName = ClassName(CONDITIONAL, "HttpDate")

    val filePart: ClassName = ClassName("$CORE.wire", "FilePart")

    fun mediaType(text: String): CodeBlock = CodeBlock.of("%T.parse(%S)", mediaType, text)

    fun ranges(text: String): CodeBlock = CodeBlock.of("listOf(%T.of(%S))", mediaRange, text)
}
