/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package io.github.rkraneis.org.junit.platform.console.tasks;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Writes a plain-text, line-oriented report meant for agents and CI logs.
 *
 * <p>Enabled by default; see {@link ReportSettings} for the switches. The report is written to
 * {@code target/junit-agent-report.txt} (overridable with {@code -Djunit.agentReport.path}).
 *
 * <p>Format: one record per line, {@code KEY=value} pairs, every free-text value double-quoted and
 * escaped (backslash, quote, CR, LF, TAB). Records are {@code RESULT}, {@code FAILURE},
 * {@code ABORTED}, {@code SKIPPED}. Nothing is colored and no emoji are used.
 */
public class AgentReportingListener implements TestExecutionListener {

	private static final List<String> FRAMEWORK_PACKAGES = List.of(
		"java.", "jdk.", "sun.", "org.junit.", "org.opentest4j.", "org.apiguardian.", "io.github.rkraneis.");

	private final boolean enabled;
	private final Path reportPath;
	private final PrintWriter out;

	private final AtomicInteger total = new AtomicInteger();
	private final AtomicInteger passed = new AtomicInteger();
	private final AtomicInteger failed = new AtomicInteger();
	private final AtomicInteger aborted = new AtomicInteger();
	private final AtomicInteger skipped = new AtomicInteger();
	private final List<String> detailLines = new CopyOnWriteArrayList<>();

	public AgentReportingListener() {
		this(ReportSettings.isAgentReportEnabled(), ReportSettings.agentReportPath(), new PrintWriter());
	}

	AgentReportingListener(boolean enabled, Path reportPath, PrintWriter out) {
		this.enabled = enabled;
		this.reportPath = reportPath;
		this.out = out;
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
		if (!enabled || !testIdentifier.isTest()) {
			return;
		}
		total.incrementAndGet();
		Throwable throwable = result.getThrowable().orElse(null);
		switch (result.getStatus()) {
			case SUCCESSFUL -> passed.incrementAndGet();
			case FAILED -> {
				failed.incrementAndGet();
				detailLines.add(record("FAILURE", testIdentifier, throwable, null));
			}
			case ABORTED -> {
				aborted.incrementAndGet();
				detailLines.add(record("ABORTED", testIdentifier, throwable, null));
			}
		}
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (!enabled || !testIdentifier.isTest()) {
			return;
		}
		total.incrementAndGet();
		skipped.incrementAndGet();
		detailLines.add(record("SKIPPED", testIdentifier, null, reason));
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		if (!enabled) {
			return;
		}
		String status = failed.get() > 0 ? "FAILED" : "PASSED";
		String counts = "total=" + total.get() + " passed=" + passed.get() + " failed=" + failed.get()
				+ " aborted=" + aborted.get() + " skipped=" + skipped.get();

		List<String> lines = new ArrayList<>();
		lines.add("RESULT status=" + status + " " + counts);
		lines.addAll(detailLines);

		Path absolute = reportPath.toAbsolutePath().normalize();
		try {
			Optional.ofNullable(absolute.getParent()).ifPresent(parent -> createDirectories(parent));
			Files.write(absolute, lines, UTF_8);
			out.println("[agent-report] status=" + status + " " + counts + " report=" + absolute);
		}
		catch (IOException e) {
			out.println("[agent-report] status=" + status + " " + counts + " report=" + absolute
					+ " WRITE FAILED: " + e.getMessage());
		}
	}

	private static void createDirectories(Path directory) {
		try {
			Files.createDirectories(directory);
		}
		catch (IOException e) {
			// surfaced by the subsequent write, which reports the failure on stdout
		}
	}

	private static String record(String kind, TestIdentifier testIdentifier, Throwable throwable, String reason) {
		String className = "";
		String methodName = "";
		TestSource source = testIdentifier.getSource().orElse(null);
		if (source instanceof MethodSource methodSource) {
			className = nullToEmpty(methodSource.getClassName());
			methodName = nullToEmpty(methodSource.getMethodName());
		}

		StringBuilder sb = new StringBuilder(kind);
		field(sb, "id", testIdentifier.getUniqueId());
		field(sb, "name", testIdentifier.getDisplayName());
		field(sb, "class", className);
		field(sb, "method", methodName);
		if (reason != null) {
			field(sb, "reason", reason);
		}
		if (throwable != null) {
			field(sb, "exception", throwable.getClass().getName());
			field(sb, "message", messageOf(throwable));
			StackTraceElement frame = locate(throwable, className).orElse(null);
			field(sb, "location", frame == null ? "" : frame.getFileName() + ":" + frame.getLineNumber());
			field(sb, "at", frame == null ? "" : frame.toString());
		}
		return sb.toString();
	}

	private static void field(StringBuilder sb, String key, String value) {
		sb.append(' ').append(key).append('=').append(quote(value));
	}

	/**
	 * Prefer the first frame inside the failing test class, otherwise the first frame outside
	 * JDK, JUnit and this library.
	 */
	static Optional<StackTraceElement> locate(Throwable throwable, String testClassName) {
		StackTraceElement[] frames = throwable.getStackTrace();
		if (!testClassName.isEmpty()) {
			for (StackTraceElement frame : frames) {
				if (testClassName.equals(frame.getClassName())) {
					return Optional.of(frame);
				}
			}
		}
		for (StackTraceElement frame : frames) {
			if (!isFramework(frame.getClassName())) {
				return Optional.of(frame);
			}
		}
		return frames.length > 0 ? Optional.of(frames[0]) : Optional.empty();
	}

	private static boolean isFramework(String className) {
		return FRAMEWORK_PACKAGES.stream().anyMatch(className::startsWith);
	}

	private static String messageOf(Throwable throwable) {
		String message = throwable.getMessage();
		return message == null || message.isBlank() ? throwable.toString() : message;
	}

	static String quote(String value) {
		if (value == null) {
			return "\"\"";
		}
		return "\"" + value.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("\t", "\\t") + "\"";
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

}
