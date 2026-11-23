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

package com.davils.kapi.runtime.server.security

import com.davils.kore.core.principal.Principal
import com.davils.krypt.x509.Certificate

/**
 * Authorizes a `mutualTLS` scheme of OpenAPI 3.2 §4.25 by the client certificate chain the transport verified.
 *
 * The server never validates a chain itself: the TLS transport of the application, such as the `tls { }` of krypt,
 * validates it against its trust anchors and hands it over in
 * [PeerAttributes.certificates][com.davils.kapi.runtime.server.PeerAttributes.certificates]. [identify] maps the
 * verified chain, the leaf first, to a caller, such as by the service identity of RFC 9525 of the leaf; a chain it does
 * not know and every other kind of credential are denied.
 *
 * @param identify returns the caller of a verified chain, or `null` if it is unknown.
 * @since 1.0.0
 */
public class CertificateAuthorizer(private val identify: suspend (List<Certificate>) -> Principal?) : Authorizer {
    override suspend fun authorize(attempt: AuthorizationAttempt): Authorization {
        val presented = attempt.credential as? PresentedCredential.Certificates
        val chain = presented?.run { chain.takeIf(List<Certificate>::isNotEmpty) }
        val principal = chain?.let { certificates -> identify(certificates) }
        return principal?.let(Authorization::Granted) ?: Authorization.Unknown
    }

    override fun toString(): String = "CertificateAuthorizer"
}
