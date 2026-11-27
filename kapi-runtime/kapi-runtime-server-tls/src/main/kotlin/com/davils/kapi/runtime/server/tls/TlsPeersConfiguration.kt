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

package com.davils.kapi.runtime.server.tls

import com.davils.kore.core.annotation.DavilsDsl
import io.ktor.server.application.ApplicationCall
import javax.net.ssl.SSLSession

/**
 * Configures [TlsPeers]: how the TLS session of the connection of a call is found.
 *
 * Ktor has no API of its own for the TLS session of a call, so the application names it for the engine it runs on,
 * such as the session of the `SslHandler` a Netty pipeline set up from the `SSLEngine` of a `TlsContext` of krypt. A
 * configuration is not thread-safe; it is read once, when the plugin is installed.
 *
 * @since 1.2.0
 */
@DavilsDsl
public class TlsPeersConfiguration internal constructor() {
    internal var reader: ((ApplicationCall) -> SSLSession?)? = null

    /**
     * Reads the TLS session of the connection a call came in over with [read], which returns `null` for a call that
     * did not come in over TLS.
     *
     * [read] is code of the application: it runs contained, and a call for which it throws is served as one without
     * a verified peer. Whatever session it returns, only an identity that the trust manager of krypt recorded in it
     * reaches the server, so a session of another trust manager, or one JSSE resumed without checking it again,
     * hands over no certificates.
     *
     * @param read returns the session of a call, or `null`.
     * @since 1.2.0
     */
    public fun session(read: (ApplicationCall) -> SSLSession?) {
        reader = read
    }
}
