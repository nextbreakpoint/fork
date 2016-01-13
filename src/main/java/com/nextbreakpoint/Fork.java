package com.nextbreakpoint;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Fork<T, A, R, E extends Exception> {
	private final Function<Exception, E> mapper;
	private final Collector<T,A,R> collector;
	private final ExecutorService executor;
	private final List<Future<T>> futures;

	private Fork(ExecutorService executor, Function<Exception, E> mapper, Collector<T, A, R> collector, List<Future<T>> futures) {
		this.collector = collector;
		this.executor = executor;
		this.futures = futures;
		this.mapper = mapper;
	}

	public static <T, A, R, E extends Exception> Fork<T, A, R, E> of(ExecutorService executor, Function<Exception, E> mapper, Collector<T, A, R> collector) {
		return new Fork<T, A, R, E>(executor, mapper, collector, Collections.emptyList());
	}

	public static <T, A, R, E extends Exception> Fork<T, A, R, E> of(Function<Exception, E> mapper, Collector<T, A, R> collector) {
		return new Fork<T, A, R, E>(defaultExecutor(), mapper, collector, Collections.emptyList());
	}

	public static <T, A, R> Fork<T, A, R, Exception> of(Collector<T, A, R> collector) {
		return new Fork<T, A, R, Exception>(defaultExecutor(), defaultMapper(), collector, Collections.emptyList());
	}

	public Fork<T, A, R, E> execute(TrySupplier<T> task) {
		return execute(Collections.singletonList(task));
	}

	public Fork<T, A, R, E> execute(List<TrySupplier<T>> tasks) {
		return new Fork<T, A, R, E>(executor, mapper, collector, tasks.stream().map(task -> executor.submit(() -> task.supply())).collect(Collectors.toList()));
	}

	private static ExecutorService defaultExecutor() {
		return Executors.newFixedThreadPool(10);
	}

	private static Function<Exception, Exception> defaultMapper() {
		return x -> x;
	}

	public Try<R, E> collect() {
		return Try.of(mapper, () -> futures.stream().map(future -> collectSingle(future)).collect(collector));
	}

	private T collectSingle(Future<T> future) {
		return Try.of(e -> new RuntimeException(e), () -> future.get()).getOrThrow();
	}
}
