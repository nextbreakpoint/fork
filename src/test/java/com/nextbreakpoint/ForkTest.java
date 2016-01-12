package com.nextbreakpoint;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ForkTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void join_givenSequence() {
		ForkResult<String> result = Fork.of((String x) -> {}).sequence(() -> "X").join();
		assertEquals("X", result.value().get());
	}
}
