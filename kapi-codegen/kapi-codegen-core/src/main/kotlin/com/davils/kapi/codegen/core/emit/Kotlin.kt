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


package com.davils.kapi.codegen.core.emit

import com.squareup.kotlinpoet.ClassName

internal object Kotlin {
    const val GENERATOR: String = "kide-common-openapi-generator"

    private const val SERIALIZATION: String = "kotlinx.serialization"

    private const val JSON: String = "kotlinx.serialization.json"

    val serializable: ClassName = ClassName(SERIALIZATION, "Serializable")

    val serialName: ClassName = ClassName(SERIALIZATION, "SerialName")

    val encodeDefault: ClassName = ClassName(SERIALIZATION, "EncodeDefault")

    val experimentalSerialization: ClassName = ClassName(SERIALIZATION, "ExperimentalSerializationApi")

    val optIn: ClassName = ClassName("kotlin", "OptIn")

    val jsonElement: ClassName = ClassName(JSON, "JsonElement")

    val jsonObject: ClassName = ClassName(JSON, "JsonObject")

    val jsonPrimitive: ClassName = ClassName(JSON, "JsonPrimitive")

    val contentPolymorphic: ClassName = ClassName(JSON, "JsonContentPolymorphicSerializer")

    val serializer: ClassName = ClassName(SERIALIZATION, "KSerializer")

    val deserializationStrategy: ClassName = ClassName(SERIALIZATION, "DeserializationStrategy")

    val serializationException: ClassName = ClassName(SERIALIZATION, "SerializationException")

    val byteString: ClassName = ClassName("kotlinx.io.bytestring", "ByteString")

    val instant: ClassName = ClassName("kotlin.time", "Instant")

    val jvmInline: ClassName = ClassName("kotlin.jvm", "JvmInline")

    val regex: ClassName = ClassName("kotlin.text", "Regex")

    val generated: ClassName = ClassName("com.davils.kore.core.annotation", "DavilsGenerated")

    val secret: ClassName = ClassName("com.davils.kore.core.secret", "Secret")

    val transferObject: ClassName = ClassName("com.davils.kore.core.architecture.data", "DataTransferObject")

    val validatable: ClassName = ClassName("com.davils.kore.core.validation", "Validatable")

    val validationScope: ClassName = ClassName("com.davils.kore.core.validation", "ValidationScope")

    val constraints: ClassName = ClassName(
        "com.davils.kapi.runtime.core.validation",
        "Constraints",
    )

    val codes: ClassName = ClassName("com.davils.kapi.runtime.core.validation", "ViolationCodes")

    val patch: ClassName = ClassName("com.davils.kapi.runtime.core.wire", "Patch")

    val base64: ClassName = ClassName("com.davils.kapi.runtime.core.wire", "Base64Serializer")
}
