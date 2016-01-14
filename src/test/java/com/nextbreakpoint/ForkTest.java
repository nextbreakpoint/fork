package com.nextbreakpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ForkTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskSuppliesString_shouldCallSupply() throws Exception {
		@SuppressWarnings("unchecked")
		Callable<Object> task = mock(Callable.class);
		Fork.of(Collectors.reducing((a, t) -> t)).submit(task).join();
		verify(task, times(1)).call();
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskSuppliesString_shouldCallSupplyInNewThread() {
		final Thread mainThread = Thread.currentThread();
		Callable<Object> task = () -> {
			assertTrue(Thread.currentThread() != mainThread);
			return null;
		};
		Fork.of(Collectors.reducing((a, t) -> t)).submit(task).join();
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskThrowsException_shouldReturnFailure() {
		Try<Optional<Object>, Throwable> result = Fork.of(Collectors.reducing((a, t) -> t)).submit(() -> { throw new Exception(); }).join();
		assertTrue(result.isFailure());
	}
	
	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskReturnsNull_shouldReturnEmptyOptional() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("Y", (a, t) -> t)).submit(() -> null).join();
		assertFalse(result.value().isPresent());
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskReturnsString_shouldReturnSuccess() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("Y", (a, t) -> t)).submit(() -> "X").join();
		assertFalse(result.isFailure());
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskReturnsString_shouldReturnSameString() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("Y", (a, t) -> t)).submit(() -> "X").join();
		assertEquals("X", result.value().get());
	}
}
