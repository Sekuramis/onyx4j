<div align="center">
  <img src="assets/banner.png" alt="Onyx4J — The Java API for Onyx" width="100%" />

  <br />
  <br />

  <a href="https://repo.onyx4j.de/#/releases/de/onyx4j/onyx4j"><img src="https://img.shields.io/badge/Release-1.0.0--RC2-a78bfa?style=for-the-badge&labelColor=15151c" alt="Release 1.0.0-RC2" /></a>
  <img src="https://img.shields.io/badge/Java-25+-9b8acb?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=15151c" alt="Java 25+" />
  <img src="https://img.shields.io/badge/Platform-Onyx-aaa5bd?style=for-the-badge&labelColor=15151c" alt="Onyx" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-a78bfa?style=for-the-badge&labelColor=15151c" alt="License Apache 2.0" /></a>
  <a href="https://github.com/Sekuramis/onyx4j/actions/workflows/ci.yml"><img src="https://github.com/Sekuramis/onyx4j/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  <a href="https://github.com/Sekuramis/onyx4j/releases"><img src="https://img.shields.io/github/v/release/Sekuramis/onyx4j?include_prereleases&style=for-the-badge&label=GitHub%20Release&color=a78bfa&labelColor=15151c" alt="GitHub Release" /></a>

  <h3>Build. Connect. Extend.</h3>
  <p>An independent, developer-first Java API for building applications, bots and integrations for Onyx.</p>

  <a href="https://onyx4j.de">Website</a> &nbsp;·&nbsp;
  <a href="https://docs.onyx4j.de">Documentation</a> &nbsp;·&nbsp;
  <a href="https://javadocs.onyx4j.de">JavaDocs</a> &nbsp;·&nbsp;
  <a href="https://repo.onyx4j.de">Maven Repository</a> &nbsp;·&nbsp;
  <a href="https://status.onyx4j.de">Status</a>
</div>

---

> [!IMPORTANT]
> **Onyx4J 1.0.0-RC2 is available.** This is a release candidate, not a stable release. Local automated tests cover the REST and gateway implementation, but full live integration validation is still pending. APIs may change before 1.0.0.

## About

Onyx4J provides a structured Java interface to the Onyx API and Gateway. It combines typed entities, annotation-driven command handling, event subscriptions and asynchronous REST operations so developers can focus on their applications rather than the underlying protocol.

Onyx4J is an independent project and is not an official Onyx SDK.

## Features

| Area | Capabilities |
| :--- | :--- |
| Client & Gateway | App authentication, Socket.IO connection, readiness, reconnect and connection statistics |
| REST API | Lazy `RestAction` operations, CompletableFuture, rate-limit handling and bounded retries |
| Commands | Slash and context commands, typed primitive options, annotation-driven routing and autocomplete |
| Interactions | Buttons, selects, modals, sliders, ratings and deferred responses |
| Events | Typed listeners, annotation-based subscriptions, priorities and asynchronous dispatch |
| Entities | Users, servers, channels, members, roles, messages and local caching |
| Permissions | Permission and role checks, owner restrictions, cooldowns and custom preconditions |
| Rich messages | Embeds, file uploads, attachments, layouts and interactive components |
| Application tools | Presence, direct messages, scheduled messages, panels and event webhooks |
| Voice | Observed voice states, voice events and voice token requests |
| Reliability | Gateway health metrics, REST statistics and partitioned local task execution |

Native voice audio transport, gateway sharding and a complete inbound HTTP interaction server are not currently implemented.

## Getting Started

### Requirements

- Java Development Kit **25 or newer**
- Gradle or Maven
- An Onyx application token and the required application scopes
- A test server where your application is installed

### Installation

Onyx4J is distributed through its own [Maven repository](https://repo.onyx4j.de).

**Gradle — Kotlin DSL**

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.onyx4j.de/releases")
}

dependencies {
    implementation("de.onyx4j:onyx4j:1.0.0-RC2")
}
```

**Gradle — Groovy DSL**

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri('https://repo.onyx4j.de/releases')
    }
}

dependencies {
    implementation 'de.onyx4j:onyx4j:1.0.0-RC2'
}
```

**Maven**

```xml
<repositories>
    <repository>
        <id>onyx4j</id>
        <url>https://repo.onyx4j.de/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>de.onyx4j</groupId>
        <artifactId>onyx4j</artifactId>
        <version>1.0.0-RC2</version>
    </dependency>
</dependencies>
```

Configure your build tool to compile and run with Java 25 or newer. The release includes the library, sources, JavaDocs, dependency metadata and checksums.

### Your first bot

Set the `ONYX_BOT_TOKEN` environment variable to your application token and register a simple slash command:

```java
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.OnyxClientBuilder;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.annotation.SlashCommand;

public class Bot {
    public static void main(String[] args) {
        OnyxClient client = OnyxClientBuilder
                .createDefault(System.getenv("ONYX_BOT_TOKEN"))
                .addCommandHandlers(new Bot())
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(client::close));
        client.awaitReady();
    }

    @SlashCommand(name = "ping", description = "Check if the bot is responding")
    public void ping(SlashCommandInteractionEvent event) {
        event.reply("Pong!").queue();
    }
}
```

Run your application and invoke `/ping` in your test server.

> [!WARNING]
> Automatic command synchronization replaces the registered command list. Register your complete intended command set, use a dedicated test application, or disable synchronization with `setAutoSyncCommands(false)`.

### Asynchronous REST

REST actions are lazy. Use `queue()` to execute an action asynchronously and handle the returned future.

```java
client.retrieveUserById(userId).queue()
        .thenAccept(user -> System.out.println(user.username()))
        .exceptionally(error -> {
            System.err.println("User lookup failed");
            return null;
        });
```

Use `map()` and `flatMap()` to compose actions. Avoid blocking calls such as `complete()` in gateway event handlers. Repeated execution of a REST action may send another request.

## Documentation

| Resource | Link |
| :--- | :--- |
| Official website | [onyx4j.de](https://onyx4j.de) |
| Installation & Quickstart | [Developer documentation](https://docs.onyx4j.de/#quickstart) |
| Java API Reference | [JavaDocs — RC2](https://javadocs.onyx4j.de/1.0.0-RC2/) |
| Releases & Snapshots | [Maven repository](https://repo.onyx4j.de) |
| Infrastructure availability | [Status page](https://status.onyx4j.de) |

## Building from source

Use JDK 25 and Gradle 9.1 or newer.

```bash
gradle clean build
```

On Windows:

```powershell
gradle clean build
```

The build generates the library, sources and JavaDoc JARs in `build/libs/`.

### Testing

RC2 passed 66 local automated tests, including a WebSocket/Engine.IO/Socket.IO fixture covering authentication payloads, heartbeat, event delivery and server-initiated reconnection.

```bash
gradle test
```

For an opt-in live smoke test, set `ONYX_BOT_TOKEN` locally and run:

```bash
gradle liveSmoke
```

The smoke test checks authentication, gateway readiness, installed servers and registered commands without synchronizing commands or sending messages. Use a dedicated test application.

## Releases

Browse [GitHub Releases](https://github.com/Sekuramis/onyx4j/releases) and the [changelog](CHANGELOG.md). Release candidates are marked as pre-releases. Future releases include a `withDependencies` JAR for manual classpath use alongside the standard library, sources and JavaDocs. Use the regular artifact for Maven and Gradle projects. See the [release process](RELEASING.md) for maintainer instructions.

## Release roadmap

| Milestone | Status |
| :--- | :--- |
| Core REST, Gateway and entity APIs | Implemented |
| Commands, interactions and event system | Implemented |
| Permissions, rich messaging and application features | Implemented |
| 1.0.0-RC1 | Completed |
| 1.0.0-RC2 | Published |
| Website, documentation and JavaDocs | Online |
| Complete live integration validation | Pending |
| Continuous integration and secret scanning | Configured |
| Automated GitHub releases and Maven publishing | Configured |
| Automated documentation deployment | Planned |
| 1.0.0 Stable | Planned |

## Feedback & Security

Feedback, bug reports and feature suggestions are welcome. Include the Onyx4J version, Java version, a minimal reproduction and sanitized error output. Use the repository's issue tracker for non-security reports. See [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

Never commit app tokens, publishing credentials or private keys. Report security vulnerabilities privately as described in [SECURITY.md](SECURITY.md).

## License

Copyright © 2026 Sekuramis.

Onyx4J is licensed under the Apache License, Version 2.0 (Apache-2.0).

See the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <a href="https://onyx4j.de"><strong>Onyx4J</strong></a>
  <br />
  Build for Onyx. In Java.
  <br />
  <sub>API &nbsp;·&nbsp; Community &nbsp;·&nbsp; Development</sub>
</div>
