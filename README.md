<div align="center">
  <img src="assets/banner.png" alt="Onyx4J — The Java API for Onyx" width="100%" />

  <br />
  <br />

  <img src="https://img.shields.io/badge/Language-Java-9b8acb?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=15151c" alt="Java" />
  <img src="https://img.shields.io/badge/Status-In_Development-a78bfa?style=for-the-badge&labelColor=15151c" alt="In development" />
  <img src="https://img.shields.io/badge/Platform-Onyx-aaa5bd?style=for-the-badge&labelColor=15151c" alt="Onyx" />

  <h3>Build. Connect. Extend.</h3>
  <p>A modern Java API for building applications, bots and integrations for Onyx.</p>

  <a href="#about">About</a> &nbsp;·&nbsp;
  <a href="#features">Features</a> &nbsp;·&nbsp;
  <a href="#getting-started">Getting Started</a> &nbsp;·&nbsp;
  <a href="#roadmap">Roadmap</a> &nbsp;·&nbsp;
  <a href="#contributing">Contributing</a>
</div>

---

> [!IMPORTANT]
> Onyx4J is currently under active development. APIs, methods and internal structures may change before the first stable release.

## About

**Onyx4J** is a Java library designed to provide developers with a clean and intuitive way to interact with the Onyx platform.

Its goal is to simplify communication with the Onyx API and Gateway, allowing developers to focus on building applications rather than managing the underlying protocol.

## Features

The intended scope of Onyx4J includes:

| Feature | Description |
| :--- | :--- |
| REST API | Access and manage Onyx resources through a structured Java interface. |
| Gateway | Receive real-time events through a persistent WebSocket connection. |
| Event System | React to platform events with dedicated Java listeners. |
| Entity Management | Work with users, servers, channels and messages. |
| Permissions | Interact with roles and permission structures. |
| Asynchronous Requests | Perform API operations without blocking application threads. |
| Reconnection | Restore gateway connectivity after interruptions. |

## Getting Started

### Requirements

- A compatible Java Development Kit (JDK)
- Maven or Gradle
- An Onyx application and its authentication credentials

The supported Java version and exact build requirements will be documented before the initial release.

### Installation

Dependency coordinates and repository information will be published once Onyx4J is available as a package.

**Gradle (Kotlin DSL)**

```kotlin
dependencies {
    implementation("<group-id>:<artifact-id>:<version>")
}
```

**Maven**

```xml
<dependency>
    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
    <version>VERSION</version>
</dependency>
```

### Quick Start

A working authentication and event-listener example will be added once the public API is finalized.

## Documentation

| Resource | Status |
| :--- | :--- |
| Getting Started Guide | Planned |
| Java API Reference | Planned |
| Gateway Documentation | Planned |
| Example Applications | Planned |

## Roadmap

The following milestones describe the intended release scope, not the current implementation status.

- [ ] Core client and authentication
- [ ] REST API integration
- [ ] Gateway and connection lifecycle
- [ ] Event dispatch and listener API
- [ ] Entities and caching
- [ ] Roles and permissions
- [ ] Error handling and rate limits
- [ ] Documentation and example projects
- [ ] First public release

## Contributing

Feedback, bug reports and feature suggestions are welcome. Please use the repository's issue tracker where available and include enough information to reproduce any reported issue.

## Security

Never commit authentication tokens, private keys or other credentials to the repository. Security vulnerabilities should be reported privately to the project maintainers.

## License

The project license will be specified before distribution. Refer to the repository's `LICENSE` file once available.

---

<div align="center">
  <strong>Onyx4J</strong>
  <br />
  The Java API for Onyx
  <br />
  <sub>API &nbsp;·&nbsp; Community &nbsp;·&nbsp; Development</sub>
</div>
