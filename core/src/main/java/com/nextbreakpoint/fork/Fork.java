package com.nextbreakpoint.fork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.nextbreakpoint.Try;

public class Fork<T, E extends Throwable> {
	private final Function<Throwable, E> mapper;
	private final ExecutorService executor;
	private final List<Future<T>> futures;

	private Fork(ExecutorService executor, Function<Throwable, E> mapper, List<Future<T>> futures) {
		this.executor = executor;
		this.futures = futures;
		this.mapper = mapper;
	}

	public static <T, E extends Throwable> Fork<T, E> of(ExecutorService executor, Function<Throwable, E> mapper) {
		return new Fork<T, E>(executor, mapper, Collections.emptyList());
	}

	public static <T, E extends Throwable> Fork<T, E> of(Function<Throwable, E> mapper) {
		return new Fork<T, E>(defaultExecutor(), mapper, Collections.emptyList());
	}

	public static <T> Fork<T, Throwable> of(ExecutorService executor) {
		return new Fork<T, Throwable>(executor, defaultMapper(), Collections.emptyList());
	}

	public static <T> Fork<T, Throwable> empty(Class<T> clazz) {
		return new Fork<T, Throwable>(defaultExecutor(), defaultMapper(), Collections.emptyList());
	}

	private static ExecutorService defaultExecutor() {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	private static Function<Throwable, Throwable> defaultMapper() {
		return x -> x;
	}

	public Fork<T, E> submit(List<Callable<T>> tasks) {
		return new Fork<T, E>(executor, mapper, merge(futures, tasks.stream().map(task -> executor.submit(() -> task.call())).collect(Collectors.toList())));
	}

	private List<Future<T>> merge(List<Future<T>> list1, List<Future<T>> list2) {
		ArrayList<Future<T>> list = new ArrayList<>();
		list.addAll(list1);
		list.addAll(list2);
		return list;
	}

	public Fork<T, E> submit(Callable<T> task) {
		return submit(Collections.singletonList(task));
	}

	public <R, A> R collect(Collector<T, A, R> collector, T failureValue) {
		return futures.stream().map(future -> joinSingle(mapper, future)).map(v -> v.getOrElse(failureValue)).collect(collector);
	}

	public <R, A> Try<R, E> collectOrFail(Collector<T, A, R> collector) {
		try {
			return Try.success(mapper, futures.stream().map(future -> joinSingle(e -> new RuntimeException(e), future)).map(v -> v.getOrThrow()).collect(collector));
		} catch (RuntimeException e) {
			return Try.failure(mapper, mapper.apply(e));
		}
	}

	private <X extends Throwable> Try<T, X> joinSingle(Function<Throwable, X> mapper, Future<T> future) {
		return Try.of(mapper, () -> future.get());
	}

	public void shutdown() {
		executor.shutdown();
	}
}
