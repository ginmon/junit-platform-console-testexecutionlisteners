/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package io.github.rkraneis.org.junit.platform.console.tasks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.rkraneis.org.junit.platform.console.options.Theme;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Prints the test tree to stdout.
 *
 * <p>Enabled locally and disabled when {@code CI} is set. Override with
 * {@code -Djunit.tree.enabled=true|false} or {@code JUNIT_TREE=on|off}; see {@link ReportSettings}.
 *
 * @since 1.0
 */
public class TreePrintingListener implements TestExecutionListener {

	private final boolean enabled;
	private final Map<String, TreeNode> nodesByUniqueId = new ConcurrentHashMap<>();
	private TreeNode root;
	private final TreePrinter treePrinter;

	public TreePrintingListener() {
		this(ReportSettings.isTreeEnabled(), new PrintWriter(), false, Theme.UNICODE);
	}

	TreePrintingListener(PrintWriter out, boolean disableAnsiColors, Theme theme) {
		this(true, out, disableAnsiColors, theme);
	}

	TreePrintingListener(boolean enabled, PrintWriter out, boolean disableAnsiColors, Theme theme) {
		this.enabled = enabled;
		this.treePrinter = new TreePrinter(out, theme, disableAnsiColors);
	}

	private TreeNode addNode(TestIdentifier testIdentifier, Supplier<TreeNode> nodeSupplier) {
		TreeNode node = nodeSupplier.get();
		nodesByUniqueId.put(testIdentifier.getUniqueId(), node);
		testIdentifier.getParentId().map(nodesByUniqueId::get).orElse(root).addChild(node);
		return node;
	}

	private TreeNode getNode(TestIdentifier testIdentifier) {
		return nodesByUniqueId.get(testIdentifier.getUniqueId());
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		if (!enabled) {
			return;
		}
		root = new TreeNode(testPlan.toString());
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		if (!enabled) {
			return;
		}
		treePrinter.print(root);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (!enabled) {
			return;
		}
		addNode(testIdentifier, () -> new TreeNode(testIdentifier));
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (!enabled) {
			return;
		}
		getNode(testIdentifier).setResult(testExecutionResult);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (!enabled) {
			return;
		}
		addNode(testIdentifier, () -> new TreeNode(testIdentifier, reason));
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		if (!enabled) {
			return;
		}
		getNode(testIdentifier).addReportEntry(entry);
	}

}
