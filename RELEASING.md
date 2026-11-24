# Releasing Onyx4J

## Initial setup

Configure the repository Actions secrets `REPOSILITE_USERNAME` and `REPOSILITE_PASSWORD` using a dedicated least-privilege publishing account. Do not add credentials to the repository. Configure Reposilite to reject redeployment of release versions where possible.

## New releases

1. Set the new version in `build.gradle.kts` and update version references in the README.
2. Add a version section to CHANGELOG.md with the release notes.
3. Open a pull request and merge it into master after CI and secret scanning pass.
4. Update your local master and push a matching tag, for example:

```bash
git switch master
git pull --ff-only
git tag -a v1.0.0-RC3 -m "Onyx4J 1.0.0-RC3"
git push origin v1.0.0-RC3
```

The Release workflow validates that the tag is on master history, scans fetched Git refs, builds and tests the project, validates the POM and license files, publishes a new Maven version, and creates a GitHub release with binary, source, JavaDoc and withDependencies JARs, POM and SHA-256 checksums. RC tags are pre-releases; stable tags are marked as latest.

## Historical RC2

After merging the release workflow, open Actions → Release → Run workflow on master. Set tag to `v1.0.0-RC2`, enable `backfill_rc2`, and leave `publish_maven` disabled.

This creates a historical pre-release pointing at the original RC2 source commit and links to existing Maven downloads. It does not rebuild or republish RC2 artifacts. The original source commit predates the repository license and release automation; consult the current LICENSE for licensing information.

## RC2 downloads

The RC2 release assets workflow downloads the original binary, sources, JavaDoc and POM from Reposilite, validates the files, generates SHA-256 checksums and attaches them to the existing pre-release. It runs automatically when its workflow file is merged into master and can be rerun manually. Existing assets are compared rather than overwritten. No Maven credentials or publishing are needed.

The withDependencies JAR is generated for future tagged releases using Gradle Shadow. It is a standalone library for manual classpath use, not an executable application. Standard Maven and Gradle consumers should use the normal artifact instead. RC2 is not rebuilt or retroactively bundled.

## Recovery

If Maven publishing succeeded but GitHub release creation failed, verify the published files and tag before manually running the Release workflow on master with the existing tag, both boolean inputs disabled. This rebuilds GitHub assets without republishing Maven; do not assume the rebuilt binaries are byte-identical to earlier artifacts.

If publishing partially failed, inspect Reposilite and resolve the incomplete release manually before attempting any further publication. Never move or reuse an existing released tag or overwrite a release artifact.

Do not publish from feature branches or expose publishing credentials to pull request workflows.
