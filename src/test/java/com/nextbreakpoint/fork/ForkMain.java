package com.nextbreakpoint.fork;

import com.nextbreakpoint.Try;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ForkMain {
	public static void main(String[] args) {
		ExecutorService executor = threadPoolExecutor();

		Fork.of(executor, String.class)
			.submit(() -> doSomething("A"))
			.submit(() -> doSomething("B"))
			.submit(() -> alwaysFail())
			.stream()
			.filter(Try::isPresent)
			.map(Try::get)
			.reduce((a, t) -> a + t)
			.ifPresent(System.out::println);

		Fork.of(executor, String.class)
			.submit(() -> doSomething("A"))
			.submit(() -> doSomething("B"))
			.submit(() -> alwaysFail())
			.stream()
			.map(result -> result.orElse("E"))
			.reduce((a, t) -> a + t)
			.ifPresent(System.out::println);

		Fork.of(executor, String.class)
			.submit(() -> doSomething("A"))
			.submit(() -> doSomething("B"))
			.submit(() -> alwaysFail())
			.stream()
			.peek(result -> result.ifFailure(handleException()))
			.map(result -> result.map(s -> "Success").orElse("Failure"))
			.forEach(System.out::println);

		Fork.of(executor, String.class)
			.submit(() -> doSomething("A"))
			.submit(() -> doSomething("B"))
			.submit(() -> alwaysFail())
			.mapper(exceptionMapper())
			.stream()
			.forEach(result -> result.ifFailure(handleIOException()));

		Fork.of(executor, String.class)
			.submit(() -> doSomething("A"))
			.submit(() -> doSomething("B"))
			.submit(() -> alwaysFail())
			.stream()
			.filter(Try::isPresent)
			.map(result -> result.get())
			.filter(value -> "A".equals(value))
			.forEach(System.out::println);

		Fork.of(executor, String.class)
			.submit(() -> doSomething("A"))
			.submit(() -> doSomething("B"))
			.submit(() -> alwaysFail())
			.timeout(200L, TimeUnit.MILLISECONDS)
			.stream()
			.filter(Try::isFailure)
			.mapToInt(result -> 1)
			.reduce((x, y) -> x + y)
			.ifPresent(System.out::println);

		executor.shutdown();
	}

	public static String doSomething(String value) throws Exception {
		sleep();
		return value;
	}

	public static String alwaysFail() throws Exception {
		sleep();
		throw new Exception("Error");
	}

	private static void sleep() throws InterruptedException {
		Thread.sleep((long) (Math.random() * 1000) + 500);
	}

	private static Function<Exception, IOException> exceptionMapper() {
		return e -> new IOException("IO Error", e);
	}

	private static Consumer<Exception> handleException() {
		return e -> System.out.println(e.getMessage());
	}

	private static Consumer<IOException> handleIOException() {
		return e -> System.out.println(e.getMessage());
	}

	private static ExecutorService threadPoolExecutor() {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
}
