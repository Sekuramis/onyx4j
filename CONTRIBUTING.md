# Contributing to Onyx4J

Thank you for contributing to Onyx4J.

## Getting started

Use JDK 25 and Gradle 9.1 or newer. Fork the repository, create a feature or fix branch from `master`, and build locally:

```bash
gradle clean build
```

## Development guidelines

- Follow the existing Java code style and package structure.
- Keep changes focused and preserve public API compatibility where practical.
- Add or update automated tests and documentation for behavioral changes.
- Use dedicated test applications for live integration testing.
- Never commit tokens, credentials, private keys, local configuration or unsanitized logs.
- Do not add unnecessary dependencies.

## Pull requests

Describe the problem, implementation, tests and any breaking changes. Reference related issues when available. Ensure the build and automated tests pass before requesting review.

For security vulnerabilities, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.

By submitting a contribution, you confirm that you have the right to submit it under the project's [Apache License 2.0](LICENSE).
