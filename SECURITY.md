# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

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
