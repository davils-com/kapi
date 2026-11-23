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

/**
 * The kind of XML node a schema maps to, the `nodeType` of an XML object of OpenAPI 3.2.
 *
 * @property keyword the value of `nodeType` in a document.
 * @since 1.0.0
 */
public enum class XmlNodeType(public val keyword: String) {
    /**
     * An element, which wraps the items of an array.
     *
     * @since 1.0.0
     */
    Element("element"),

    /**
     * An attribute of the element of the enclosing schema.
     *
     * @since 1.0.0
     */
    Attribute("attribute"),

    /**
     * The text of the element of the enclosing schema.
     *
     * @since 1.0.0
     */
    Text("text"),

    /**
     * A CDATA section in the element of the enclosing schema.
     *
     * @since 1.0.0
     */
    Cdata("cdata"),

    /**
     * No node of its own; the value or the items appear directly in the enclosing element.
     *
     * @since 1.0.0
     */
    None("none"),
    ;

    /**
     * Finds node types.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the node type written as [keyword].
         *
         * @param keyword the value of `nodeType`, which may be any text; it is compared exactly.
         * @return the node type, or `null` if [keyword] names none.
         * @since 1.0.0
         */
        public fun fromKeyword(keyword: String): XmlNodeType? = entries.firstOrNull { type -> type.keyword == keyword }
    }
}
