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

internal class ObjectShape(
    val subject: String,
    fields: List<FieldRule>,
    val retired: Map<String, String> = emptyMap(),
) {
    private val rules: Map<String, FieldRule> = fields.associateBy(FieldRule::name)

    val names: Set<String> get() = rules.keys

    val required: List<FieldRule> get() = rules.values.filter(FieldRule::isRequired)

    fun rule(name: String): FieldRule? = rules[name]
}
