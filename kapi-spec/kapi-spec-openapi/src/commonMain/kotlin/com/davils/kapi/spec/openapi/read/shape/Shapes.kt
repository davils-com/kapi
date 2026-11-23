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

package com.davils.kapi.spec.openapi.read.shape

import com.davils.kapi.spec.openapi.VersionFamily

internal object Shapes {
    val document: ObjectShape = ObjectShape(
        subject = "The document",
        fields = listOf(
            FieldRule(Keys.OPENAPI, FieldKind.Value, isRequired = true),
            FieldRule(Keys.SELF, FieldKind.Text, since = VersionFamily.V32),
            FieldRule(Keys.INFO, FieldKind.Mapping, isRequired = true),
            FieldRule(Keys.SERVERS, FieldKind.Sequence),
            FieldRule(Keys.PATHS, FieldKind.Mapping),
            FieldRule(Keys.COMPONENTS, FieldKind.Mapping),
            FieldRule(Keys.SECURITY, FieldKind.Sequence),
            FieldRule(Keys.TAGS, FieldKind.Sequence),
            FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
            FieldRule(Keys.JSON_SCHEMA_DIALECT, FieldKind.Text, since = VersionFamily.V31),
            FieldRule(Keys.WEBHOOKS, FieldKind.Mapping, since = VersionFamily.V31),
        ),
    )

    val info: ObjectShape = ObjectShape(
        subject = "The info",
        fields = listOf(
            FieldRule(Keys.TITLE, FieldKind.Text, isRequired = true),
            FieldRule(Keys.VERSION, FieldKind.Text, isRequired = true),
            FieldRule(Keys.SUMMARY, FieldKind.Text, since = VersionFamily.V31),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.TERMS_OF_SERVICE, FieldKind.Text),
            FieldRule(Keys.CONTACT, FieldKind.Mapping),
            FieldRule(Keys.LICENSE, FieldKind.Mapping),
        ),
    )

    val contact: ObjectShape = ObjectShape(
        subject = "A contact",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text),
            FieldRule(Keys.URL, FieldKind.Text),
            FieldRule(Keys.EMAIL, FieldKind.Text),
        ),
    )

    val license: ObjectShape = ObjectShape(
        subject = "A license",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.IDENTIFIER, FieldKind.Text, since = VersionFamily.V31),
            FieldRule(Keys.URL, FieldKind.Text),
        ),
    )

    val server: ObjectShape = ObjectShape(
        subject = "A server",
        fields = listOf(
            FieldRule(Keys.URL, FieldKind.Text, isRequired = true),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.NAME, FieldKind.Text, since = VersionFamily.V32),
            FieldRule(Keys.VARIABLES, FieldKind.Mapping),
        ),
    )

    val serverVariable: ObjectShape = ObjectShape(
        subject = "A server variable",
        fields = listOf(
            FieldRule(Keys.ENUM, FieldKind.Sequence),
            FieldRule(Keys.DEFAULT, FieldKind.Text, isRequired = true),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
        ),
    )

    val externalDocs: ObjectShape = ObjectShape(
        subject = "External documentation",
        fields = listOf(
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.URL, FieldKind.Text, isRequired = true),
        ),
    )

    val tag: ObjectShape = ObjectShape(
        subject = "A tag",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.SUMMARY, FieldKind.Text, since = VersionFamily.V32),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
            FieldRule(Keys.PARENT, FieldKind.Text, since = VersionFamily.V32),
            FieldRule(Keys.KIND, FieldKind.Text, since = VersionFamily.V32),
        ),
    )

    val pathItem: ObjectShape = ObjectShape(
        subject = "A path item",
        fields = listOf(
            FieldRule(Keys.REFERENCE, FieldKind.Text),
            FieldRule(Keys.SUMMARY, FieldKind.Text),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.GET, FieldKind.Mapping),
            FieldRule(Keys.PUT, FieldKind.Mapping),
            FieldRule(Keys.POST, FieldKind.Mapping),
            FieldRule(Keys.DELETE, FieldKind.Mapping),
            FieldRule(Keys.OPTIONS, FieldKind.Mapping),
            FieldRule(Keys.HEAD, FieldKind.Mapping),
            FieldRule(Keys.PATCH, FieldKind.Mapping),
            FieldRule(Keys.TRACE, FieldKind.Mapping),
            FieldRule(Keys.QUERY, FieldKind.Mapping, since = VersionFamily.V32),
            FieldRule(Keys.ADDITIONAL_OPERATIONS, FieldKind.Mapping, since = VersionFamily.V32),
            FieldRule(Keys.SERVERS, FieldKind.Sequence),
            FieldRule(Keys.PARAMETERS, FieldKind.Sequence),
        ),
    )

    val operation: ObjectShape = ObjectShape(
        subject = "An operation",
        fields = listOf(
            FieldRule(Keys.TAGS, FieldKind.Sequence),
            FieldRule(Keys.SUMMARY, FieldKind.Text),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
            FieldRule(Keys.OPERATION_ID, FieldKind.Text),
            FieldRule(Keys.PARAMETERS, FieldKind.Sequence),
            FieldRule(Keys.REQUEST_BODY, FieldKind.Mapping),
            FieldRule(Keys.RESPONSES, FieldKind.Mapping),
            FieldRule(Keys.CALLBACKS, FieldKind.Mapping),
            FieldRule(Keys.DEPRECATED, FieldKind.Flag),
            FieldRule(Keys.SECURITY, FieldKind.Sequence),
            FieldRule(Keys.SERVERS, FieldKind.Sequence),
        ),
    )

    val parameter: ObjectShape = ObjectShape(
        subject = "A parameter",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.IN, FieldKind.Text, isRequired = true),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.REQUIRED, FieldKind.Flag),
            FieldRule(Keys.DEPRECATED, FieldKind.Flag),
            FieldRule(Keys.ALLOW_EMPTY_VALUE, FieldKind.Flag),
            FieldRule(Keys.STYLE, FieldKind.Text),
            FieldRule(Keys.EXPLODE, FieldKind.Flag),
            FieldRule(Keys.ALLOW_RESERVED, FieldKind.Flag),
            FieldRule(Keys.SCHEMA, FieldKind.Schema),
            FieldRule(Keys.EXAMPLE, FieldKind.Value),
            FieldRule(Keys.EXAMPLES, FieldKind.Mapping),
            FieldRule(Keys.CONTENT, FieldKind.Mapping),
        ),
    )

    val header: ObjectShape = ObjectShape(
        subject = "A header",
        fields = listOf(
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.REQUIRED, FieldKind.Flag),
            FieldRule(Keys.DEPRECATED, FieldKind.Flag),
            FieldRule(Keys.ALLOW_EMPTY_VALUE, FieldKind.Flag),
            FieldRule(Keys.STYLE, FieldKind.Text),
            FieldRule(Keys.EXPLODE, FieldKind.Flag),
            FieldRule(Keys.ALLOW_RESERVED, FieldKind.Flag),
            FieldRule(Keys.SCHEMA, FieldKind.Schema),
            FieldRule(Keys.EXAMPLE, FieldKind.Value),
            FieldRule(Keys.EXAMPLES, FieldKind.Mapping),
            FieldRule(Keys.CONTENT, FieldKind.Mapping),
        ),
    )

    val requestBody: ObjectShape = ObjectShape(
        subject = "A request body",
        fields = listOf(
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.CONTENT, FieldKind.Mapping, isRequired = true),
            FieldRule(Keys.REQUIRED, FieldKind.Flag),
        ),
    )

    val mediaType: ObjectShape = ObjectShape(
        subject = "A media type",
        fields = listOf(
            FieldRule(Keys.SCHEMA, FieldKind.Schema),
            FieldRule(Keys.ITEM_SCHEMA, FieldKind.Schema, since = VersionFamily.V32),
            FieldRule(Keys.EXAMPLE, FieldKind.Value),
            FieldRule(Keys.EXAMPLES, FieldKind.Mapping),
            FieldRule(Keys.ENCODING, FieldKind.Mapping),
            FieldRule(Keys.PREFIX_ENCODING, FieldKind.Sequence, since = VersionFamily.V32),
            FieldRule(Keys.ITEM_ENCODING, FieldKind.Mapping, since = VersionFamily.V32),
        ),
    )

    val encoding: ObjectShape = ObjectShape(
        subject = "An encoding",
        fields = listOf(
            FieldRule(Keys.CONTENT_TYPE, FieldKind.Text),
            FieldRule(Keys.HEADERS, FieldKind.Mapping),
            FieldRule(Keys.STYLE, FieldKind.Text),
            FieldRule(Keys.EXPLODE, FieldKind.Flag),
            FieldRule(Keys.ALLOW_RESERVED, FieldKind.Flag),
            FieldRule(Keys.ENCODING, FieldKind.Mapping, since = VersionFamily.V32),
            FieldRule(Keys.PREFIX_ENCODING, FieldKind.Sequence, since = VersionFamily.V32),
            FieldRule(Keys.ITEM_ENCODING, FieldKind.Mapping, since = VersionFamily.V32),
        ),
    )

    val response: ObjectShape = ObjectShape(
        subject = "A response",
        fields = listOf(
            FieldRule(Keys.SUMMARY, FieldKind.Text, since = VersionFamily.V32),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text, isRequired = true, optionalSince = VersionFamily.V32),
            FieldRule(Keys.HEADERS, FieldKind.Mapping),
            FieldRule(Keys.CONTENT, FieldKind.Mapping),
            FieldRule(Keys.LINKS, FieldKind.Mapping),
        ),
    )

    val example: ObjectShape = ObjectShape(
        subject = "An example",
        fields = listOf(
            FieldRule(Keys.SUMMARY, FieldKind.Text),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.VALUE, FieldKind.Value),
            FieldRule(Keys.EXTERNAL_VALUE, FieldKind.Text),
            FieldRule(Keys.DATA_VALUE, FieldKind.Value, since = VersionFamily.V32),
            FieldRule(Keys.SERIALIZED_VALUE, FieldKind.Text, since = VersionFamily.V32),
        ),
    )

    val link: ObjectShape = ObjectShape(
        subject = "A link",
        fields = listOf(
            FieldRule(Keys.OPERATION_REF, FieldKind.Text),
            FieldRule(Keys.OPERATION_ID, FieldKind.Text),
            FieldRule(Keys.PARAMETERS, FieldKind.Mapping),
            FieldRule(Keys.REQUEST_BODY, FieldKind.Value),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.SERVER, FieldKind.Mapping),
        ),
    )

    val components: ObjectShape = ObjectShape(
        subject = "The components",
        fields = listOf(
            FieldRule(Keys.SCHEMAS, FieldKind.Mapping),
            FieldRule(Keys.RESPONSES, FieldKind.Mapping),
            FieldRule(Keys.PARAMETERS, FieldKind.Mapping),
            FieldRule(Keys.EXAMPLES, FieldKind.Mapping),
            FieldRule(Keys.REQUEST_BODIES, FieldKind.Mapping),
            FieldRule(Keys.HEADERS, FieldKind.Mapping),
            FieldRule(Keys.SECURITY_SCHEMES, FieldKind.Mapping),
            FieldRule(Keys.LINKS, FieldKind.Mapping),
            FieldRule(Keys.CALLBACKS, FieldKind.Mapping),
            FieldRule(Keys.PATH_ITEMS, FieldKind.Mapping, since = VersionFamily.V31),
            FieldRule(Keys.MEDIA_TYPES, FieldKind.Mapping, since = VersionFamily.V32),
        ),
    )

    val securityScheme: ObjectShape = ObjectShape(
        subject = "A security scheme",
        fields = listOf(
            FieldRule(Keys.TYPE, FieldKind.Text, isRequired = true),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.NAME, FieldKind.Text),
            FieldRule(Keys.IN, FieldKind.Text),
            FieldRule(Keys.SCHEME, FieldKind.Text),
            FieldRule(Keys.BEARER_FORMAT, FieldKind.Text),
            FieldRule(Keys.FLOWS, FieldKind.Mapping),
            FieldRule(Keys.OPEN_ID_CONNECT_URL, FieldKind.Text),
            FieldRule(Keys.OAUTH2_METADATA_URL, FieldKind.Text, since = VersionFamily.V32),
            FieldRule(Keys.DEPRECATED, FieldKind.Flag, since = VersionFamily.V32),
        ),
    )

    val oauthFlows: ObjectShape = ObjectShape(
        subject = "The flows of a security scheme",
        fields = listOf(
            FieldRule(Keys.IMPLICIT, FieldKind.Mapping),
            FieldRule(Keys.PASSWORD, FieldKind.Mapping),
            FieldRule(Keys.CLIENT_CREDENTIALS, FieldKind.Mapping),
            FieldRule(Keys.AUTHORIZATION_CODE, FieldKind.Mapping),
            FieldRule(Keys.DEVICE_AUTHORIZATION, FieldKind.Mapping, since = VersionFamily.V32),
        ),
    )

    val oauthFlow: ObjectShape = ObjectShape(
        subject = "A flow of a security scheme",
        fields = listOf(
            FieldRule(Keys.AUTHORIZATION_URL, FieldKind.Text),
            FieldRule(Keys.TOKEN_URL, FieldKind.Text),
            FieldRule(Keys.REFRESH_URL, FieldKind.Text),
            FieldRule(Keys.SCOPES, FieldKind.Mapping),
        ),
    )

    val deviceFlow: ObjectShape = ObjectShape(
        subject = "A device authorization flow of a security scheme",
        fields = listOf(
            FieldRule(Keys.DEVICE_AUTHORIZATION_URL, FieldKind.Text, isRequired = true),
            FieldRule(Keys.TOKEN_URL, FieldKind.Text, isRequired = true),
            FieldRule(Keys.REFRESH_URL, FieldKind.Text),
            FieldRule(Keys.SCOPES, FieldKind.Mapping),
        ),
    )

    val discriminator: ObjectShape = ObjectShape(
        subject = "A discriminator",
        fields = listOf(
            FieldRule(Keys.PROPERTY_NAME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.MAPPING, FieldKind.Mapping),
            FieldRule(Keys.DEFAULT_MAPPING, FieldKind.Text, since = VersionFamily.V32),
        ),
    )

    val xml: ObjectShape = ObjectShape(
        subject = "The xml of a schema",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text),
            FieldRule(Keys.NAMESPACE, FieldKind.Text),
            FieldRule(Keys.PREFIX, FieldKind.Text),
            FieldRule(Keys.ATTRIBUTE, FieldKind.Flag),
            FieldRule(Keys.WRAPPED, FieldKind.Flag),
            FieldRule(Keys.NODE_TYPE, FieldKind.Text, since = VersionFamily.V32),
        ),
    )

    val schema30: ObjectShape = ObjectShape(
        subject = "A schema",
        fields = listOf(
            FieldRule(Keys.REFERENCE, FieldKind.Text),
            FieldRule(Keys.TITLE, FieldKind.Text),
            FieldRule(Keys.MULTIPLE_OF, FieldKind.Number),
            FieldRule(Keys.MAXIMUM, FieldKind.Number),
            FieldRule(Keys.EXCLUSIVE_MAXIMUM, FieldKind.Flag),
            FieldRule(Keys.MINIMUM, FieldKind.Number),
            FieldRule(Keys.EXCLUSIVE_MINIMUM, FieldKind.Flag),
            FieldRule(Keys.MAX_LENGTH, FieldKind.Integer),
            FieldRule(Keys.MIN_LENGTH, FieldKind.Integer),
            FieldRule(Keys.PATTERN, FieldKind.Text),
            FieldRule(Keys.MAX_ITEMS, FieldKind.Integer),
            FieldRule(Keys.MIN_ITEMS, FieldKind.Integer),
            FieldRule(Keys.UNIQUE_ITEMS, FieldKind.Flag),
            FieldRule(Keys.MAX_PROPERTIES, FieldKind.Integer),
            FieldRule(Keys.MIN_PROPERTIES, FieldKind.Integer),
            FieldRule(Keys.REQUIRED, FieldKind.Sequence),
            FieldRule(Keys.ENUM, FieldKind.Sequence),
            FieldRule(Keys.TYPE, FieldKind.Text),
            FieldRule(Keys.ALL_OF, FieldKind.Sequence),
            FieldRule(Keys.ONE_OF, FieldKind.Sequence),
            FieldRule(Keys.ANY_OF, FieldKind.Sequence),
            FieldRule(Keys.NOT, FieldKind.Schema),
            FieldRule(Keys.ITEMS, FieldKind.Schema),
            FieldRule(Keys.PROPERTIES, FieldKind.Mapping),
            FieldRule(Keys.ADDITIONAL_PROPERTIES, FieldKind.Schema),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.FORMAT, FieldKind.Text),
            FieldRule(Keys.DEFAULT, FieldKind.Value),
            FieldRule(Keys.NULLABLE, FieldKind.Flag),
            FieldRule(Keys.DISCRIMINATOR, FieldKind.Mapping),
            FieldRule(Keys.READ_ONLY, FieldKind.Flag),
            FieldRule(Keys.WRITE_ONLY, FieldKind.Flag),
            FieldRule(Keys.XML, FieldKind.Mapping),
            FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
            FieldRule(Keys.EXAMPLE, FieldKind.Value),
            FieldRule(Keys.DEPRECATED, FieldKind.Flag),
        ),
    )

    val schema31: ObjectShape = ObjectShape(
        subject = "A schema",
        fields = listOf(
            FieldRule(Keys.REFERENCE, FieldKind.Text),
            FieldRule(Keys.SCHEMA_ID, FieldKind.Text),
            FieldRule(Keys.DIALECT, FieldKind.Text),
            FieldRule(Keys.ANCHOR, FieldKind.Text),
            FieldRule(Keys.DYNAMIC_ANCHOR, FieldKind.Text),
            FieldRule(Keys.DYNAMIC_REFERENCE, FieldKind.Text),
            FieldRule(Keys.COMMENT, FieldKind.Text),
            FieldRule(Keys.DEFINITIONS, FieldKind.Mapping),
            FieldRule(Keys.TITLE, FieldKind.Text),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.DEFAULT, FieldKind.Value),
            FieldRule(Keys.DEPRECATED, FieldKind.Flag),
            FieldRule(Keys.READ_ONLY, FieldKind.Flag),
            FieldRule(Keys.WRITE_ONLY, FieldKind.Flag),
            FieldRule(Keys.EXAMPLES, FieldKind.Sequence),
            FieldRule(Keys.EXAMPLE, FieldKind.Value),
            FieldRule(Keys.TYPE, FieldKind.TextOrSequence),
            FieldRule(Keys.ENUM, FieldKind.Sequence),
            FieldRule(Keys.CONST, FieldKind.Value),
            FieldRule(Keys.MULTIPLE_OF, FieldKind.Number),
            FieldRule(Keys.MAXIMUM, FieldKind.Number),
            FieldRule(Keys.EXCLUSIVE_MAXIMUM, FieldKind.Number),
            FieldRule(Keys.MINIMUM, FieldKind.Number),
            FieldRule(Keys.EXCLUSIVE_MINIMUM, FieldKind.Number),
            FieldRule(Keys.MAX_LENGTH, FieldKind.Integer),
            FieldRule(Keys.MIN_LENGTH, FieldKind.Integer),
            FieldRule(Keys.PATTERN, FieldKind.Text),
            FieldRule(Keys.FORMAT, FieldKind.Text),
            FieldRule(Keys.ITEMS, FieldKind.Schema),
            FieldRule(Keys.PREFIX_ITEMS, FieldKind.Sequence),
            FieldRule(Keys.CONTAINS, FieldKind.Schema),
            FieldRule(Keys.MIN_CONTAINS, FieldKind.Integer),
            FieldRule(Keys.MAX_CONTAINS, FieldKind.Integer),
            FieldRule(Keys.MAX_ITEMS, FieldKind.Integer),
            FieldRule(Keys.MIN_ITEMS, FieldKind.Integer),
            FieldRule(Keys.UNIQUE_ITEMS, FieldKind.Flag),
            FieldRule(Keys.UNEVALUATED_ITEMS, FieldKind.Schema),
            FieldRule(Keys.PROPERTIES, FieldKind.Mapping),
            FieldRule(Keys.PATTERN_PROPERTIES, FieldKind.Mapping),
            FieldRule(Keys.ADDITIONAL_PROPERTIES, FieldKind.Schema),
            FieldRule(Keys.PROPERTY_NAMES, FieldKind.Schema),
            FieldRule(Keys.REQUIRED, FieldKind.Sequence),
            FieldRule(Keys.DEPENDENT_REQUIRED, FieldKind.Mapping),
            FieldRule(Keys.DEPENDENT_SCHEMAS, FieldKind.Mapping),
            FieldRule(Keys.MAX_PROPERTIES, FieldKind.Integer),
            FieldRule(Keys.MIN_PROPERTIES, FieldKind.Integer),
            FieldRule(Keys.UNEVALUATED_PROPERTIES, FieldKind.Schema),
            FieldRule(Keys.ALL_OF, FieldKind.Sequence),
            FieldRule(Keys.ANY_OF, FieldKind.Sequence),
            FieldRule(Keys.ONE_OF, FieldKind.Sequence),
            FieldRule(Keys.NOT, FieldKind.Schema),
            FieldRule(Keys.IF, FieldKind.Schema),
            FieldRule(Keys.THEN, FieldKind.Schema),
            FieldRule(Keys.ELSE, FieldKind.Schema),
            FieldRule(Keys.CONTENT_ENCODING, FieldKind.Text),
            FieldRule(Keys.CONTENT_MEDIA_TYPE, FieldKind.Text),
            FieldRule(Keys.CONTENT_SCHEMA, FieldKind.Schema),
            FieldRule(Keys.DISCRIMINATOR, FieldKind.Mapping),
            FieldRule(Keys.XML, FieldKind.Mapping),
            FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
        ),
        isOpen = true,
    )
}
