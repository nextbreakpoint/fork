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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
	public void shouldCallTaskInNewThread() throws Exception {
		final CallableAssertion callableAssert = new CallableAssertion(200);
		Callable<String> task = spy(callableAssert);
		fork().submit(task).joinCompleted();
		verify(task, times(1)).call();
	}

	@Test
	public void shouldAwaitForResult() throws Exception {
		final CallableAssertion callableAssertion1 = new CallableAssertion(200);
		assertThat(callableAssertion1.isCompleted(), is(false));
		final Fork<String, Exception> fork1 = fork().submit(callableAssertion1);
		Thread.sleep(100);
		assertThat(callableAssertion1.isCompleted(), is(false));
		fork1.joinCompleted();
		assertThat(callableAssertion1.isCompleted(), is(true));

		final CallableAssertion callableAssertion2 = new CallableAssertion(100);
		assertThat(callableAssertion2.isCompleted(), is(false));
		final Fork<String, Exception> fork2 = fork().submit(callableAssertion2);
		Thread.sleep(200);
		assertThat(callableAssertion2.isCompleted(), is(true));
		fork2.joinCompleted();
		assertThat(callableAssertion2.isCompleted(), is(true));
	}

	@Test
	public void shouldReturnSuccessWhenSingleTaskSubmitted() {
		assertFalse(fork().submit(task()).join().findFirst().get().isFailure());
	}

	@Test
	public void shouldReturnFailureWhenSingleTaskSubmittedAndTaskThrowsException() {
		assertTrue(fork().submit(taskWithException()).join().findFirst().get().isFailure());
	}

	@Test
	public void shouldReturnCountOneWhenSingleTaskSubmitted() {
		assertEquals(1, fork().submit(task()).join().count());
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

	private Callable<String> task() {
		return () -> "X";
	}

	private Callable<String> taskWithDelay(Long timeout, TimeUnit unit) {
		return () -> { Thread.sleep(unit.toMillis(timeout)); return "X"; };
	}

	private Callable<String> taskWithException() {
		return () -> { throw new NullPointerException("<null>"); };
	}

	private Fork<String, Exception> fork() {
		return Fork.with(executor);
	}

	private Collector<String, ?, String> joinCollector() {
		return Collectors.reducing("", (a, t) -> a + t);
	}

	private class CallableAssertion implements Callable<String> {
		private final Thread mainThread = Thread.currentThread();
		private final long delay;
		private volatile boolean completed;

		public CallableAssertion(long delay) {
			this.delay = delay;
		}

		@Override
		public String call() throws Exception {
			Thread.sleep(delay);
			assertTrue(Thread.currentThread() != mainThread);
			completed = true;
			return null;
		}

		public boolean isCompleted() {
			return completed;
		}
	}
}
