package io.github.rkraneis.org.junit.platform.console.tasks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Deliberately failing, skipped and passing tests. Not named *Test so surefire never runs it directly;
 * the tests in this package drive it through a nested launcher.
 */
class ReportFixture {

	@Test
	void passes() {
	}

	@Test
	void fails() {
		assertEquals(3, 4, "expected three, coverage was 100% and %n stays literal");
	}

	@Test
	@Disabled("skip reason with 50% and \"quotes\"")
	void skipped() {
	}

}
