package com.nextbreakpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ForkTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskSuppliesString_shouldCallSupply() throws Exception {
		@SuppressWarnings("unchecked")
		ForkTask<String, Exception> task = mock(ForkTask.class);
		Fork.of((String x) -> x).execute(task).collect();
		verify(task, times(1)).supply();
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskSuppliesString_shouldCallSupplyInNewThread() {
		final Thread mainThread = Thread.currentThread();
		ForkTask<String, Exception> task = () -> {
			assertTrue(Thread.currentThread() != mainThread);
			return null;
		};
		Fork.of((String x) -> x).execute(task).collect();
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskThrowsException_shouldReturnFailure() {
		Try<List<String>, Exception> result = Fork.of((String x) -> x).execute(() -> { throw new Exception(); }).collect();
		assertTrue(result.isFailure());
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskSuppliesString_shouldReturnSuppliedString() {
		Try<List<String>, Exception> result = Fork.of((String x) -> x).execute(() -> "X").collect();
		assertEquals("X", result.value().get().get(0));
	}
}
