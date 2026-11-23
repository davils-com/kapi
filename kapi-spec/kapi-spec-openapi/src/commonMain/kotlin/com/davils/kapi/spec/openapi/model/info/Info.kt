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

package com.davils.kapi.spec.openapi.model.info

import com.davils.kapi.spec.core.model.Element

public class Info internal constructor(builder: InfoBuilder) : Element(builder) {
    public val title: String = builder.title

    public val version: String = builder.version

    public val summary: String? = builder.summary

    public val description: String? = builder.description

    public val termsOfService: String? = builder.termsOfService

    public val contact: Contact? = builder.contact

    public val license: License? = builder.license

    override val identity: List<Any?>
        get() = listOf(
            title,
            version,
            summary,
            description,
            termsOfService,
            contact,
            license,
        )
}
