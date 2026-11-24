# Changelog

Notable changes to Onyx4J are documented here. Releases follow semantic versioning; release candidates may change before the first stable release.

## [Unreleased]

- Automated GitHub releases and optional Maven publishing.
- Release artifact and license verification.
- Repository contribution and security policies.

## [1.0.0-RC2]

### Added

- Expanded REST coverage, context commands and event webhook support.
- Presence, panels, scheduled messages and direct messages.
- Rich message layouts, embeds and file uploads.
- Voice state management and voice token requests.
- Server-partitioned task execution and reliability statistics.
- Annotation-driven event and interaction handling.

### Improved

- Gateway lifecycle, reconnection and REST reliability.
- Command access controls, middleware and cooldown management.
- Local integration test coverage.

### Known limitations

- Full live integration validation is pending.
- Native voice audio transport, gateway sharding and a complete inbound HTTP interaction server are not implemented.

### Installation

```kotlin
implementation("de.onyx4j:onyx4j:1.0.0-RC2")
```

Repository: https://repo.onyx4j.de/releases

## [1.0.0-RC1]

Initial release candidate. Historical release notes are not available in this repository.

[Unreleased]: https://github.com/Sekuramis/onyx4j/compare/v1.0.0-RC2...master
[1.0.0-RC2]: https://github.com/Sekuramis/onyx4j/releases/tag/v1.0.0-RC2
