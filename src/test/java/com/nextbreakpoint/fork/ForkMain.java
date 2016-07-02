package com.nextbreakpoint.fork;

import com.nextbreakpoint.Try;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class ForkMain {
	public static void main(String[] args) {
		ExecutorService executor = threadPoolExecutor();

		Fork.of(executor, String.class)
			.submit(() -> service1.doSomething())
			.submit(() -> service2.doSomething())
			.submit(() -> service3.doSomething())
			.stream()
			.filter(Try::isPresent)
			.map(Try::get)
			.reduce((a, t) -> a + t)
			.ifPresent(System.out::println);

		Fork.of(executor, String.class)
			.submit(() -> service1.doSomething())
			.submit(() -> service2.doSomething()) 
			.submit(() -> service3.doSomething())
			.stream()
			.map(result -> result.getOrElse("E"))
			.reduce((a, t) -> a + t)
			.ifPresent(System.out::println);

		Fork.of(executor, String.class)
			.submit(() -> service1.doSomething())
			.submit(() -> service2.doSomething())
			.submit(() -> service3.doSomething())
			.stream()
			.peek(result -> result.ifFailure(handleException()))
			.map(result -> result.isFailure() ? "Failure" : "Success")
			.forEach(System.out::println);

		Fork.of(executor, String.class)
			.submit(() -> service1.doSomething())
			.submit(() -> service2.doSomething())
			.submit(() -> service3.doSomething())
			.stream(exceptionMapper())
			.forEach(result -> result.onFailure(handleIOException()).ifPresent(System.out::println));

		executor.shutdown();
	}

	private static final ServiceOK service1 = new ServiceOK("A");
	private static final ServiceOK service2 = new ServiceOK("B");
	private static final ServiceKO service3 = new ServiceKO();

	private static Consumer<Throwable> handleException() {
		return e -> System.out.println(e.getMessage());
	}

	private static Consumer<IOException> handleIOException() {
		return e -> System.out.println(e.getMessage());
	}

	private static ExecutorService threadPoolExecutor() {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	private static Function<Throwable, IOException> exceptionMapper() {
		return e -> new IOException("IO Error", e);
	}

	private interface Service {
		String doSomething() throws Exception;
	}
	
	private static class ServiceOK implements Service {
		private String value;

		public ServiceOK(String value) {
			this.value = value;
		}

		@Override
		public String doSomething() throws Exception {
			Thread.sleep((long) (Math.random() * 1000));
			return value;
		}
	}
	
	private static class ServiceKO implements Service {
		@Override
		public String doSomething() throws Exception {
			Thread.sleep((long) (Math.random() * 1000));
			throw new Exception("Error");
		}
	}
}