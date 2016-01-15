package com.nextbreakpoint.fork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.nextbreakpoint.Try;

public class ForkTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void join_givenCollectorReturnsUnmodificedValue_shouldReturnSuccess() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> t)).join();
		assertFalse(result.isFailure());
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValue_shouldReturnEmptyOptional() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> t)).join();
		assertEquals("", result.get());
	}

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
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> t)).submit(() -> null).join();
		assertFalse(result.value().isPresent());
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskReturnsString_shouldReturnSuccess() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> t)).submit(() -> "X").join();
		assertFalse(result.isFailure());
	}

	@Test
	public void join_givenCollectorReturnsUnmodificedValueAndTaskReturnsString_shouldReturnSameString_whenSubmittingSingleTask() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> t)).submit(() -> "X").join();
		assertEquals("X", result.value().get());
	}

	@Test
	public void join_givenCollectorConcatenatesValuesAndTaskReturnsString_shouldReturnConcatenatedStrings_whenSubmittingMultipleTasks() {
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> a + t)).submit(() -> "X").submit(() -> "Y").join();
		assertEquals("XY", result.value().get());
	}

	@Test
	public void join_givenCollectorConcatenatesValuesAndTaskReturnsString_shouldReturnConcatenatedStrings_whenSubmittingListOfTasks() {
		List<Callable<String>> tasks = new ArrayList<>();
		tasks.add(() -> "Y");
		tasks.add(() -> "X");
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> a + t)).submit(tasks).join();
		assertEquals("YX", result.value().get());
	}

	@Test
	public void join_givenCollectorConcatenatesValuesAndTaskReturnsString_shouldReturnConcatenatedStrings_whenSubmittingMultipleListOfTasks() {
		List<Callable<String>> tasks1 = new ArrayList<>();
		tasks1.add(() -> "Y");
		tasks1.add(() -> "X");
		List<Callable<String>> tasks2 = new ArrayList<>();
		tasks2.add(() -> "X");
		tasks2.add(() -> "Y");
		Try<String, Throwable> result = Fork.of(Collectors.reducing("", (a, t) -> a + t)).submit(tasks1).submit(tasks2).join();
		assertEquals("YXXY", result.value().get());
	}
}
