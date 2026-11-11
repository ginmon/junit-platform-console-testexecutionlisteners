# AGENTS.md

## Architecture

Maven library (`jar`) that ports JUnit Platform Console launcher output into `TestExecutionListener` implementations. Consumers add the listener via `META-INF/services/org.junit.platform.launcher.TestExecutionListener` during Maven/Surefire runs.

- `src/main/java/io/github/rkraneis/org/junit/platform/console/tasks/`: listeners (`FlatPrintingListener`, `TreePrintingListener`, `VerboseTreePrintingListener`) and printing helpers (`TreePrinter`, `TreeNode`, `PrintWriter`, `Color`).
- `src/main/java/io/github/rkraneis/org/junit/platform/console/options/`: `Theme`.
- Runtime deps: `junit-platform-engine`, `junit-platform-launcher` (1.13.4, managed in `pom.xml`).

## Commands

- Build and test: `mvn verify`
- Compile only: `mvn compile`
- Internal profile (CI): `mvn --activate-profiles ginmon-internal verify`
- Release and deploy run only through Jenkins (`jenkins/release.jenkinsfile`, `jenkins/deploy.jenkinsfile`). Never run them locally.

## Conventions

1. Java 21 (`maven.compiler.release` 21). Use language features available at that level.
2. Keep the `io.github.rkraneis.org.junit.platform.console` package names. Consumers reference them in service files.
3. Public listener class names are API. Do not rename or move them without a major version decision.
4. JUnit versions are pinned in `pom.xml`. Change them only with a matching compatibility check against the Platform launcher API.
5. `target/` is build output. Never edit or commit it.

## Rules

For path-specific rules, refer to the `docs/rules/` directory. Index: `docs/rules/README.md`.
