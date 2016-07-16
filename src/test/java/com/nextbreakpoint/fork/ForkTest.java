package com.nextbreakpoint.fork;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ForkTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	private ExecutorService executor;
	
	@Before 
	public void setup() {
		executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	@After 
	public void cleanup() {
		executor.shutdown();
	}

	@Test
	public void shouldCallTaskInNewThread() {
		final Thread mainThread = Thread.currentThread();
		Callable<String> task = () -> {
			assertTrue(Thread.currentThread() != mainThread);
			return null;
		};
		fork().submit(taskWithDelay()).join();
	}

	@Test
	public void shouldReturnSuccessWhenSingleTaskSubmitted() {
		assertFalse(fork().submit(taskWithDelay()).join().findFirst().get().isFailure());
	}

	@Test
	public void shouldReturnFailureWhenSingleTaskSubmittedAndTaskThrowsException() {
		assertTrue(fork().submit(taskWithException()).join().findFirst().get().isFailure());
	}

	@Test
	public void shouldReturnCountOneWhenSingleTaskSubmitted() {
		assertEquals(1, fork().submit(taskWithDelay()).join().count());
	}

	@Test
	public void shouldReturnCountTwoWhenSingleListOfTasksSubmitted() {
		List<Callable<String>> tasks = createList("X", "Y");
		assertEquals(2, fork().submit(tasks).join().count());
	}

	@Test
	public void shouldReturnCountFourWhenMultipleListsOfTasksSubmitted() {
		List<Callable<String>> tasks1 = createList("X", "Y");
		List<Callable<String>> tasks2 = createList("Z", "W");
		assertEquals(4, fork().submit(tasks1).submit(tasks2).join().count());
	}

	@Test
	public void shouldConcatenateStringsWhenSingleListOfTasksSubmitted() {
		List<Callable<String>> tasks = createList("X", "Y");
		assertEquals("XY", fork().submit(tasks).join().map(r -> r.get()).collect(joinCollector()));
	}

	@Test
	public void shouldConcatenateStringsWhenMultipleListsOfTasksSubmitted() {
		List<Callable<String>> tasks1 = createList("X", "Y");
		List<Callable<String>> tasks2 = createList("Z", "W");
		assertEquals("XYZW", fork().submit(tasks1).submit(tasks2).join().map(r -> r.get()).collect(joinCollector()));
	}

	@Test
	public void shouldThrowExecutionExceptionWhenTaskThrowsNullPointerException() throws Throwable {
		exception.expect(ExecutionException.class);
		exception.expectMessage("<null>");
		fork().submit(taskWithException()).join().findFirst().get().throwIfFailure();
	}

	@Test
	public void shouldThrowIOExceptionWhenTaskThrowsNullPointerException() throws IOException {
		exception.expect(IOException.class);
		exception.expectMessage("<test>");
		fork().mapper(testMapper()).submit(taskWithException()).join().findFirst().get().throwIfFailure();
	}

	@Test
	public void shouldReturnSuccessWhenNoTimeoutHappens() {
		assertFalse(fork().timeout(2L, TimeUnit.SECONDS).submit(taskWithDelay(1L, TimeUnit.SECONDS)).join().findFirst().get().isFailure());
	}

	@Test
	public void shouldReturnFailureWhenTimeoutHappens() {
		assertTrue(fork().timeout(1L, TimeUnit.SECONDS).submit(taskWithDelay(2L, TimeUnit.SECONDS)).join().findFirst().get().isFailure());
	}

	private List<Callable<String>> createList(String... values) {
		ArrayList<Callable<String>> result = new ArrayList<>();
		for (String value : values) {
			result.add(() -> value);
		}
		return result;
	}

	private Function<Exception, IOException> testMapper() {
		return e -> new IOException("<test>", e);
	}

	private Callable<String> taskWithDelay() {
		return () -> "X";
	}

	private Callable<String> taskWithDelay(Long timeout, TimeUnit unit) {
		return () -> { Thread.sleep(unit.toMillis(timeout)); return "X"; };
	}

	private Callable<String> taskWithException() {
		return () -> { throw new NullPointerException("<null>"); };
	}

	private Fork<String, Exception> fork() {
		return Fork.with(executor).type(String.class);
	}

	private Collector<String, ?, String> joinCollector() {
		return Collectors.reducing("", (a, t) -> a + t);
	}
}
