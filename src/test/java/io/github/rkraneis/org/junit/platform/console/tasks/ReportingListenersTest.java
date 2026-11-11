package io.github.rkraneis.org.junit.platform.console.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.rkraneis.org.junit.platform.console.options.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

class ReportingListenersTest {

	/** Runs {@link ReportFixture} with only the given listeners; auto-registration is off to avoid recursion. */
	private static void runFixture(TestExecutionListener... listeners) {
		Launcher launcher = LauncherFactory.create(LauncherConfig.builder()
			.enableTestExecutionListenerAutoRegistration(false)
			.addTestExecutionListeners(listeners)
			.build());
		launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(ReportFixture.class)).build());
	}

	private static String capture(Runnable action) {
		PrintStream original = System.out;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
		try {
			action.run();
		}
		finally {
			System.setOut(original);
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}

	@Test
	void agentReportCountsAndRecordsAreMachineReadable(@TempDir Path dir) throws Exception {
		Path report = dir.resolve("agent.txt");
		String stdout = capture(() -> runFixture(new AgentReportingListener(true, report, new PrintWriter())));

		List<String> lines = Files.readAllLines(report, StandardCharsets.UTF_8);
		assertEquals("RESULT status=FAILED total=3 passed=1 failed=1 aborted=0 skipped=1", lines.get(0));

		String failure = lines.stream().filter(l -> l.startsWith("FAILURE ")).findFirst().orElseThrow();
		assertTrue(failure.contains("method=\"fails\""), failure);
		assertTrue(failure.contains("exception=\"org.opentest4j.AssertionFailedError\""), failure);
		assertTrue(failure.contains("location=\"ReportFixture.java:"), failure);
		assertTrue(failure.contains("100%"), failure);

		String skipped = lines.stream().filter(l -> l.startsWith("SKIPPED ")).findFirst().orElseThrow();
		assertTrue(skipped.contains("reason=\"skip reason with 50% and \\\"quotes\\\"\""), skipped);

		assertTrue(stdout.contains("[agent-report] status=FAILED"), stdout);
		assertTrue(stdout.contains("report=" + report.toAbsolutePath().normalize()), stdout);
	}

	@Test
	void agentReportIsSilentWhenDisabled(@TempDir Path dir) {
		Path report = dir.resolve("off.txt");
		capture(() -> runFixture(new AgentReportingListener(false, report, new PrintWriter())));
		assertFalse(Files.exists(report));
	}

	@Test
	void verboseTreeSurvivesPercentSignsInMessages() {
		// before the fix, '%' in exception messages was used as a printf format and broke the output
		String stdout = capture(() -> runFixture(
			new VerboseTreePrintingListener(new PrintWriter(), true, 16, Theme.ASCII)));
		assertTrue(stdout.contains("coverage was 100% and %n stays literal"), stdout);
	}

	@Test
	void treeShowsFailureLocationAndUniqueId() {
		String stdout = capture(() -> runFixture(new TreePrintingListener(new PrintWriter(), true, Theme.ASCII)));
		assertTrue(stdout.contains("at ReportFixture.java:"), stdout);
		assertTrue(stdout.contains("id [engine:junit-jupiter]/[class:io.github.rkraneis.org.junit.platform.console.tasks.ReportFixture]/[method:fails()]"),
			stdout);
	}

	@Test
	void treeIsSilentWhenDisabled() {
		String stdout = capture(() -> runFixture(
			new TreePrintingListener(false, new PrintWriter(), true, Theme.ASCII)));
		assertFalse(stdout.contains("ReportFixture"), stdout);
	}

	@Test
	void quoteEscapesSpecialCharacters() {
		assertEquals("\"a\\\"b\\\\c\\nd\\te\\r\"", AgentReportingListener.quote("a\"b\\c\nd\te\r"));
	}

}
