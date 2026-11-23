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
import com.squareup.kotlinpoet.MemberName

internal object ServerTypes {
    private const val SERVER: String = "com.davils.kapi.runtime.server"

    private const val OPERATION: String = "$SERVER.operation"

    private const val REQUEST: String = "$SERVER.request"

    private const val PARAMETER: String = "com.davils.kapi.runtime.core.parameter"

    val server: ClassName = ClassName(SERVER, "Server")

    val operation: ClassName = ClassName(OPERATION, "Operation")

    val schemeUse: ClassName = ClassName(OPERATION, "SchemeUse")

    val source: ClassName = ClassName(OPERATION, "CredentialSource")

    val request: ClassName = ClassName(REQUEST, "Request")

    val place: ClassName = ClassName(REQUEST, "Place")

    val text: ClassName = ClassName(REQUEST, "Text")

    val reply: ClassName = ClassName("$SERVER.reply", "Reply")

    val route: ClassName = ClassName("io.ktor.server.routing", "Route")

    val format: ClassName = ClassName(PARAMETER, "ParameterFormat")

    val style: ClassName = ClassName(PARAMETER, "ParameterStyle")

    val bind: MemberName = MemberName("$SERVER.ktor", "operation", isExtension = true)
}
