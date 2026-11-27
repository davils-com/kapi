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

package com.davils.kapi.runtime.server.tls.fixture

import com.davils.kore.core.outcome.getOrNull
import com.davils.krypt.authority.CertificateAuthority
import com.davils.krypt.authority.IssuanceRequest
import com.davils.krypt.authority.profile.CertificateProfile
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.keystore.Identities
import com.davils.krypt.keystore.Identity
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.tls.ClientAuthentication
import com.davils.krypt.tls.TlsContext
import com.davils.krypt.tls.TlsRole
import com.davils.krypt.tls.tls
import com.davils.krypt.x509.name.GeneralName
import com.davils.krypt.x509.name.distinguishedName
import com.davils.krypt.x509.path.TrustAnchor
import kotlin.time.Duration.Companion.days

private const val HOST: String = "server.example"

private const val DAYS: Int = 30

private const val LEAF: String = "leaf"

private val crypto: Crypto = Crypto()

internal class Pki private constructor(private val authority: CertificateAuthority) {
    private val anchor: TrustAnchor get() = TrustAnchor.of(authority.certificate)

    suspend fun identity(profile: CertificateProfile, name: GeneralName): Identity {
        val pair = crypto.keys.generate(KeyType.P256)
        val request = IssuanceRequest(
            subject = distinguishedName { organization(LEAF) },
            publicKey = pair.publicKey,
            profile = profile,
            names = listOf(name),
            validity = DAYS.days,
        )
        val outcome = authority.issue(request)
        val issued = checkNotNull(outcome.getOrNull()) { "The authority refused the request: $outcome" }
        val identity = Identities(crypto).of(pair.privateKey, listOf(issued))
        return checkNotNull(identity.getOrNull()) { "The key does not fit" }
    }

    suspend fun connect(client: Identity?, authentication: ClientAuthentication): Connection {
        val server = identity(CertificateProfile.TlsServer, GeneralName.Dns(HOST))
        val serverContext = TlsContext(
            tls(TlsRole.Server, crypto) {
                identities = listOf(server)
                clientAuthentication = authentication
                trust { anchors = listOf(anchor) }
            },
        )
        val clientContext = TlsContext(
            tls(TlsRole.Client, crypto) {
                identities = listOfNotNull(client)
                trust { anchors = listOf(anchor) }
            },
        )
        val connection = Connection(client = clientContext.createEngine(HOST), server = serverContext.createEngine())
        return connection.handshake()
    }

    companion object {
        suspend fun create(): Pki {
            val pair = crypto.keys.generate(KeyType.P256)
            val signer = crypto.signatures.signer(pair.privateKey)
            val root = CertificateAuthority.createRoot(
                crypto = crypto,
                signer = signer,
                subject = distinguishedName { commonName("Root") },
                validity = (DAYS * 2).days,
            )
            val authority = CertificateAuthority(
                crypto = crypto,
                certificate = root,
                signer = signer,
                repository = MemoryRepository(),
            )
            return Pki(authority)
        }
    }
}
