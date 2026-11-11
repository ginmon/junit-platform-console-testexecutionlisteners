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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.function.Function;

/**
 * Switches for the reporters.
 *
 * <p>Precedence per reporter: explicit system property, then environment variable, then the default.
 * Accepted values: {@code true/false}, {@code 1/0}, {@code on/off}, {@code yes/no}. Anything else is ignored.
 *
 * <ul>
 * <li>Agent report: on by default. Off with {@code -Djunit.agentReport.enabled=false} or {@code JUNIT_AGENT_REPORT=0}.</li>
 * <li>Tree report: on locally, off when {@code CI} is set. Override with {@code -Djunit.tree.enabled=...} or {@code JUNIT_TREE=...}.</li>
 * </ul>
 */
final class ReportSettings {

	static final String AGENT_ENABLED_PROPERTY = "junit.agentReport.enabled";
	static final String AGENT_ENABLED_ENV = "JUNIT_AGENT_REPORT";
	static final String AGENT_PATH_PROPERTY = "junit.agentReport.path";
	static final String DEFAULT_AGENT_REPORT_PATH = "target/junit-agent-report.txt";

	static final String TREE_ENABLED_PROPERTY = "junit.tree.enabled";
	static final String TREE_ENABLED_ENV = "JUNIT_TREE";
	static final String CI_ENV = "CI";

	private ReportSettings() {
	}

	static boolean isAgentReportEnabled() {
		return isAgentReportEnabled(System::getProperty, System::getenv);
	}

	static boolean isAgentReportEnabled(Function<String, String> properties, Function<String, String> environment) {
		return resolve(properties.apply(AGENT_ENABLED_PROPERTY), environment.apply(AGENT_ENABLED_ENV), true);
	}

	static boolean isTreeEnabled() {
		return isTreeEnabled(System::getProperty, System::getenv);
	}

	static boolean isTreeEnabled(Function<String, String> properties, Function<String, String> environment) {
		boolean ci = isNotEmpty(environment.apply(CI_ENV));
		return resolve(properties.apply(TREE_ENABLED_PROPERTY), environment.apply(TREE_ENABLED_ENV), !ci);
	}

	static Path agentReportPath() {
		return agentReportPath(System::getProperty);
	}

	static Path agentReportPath(Function<String, String> properties) {
		String configured = properties.apply(AGENT_PATH_PROPERTY);
		return Paths.get(isNotEmpty(configured) ? configured : DEFAULT_AGENT_REPORT_PATH);
	}

	static boolean resolve(String explicitProperty, String environmentValue, boolean defaultValue) {
		Boolean value = parse(explicitProperty);
		if (value == null) {
			value = parse(environmentValue);
		}
		return value == null ? defaultValue : value;
	}

	private static Boolean parse(String value) {
		if (value == null) {
			return null;
		}
		switch (value.trim().toLowerCase(Locale.ROOT)) {
			case "true", "1", "on", "yes":
				return Boolean.TRUE;
			case "false", "0", "off", "no":
				return Boolean.FALSE;
			default:
				return null;
		}
	}

	private static boolean isNotEmpty(String value) {
		return value != null && !value.isEmpty();
	}

}
