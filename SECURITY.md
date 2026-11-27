# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.1.x   | Yes       |
| < 1.1   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/kapi/security/advisories/new),
or by email to development@davils.com.

Please include the affected kapi version, module and target (JVM or Wasm), a description of
the impact, the steps to reproduce it, ideally as a Kotlin test, and any mitigation you are aware of. We acknowledge a
report within three working days and agree on a disclosure date with you.

## Threat Model

kapi is a library and a build tool: it runs in the process and with the privileges of the application or the build
that uses it, and it trusts the platform below it, the JDK on the JVM and the JavaScript host on Wasm, and the Ktor
engine the application chooses. It assumes that an attacker controls every document kapi reads, every request a
generated server receives and every response, event and frame a generated client receives.

## Known Limitations

- **DNS rebinding.** The destination policy checks a name and, with a resolver, every address it resolves to, but the
  Ktor engine of the application resolves the name again when it connects, and kapi cannot pin the address it
  checked. An application that calls hosts an attacker may name should resolve through the engine itself and pass
  the addresses to `DestinationPolicy.checkResolved`, such as in the `Dns` of an OkHttp engine, or list the hosts it
  calls in `DestinationPolicy.hosts`.
- **Replayed webhooks.** A signature of RFC 9421 can be replayed for as long as `SignaturePolicy.maxAge` and its
  `expires` allow, unless the application remembers accepted signatures with a `ReplayGuard` in
  `SignaturePolicy.replays`.
- **Unique models.** `uniqueItems` over strings, numbers and enums is checked by sorting; over models and other
  values it uses `equals` and `hashCode`, so its cost is bounded by the size of the body only.
- **Deliveries before authorization.** A webhook delivery is read as a whole within `maxRequestBytes` and verified
  before it is authorized, so an unauthorized sender can make the server read that much.
