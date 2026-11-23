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

package com.davils.kapi.spec.openapi.model.schema

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of an [Xml], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the XML object it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlBuilder internal constructor() : ElementBuilder<Xml>() {
    override val subject: String get() = "openapi xml"

    /**
     * The value of `name`, the name of the element or attribute.
     *
     * @since 1.0.0
     */
    public var name: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `namespace`, the namespace of the element or attribute.
     *
     * @since 1.0.0
     */
    public var namespace: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `prefix`, the prefix of the namespace.
     *
     * @since 1.0.0
     */
    public var prefix: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `attribute`, whether the property is an attribute.
     *
     * @since 1.0.0
     */
    public var isAttribute: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `wrapped`, whether the items of an array are wrapped.
     *
     * @since 1.0.0
     */
    public var isWrapped: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `nodeType`, the kind of node the schema maps to, written from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    public var nodeType: XmlNodeType? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Xml = Xml(this)
}
