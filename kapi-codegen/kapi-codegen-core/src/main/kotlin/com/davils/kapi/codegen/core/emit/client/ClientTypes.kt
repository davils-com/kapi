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


package com.davils.kapi.codegen.core.emit.client

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

internal object ClientTypes {
    private const val CLIENT: String = "com.davils.kapi.runtime.client"

    private const val CALL: String = "com.davils.kapi.runtime.client.call"

    private const val PARAMETER: String = "com.davils.kapi.runtime.core.parameter"

    private const val BUILTINS: String = "kotlinx.serialization.builtins"

    val client: ClassName = ClassName(CLIENT, "Client")

    val exchange: ClassName = ClassName(CALL, "Exchange")

    val answered: ClassName = exchange.nestedClass("Answered")

    val failed: ClassName = exchange.nestedClass("Failed")

    val failure: ClassName = ClassName(CALL, "CallFailure")

    val securityUse: ClassName = ClassName(CALL, "SecurityUse")

    val placement: ClassName = ClassName(CALL, "CredentialPlacement")

    val apiCall: MemberName = MemberName(CALL, "apiCall")

    val format: ClassName = ClassName(PARAMETER, "ParameterFormat")

    val style: ClassName = ClassName(PARAMETER, "ParameterStyle")

    val value: ClassName = ClassName(PARAMETER, "ParameterValue")

    val headers: ClassName = ClassName("com.davils.kapi.runtime.core.response", "Headers")

    val decoding: ClassName = ClassName("com.davils.kapi.runtime.core.codec", "Decoding")

    val decodeToString: MemberName = MemberName("kotlinx.io.bytestring", "decodeToString")

    val milliseconds: MemberName =
        MemberName(ClassName("kotlin.time", "Duration").nestedClass("Companion"), "milliseconds")

    val serializer: MemberName = MemberName(BUILTINS, "serializer")

    val nullable: MemberName = MemberName(BUILTINS, "nullable")

    val listSerializer: MemberName = MemberName(BUILTINS, "ListSerializer")

    val mapSerializer: MemberName = MemberName(BUILTINS, "MapSerializer")
}
