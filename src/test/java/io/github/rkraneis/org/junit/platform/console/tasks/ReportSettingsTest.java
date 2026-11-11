package io.github.rkraneis.org.junit.platform.console.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReportSettingsTest {

	private static Map<String, String> env(String... kv) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			map.put(kv[i], kv[i + 1]);
		}
		return map;
	}

	private static boolean agent(Map<String, String> props, Map<String, String> env) {
		return ReportSettings.isAgentReportEnabled(props::get, env::get);
	}

	private static boolean tree(Map<String, String> props, Map<String, String> env) {
		return ReportSettings.isTreeEnabled(props::get, env::get);
	}

	@Test
	void agentReportIsOnByDefault() {
		assertTrue(agent(env(), env()));
	}

	@Test
	void agentReportCanBeDisabledByEnvironment() {
		assertFalse(agent(env(), env("JUNIT_AGENT_REPORT", "0")));
	}

	@Test
	void agentReportPropertyOverridesEnvironment() {
		assertTrue(agent(env("junit.agentReport.enabled", "true"), env("JUNIT_AGENT_REPORT", "off")));
	}

	@Test
	void treeReportIsOnLocally() {
		assertTrue(tree(env(), env()));
	}

	@Test
	void treeReportIsOffWhenCiIsSet() {
		assertFalse(tree(env(), env("CI", "true")));
	}

	@Test
	void treeReportCanBeForcedOnInCi() {
		assertTrue(tree(env(), env("CI", "true", "JUNIT_TREE", "on")));
	}

	@Test
	void treeReportCanBeDisabledLocallyByProperty() {
		assertFalse(tree(env("junit.tree.enabled", "false"), env()));
	}

	@Test
	void unknownValuesAreIgnored() {
		assertTrue(agent(env("junit.agentReport.enabled", "maybe"), env()));
		assertFalse(tree(env(), env("CI", "1", "JUNIT_TREE", "sometimes")));
	}

	@Test
	void reportPathDefaultsToTargetAndHonoursOverride() {
		assertEquals("target/junit-agent-report.txt", ReportSettings.agentReportPath(env()::get).toString());
		assertEquals("custom/r.txt",
			ReportSettings.agentReportPath(env("junit.agentReport.path", "custom/r.txt")::get).toString());
	}

}
