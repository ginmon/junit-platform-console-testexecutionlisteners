# junit-platform-console-testexecutionlisteners

Console-style test output for Maven and Surefire, plus a plain-text report that agents can read.

It ports the JUnit Platform Console launcher output into `TestExecutionListener`s. Add the library as a test dependency and the listeners register themselves through `META-INF/services`. Nothing else is needed in your repository.

```xml
<dependency>
  <groupId>io.github.rkraneis</groupId>
  <artifactId>junit-platform-console-testexecutionlisteners</artifactId>
  <version>${version}</version>
  <scope>test</scope>
</dependency>
```

---

## Listeners at a glance

| Listener | Output | Use it for | Enabled by default |
|---|---|---|---|
| `AgentReportingListener` | `target/junit-agent-report.txt` + one stdout line | Agents and CI: failures without log scraping | **Yes** |
| `TreePrintingListener` | Unicode or ASCII test tree on stdout | Humans reading a run | **Yes locally, off when `CI` is set** |
| `FlatPrintingListener` | One line per event | Logs that get grepped | Opt-in (service file) |
| `VerboseTreePrintingListener` | Tree with status, duration, tags, source | Debugging one test run | Opt-in (service file) |

Only `AgentReportingListener` and `TreePrintingListener` register automatically. The flat and verbose listeners are for consumers who want them. Add them to your own `META-INF/services/org.junit.platform.launcher.TestExecutionListener` if you need them.

---

## Agent report

Agents usually have to scrape Maven output to find the failing test. The agent report gives them the answer in a fixed format.

**Where:** `target/junit-agent-report.txt`, with the path printed on stdout:

```
[agent-report] status=PASSED total=9 passed=9 failed=0 aborted=0 skipped=0 report=/path/to/target/junit-agent-report.txt
```

**Format:** one record per line, `KEY=value` pairs, every free-text value double-quoted and escaped (`\"`, `\\`, `\n`, `\t`, `\r`). No colours, no emoji.

| Record | When | Fields |
|---|---|---|
| `RESULT` | always, first line | `status` (`PASSED` or `FAILED`), `total`, `passed`, `failed`, `aborted`, `skipped` |
| `FAILURE` | a test failed | `id`, `name`, `class`, `method`, `exception`, `message`, `location`, `at` |
| `ABORTED` | a test was aborted | same as `FAILURE` |
| `SKIPPED` | a test was skipped | `id`, `name`, `class`, `method`, `reason` |

`location` is the first stack frame in the failing test class, or the first frame outside the JDK, JUnit and this library. `at` is the full frame.

### Example: all green

```
RESULT status=PASSED total=9 passed=9 failed=0 aborted=0 skipped=0
```

### Example: one failure (real output)

Produced by running a deliberately failing fixture. The `message` and `reason` values keep their `%` and quotes, escaped where needed.

```
RESULT status=FAILED total=3 passed=1 failed=1 aborted=0 skipped=1
FAILURE id="[engine:junit-jupiter]/[class:...ReportFixture]/[method:fails()]" name="fails()" class="...ReportFixture" method="fails" exception="org.opentest4j.AssertionFailedError" message="expected three, coverage was 100% and %n stays literal ==> expected: <3> but was: <4>" location="ReportFixture.java:20" at="...ReportFixture.fails(ReportFixture.java:20)"
SKIPPED id="[engine:junit-jupiter]/[class:...ReportFixture]/[method:skipped()]" name="skipped()" class="...ReportFixture" method="skipped" reason="skip reason with 50% and \"quotes\""
```

Read it like this: `RESULT` tells you whether to act. Each `FAILURE` line names the file and line to open. Class names are shortened with `...` here for width; the report has the full names.

### Reading it as an agent

```sh
# Did the run pass?
rg -q '^RESULT status=PASSED' target/junit-agent-report.txt && echo green

# Which tests failed, and where?
rg '^FAILURE ' target/junit-agent-report.txt | rg -o 'class="[^"]+" method="[^"]+".*location="[^"]+"'

# Only the failure message, no stack trace noise
rg '^FAILURE ' target/junit-agent-report.txt | rg -o 'message="(?:[^"\\]|\\.)*"'
```

Each `FAILURE` line has everything needed to find the fix, so an agent does not need the Maven log.

---

## Tree report

The tree is for people. It uses Unicode by default, and ASCII is available.

### Example: passing run

```
╷
├─ JUnit Jupiter ✔
│  └─ ReportSettingsTest ✔
│     ├─ agentReportIsOnByDefault() ✔
│     ├─ agentReportCanBeDisabledByEnvironment() ✔
│     ├─ treeReportIsOffWhenCiIsSet() ✔
│     └─ treeReportIsOnLocally() ✔
```

### Example: failing test (real output)

Failed nodes show the exception message, then `at` with the file and line, then the test `id`. Paste the `id`'s method name into `-Dtest=` to rerun just that test.

```
├─ ReportFixture ✘
│  ├─ fails() ✘ expected three, coverage was 100% and %n stays literal ==> expected: <3> but was: <4>
│  │     at ReportFixture.java:20
│  │     id [engine:junit-jupiter]/[class:...ReportFixture]/[method:fails()]
│  └─ skipped() ↷ skip reason with 50% and "quotes"
```

### Animation: the tree printing as the run goes

GitHub cannot play animations in a README, so each frame is shown below in order. The tree builds up node by node as tests finish. Each frame is what is on screen at that moment.

<details>
<summary>Show the frames (click to expand)</summary>

```
frame 1   ╷
frame 2   ╷
          ├─ JUnit Jupiter
frame 3   ╷
          ├─ JUnit Jupiter
          │  └─ ReportSettingsTest
frame 4   ╷
          ├─ JUnit Jupiter
          │  └─ ReportSettingsTest
          │     ├─ agentReportIsOnByDefault() ✔
frame 5   ╷
          ├─ JUnit Jupiter
          │  └─ ReportSettingsTest
          │     ├─ agentReportIsOnByDefault() ✔
          │     ├─ agentReportCanBeDisabledByEnvironment() ✔
frame 6   ╷
          ├─ JUnit Jupiter
          │  └─ ReportSettingsTest
          │     ├─ agentReportIsOnByDefault() ✔
          │     ├─ agentReportCanBeDisabledByEnvironment() ✔
          │     ├─ treeReportIsOffWhenCiIsSet() ✔
          │     └─ treeReportIsOnLocally() ✔
frame 7   (run finished, tree printed in full, same as the passing example above)
```

</details>

*The frames are a step-by-step reading of the same final tree. The real tree prints once, at the end of the plan, not live.*

---

## Settings

Each reporter resolves in this order: system property, then environment variable, then the default. Accepted values: `true/false`, `1/0`, `on/off`, `yes/no`. Anything else is ignored.

| Reporter | System property | Environment variable | Default |
|---|---|---|---|
| Agent report | `-Djunit.agentReport.enabled=false` | `JUNIT_AGENT_REPORT=0` | on |
| Agent report path | `-Djunit.agentReport.path=build/report.txt` | - | `target/junit-agent-report.txt` |
| Tree report | `-Djunit.tree.enabled=true` | `JUNIT_TREE=on` | on, off when `CI` is set |

### Common setups

```sh
# Local run, tree and agent report (defaults)
mvn verify

# Quiet local run, agent report only
JUNIT_TREE=off mvn verify

# Tree in CI (CI turns it off by default)
JUNIT_TREE=on mvn verify

# No report files at all
JUNIT_AGENT_REPORT=0 JUNIT_TREE=off mvn verify
```

---

## Maven plugin

Surefire runs the listeners with no extra configuration. The agent report lives in `target/`, next to the surefire reports. Failsafe is not verified by this repository's tests.

---

## Behaviour notes

- **The agent report has no colour codes.** The tree does use ANSI colours. Read the agent report, not the tree, from a program.
- **The tree prints once, at the end.** It is not a live stream.
- **The agent report is written even when tests fail.** A failing build still gives you the report.
- **Overwrite.** Each run replaces the previous `junit-agent-report.txt`.

---

## Version and compatibility

- Java 21
- JUnit Platform 6.1.3 (launcher and engine)

## Contributing

Build with `mvn verify`. Tasks are also available via `mise run verify`, since `mise.toml` pins Java 21 and Maven 3.9.16.
