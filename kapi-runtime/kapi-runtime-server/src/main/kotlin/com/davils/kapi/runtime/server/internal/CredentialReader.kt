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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterReading
import com.davils.kapi.runtime.core.parameter.ParameterShape
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kapi.runtime.core.parameter.readCookie
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.request.Request
import com.davils.kapi.runtime.server.security.PresentedCredential
import com.davils.kore.core.failure.contained
import kotlin.io.encoding.Base64

private const val AUTHORIZATION: String = "authorization"

private const val COOKIE: String = "cookie"

private const val BEARER: String = "Bearer"

private const val BASIC: String = "Basic"

private const val COLON: Char = ':'

internal object CredentialReader {
    fun presentedOf(source: CredentialSource, request: Request): PresentedCredential? {
        val credential = credentialOf(source, request)
        if (credential != null || isUnreadable(source, request)) return credential
        return request.peer.identity?.let(PresentedCredential::Peer)
    }

    private fun credentialOf(source: CredentialSource, request: Request): PresentedCredential? = when (source) {
        CredentialSource.Bearer -> {
            schemed(request, BEARER)?.let { token -> PresentedCredential.Token(Secret(token)) }
        }

        CredentialSource.Basic -> {
            schemed(request, BASIC)?.let(::basicOf)
        }

        is CredentialSource.Header -> {
            keyOf(request.headers.all(source.name).singleOrNull())
        }

        is CredentialSource.Query -> {
            keyOf(queried(request, source.name))
        }

        is CredentialSource.Cookie -> {
            keyOf(cookieOf(request, source.name))
        }

        CredentialSource.Certificate -> {
            request.peer.certificates
                .takeIf { chain -> chain.isNotEmpty() }
                ?.let(PresentedCredential::Certificates)
        }
    }

    private fun isUnreadable(source: CredentialSource, request: Request): Boolean {
        val isAuthorization = source == CredentialSource.Bearer || source == CredentialSource.Basic
        val isAmbiguous = isAuthorization && request.headers.all(AUTHORIZATION).size > 1
        val isBrokenBasic = source == CredentialSource.Basic && schemed(request, BASIC) != null
        return isAmbiguous || isBrokenBasic
    }

    private fun schemed(request: Request, scheme: String): String? {
        val values = request.headers.all(AUTHORIZATION)
        val value = values.singleOrNull().orEmpty()
        val prefix = "$scheme "
        val token = value.drop(prefix.length).trim()
        return token.takeIf { value.startsWith(prefix, ignoreCase = true) && token.isNotEmpty() }
    }

    private fun basicOf(text: String): PresentedCredential? {
        val decoded = contained { Base64.Default.decode(text).decodeToString(throwOnInvalidSequence = true) }
        val pair = decoded.getOrNull()?.takeIf { found -> COLON in found && HeaderChecks.isValue(found) }
        return pair?.let { found ->
            PresentedCredential.Basic(
                user = found.substringBefore(COLON),
                password = Secret(found.substringAfter(COLON)),
            )
        }
    }

    private fun keyOf(text: String?): PresentedCredential? =
        text?.takeIf(String::isNotBlank)?.let { key -> PresentedCredential.Key(Secret(key)) }

    private fun queried(request: Request, name: String): String? {
        val values = request.query.filter { (candidate, _) -> candidate == name }
        return values.singleOrNull()?.second
    }

    private fun cookieOf(request: Request, name: String): String? {
        val values = request.headers.all(COOKIE)
        val header = values.joinToString("; ").takeIf { values.isNotEmpty() }
        val format = ParameterFormat(name, ParameterStyle.Cookie, isExploded = false)
        val reading = format.readCookie(header, ParameterShape.Text) as? ParameterReading.Present
        return (reading?.value as? ParameterValue.Text)?.value
    }
}
