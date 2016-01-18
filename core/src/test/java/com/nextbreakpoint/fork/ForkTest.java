package com.nextbreakpoint.fork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
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
	public void join_givenCollectorReturnsUnmodifiedValueAndIdentityIsEmptyString_shouldReturnEmptyString() {
		String result = Fork.empty(String.class).collect(Collectors.reducing("", (a, t) -> t), "");
		assertEquals("", result);
	}

	@Test
	public void join_givenCollectorReturnsUnmodifiedValueAndTaskSuppliesString_shouldCallSupply() throws Exception {
		@SuppressWarnings("unchecked")
		Callable<Object> task = mock(Callable.class);
		Fork.empty(Object.class).submit(task).collect(Collectors.reducing("", (a, t) -> t), "");
		verify(task, times(1)).call();
	}

	@Test
	public void join_givenCollectorReturnsUnmodifiedValueAndTaskSuppliesString_shouldCallSupplyInNewThread() {
		final Thread mainThread = Thread.currentThread();
		Callable<Object> task = () -> {
			assertTrue(Thread.currentThread() != mainThread);
			return null;
		};
		Fork.empty(Object.class).submit(task).collect(Collectors.reducing("", (a, t) -> t), "");
	}

	@Test
	public void join_givenCollectorReturnsUnmodifiedValueAndTaskThrowsException_shouldReturnFailureValue() {
		String result = Fork.empty(String.class).submit(() -> { throw new Exception(); }).collect(Collectors.reducing("", (a, t) -> t), "X");
		assertEquals("X", result);
	}

	@Test
	public void join_givenCollectorReturnsUnmodifiedValueAndTaskThrowsException_shouldReturnFailure() {
		Try<String, Throwable> result = Fork.empty(String.class).submit(() -> { throw new Exception(); }).collectOrFail(Collectors.reducing("", (a, t) -> t));
		assertTrue(result.isFailure());
	}
	
	@Test
	public void join_givenCollectorReturnsUnmodifiedValueAndTaskReturnsString_shouldReturnSuccess() {
		Try<String, Throwable> result = Fork.empty(String.class).submit(() -> "X").collectOrFail(Collectors.reducing("", (a, t) -> t));
		assertFalse(result.isFailure());
	}

	@Test
	public void join_givenCollectorReturnsUnmodifiedValueAndTaskReturnsString_shouldReturnSameString_whenSubmittingSingleTask() {
		Try<String, Throwable> result = Fork.empty(String.class).submit(() -> "X").collectOrFail(Collectors.reducing("", (a, t) -> t));
		assertEquals("X", result.value().get());
	}

	@Test
	public void join_givenCollectorConcatenatesValuesAndTaskReturnsString_shouldReturnConcatenatedStrings_whenSubmittingMultipleTasks() {
		Try<String, Throwable> result = Fork.empty(String.class).submit(() -> "X").submit(() -> "Y").collectOrFail(Collectors.reducing("", (a, t) -> a + t));
		assertEquals("XY", result.value().get());
	}

	@Test
	public void join_givenCollectorConcatenatesValuesAndTaskReturnsString_shouldReturnConcatenatedStrings_whenSubmittingListOfTasks() {
		List<Callable<String>> tasks = new ArrayList<>();
		tasks.add(() -> "Y");
		tasks.add(() -> "X");
		Try<String, Throwable> result = Fork.empty(String.class).submit(tasks).collectOrFail(Collectors.reducing("", (a, t) -> a + t));
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
		Try<String, Throwable> result = Fork.empty(String.class).submit(tasks1).submit(tasks2).collectOrFail(Collectors.reducing("", (a, t) -> a + t));
		assertEquals("YXXY", result.value().get());
	}
}
