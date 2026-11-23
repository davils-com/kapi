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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal object Annotations {
    fun generated(origin: String): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Kotlin.generated)
        annotation.addMember("by = %S", Kotlin.GENERATOR)
        annotation.addMember("from = %S", origin)
        return annotation.build()
    }

    fun annotated(type: TypeSpec, origin: String): TypeSpec {
        val builder = type.toBuilder()
        builder.addAnnotation(generated(origin))
        return builder.build()
    }

    fun serialName(name: String): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Kotlin.serialName)
        annotation.addMember("%S", name)
        return annotation.build()
    }

    fun serializedWith(serializer: ClassName): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Kotlin.serializable)
        annotation.addMember("with = %T::class", serializer)
        return annotation.build()
    }

    fun neverDefault(): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Kotlin.encodeDefault)
        annotation.addMember("%T.Mode.NEVER", Kotlin.encodeDefault)
        return annotation.build()
    }

    fun experimentalSerialization(): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Kotlin.optIn)
        annotation.addMember("%T::class", Kotlin.experimentalSerialization)
        return annotation.build()
    }
}
