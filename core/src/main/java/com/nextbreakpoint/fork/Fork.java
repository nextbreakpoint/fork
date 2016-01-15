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

public class Fork<T, A, R, E extends Throwable> {
	private final Function<Throwable, E> mapper;
	private final Collector<T,A,R> collector;
	private final ExecutorService executor;
	private final List<Future<T>> futures;

	private Fork(ExecutorService executor, Function<Throwable, E> mapper, Collector<T, A, R> collector, List<Future<T>> futures) {
		this.collector = collector;
		this.executor = executor;
		this.futures = futures;
		this.mapper = mapper;
	}

	public static <T, A, R, E extends Throwable> Fork<T, A, R, E> of(ExecutorService executor, Function<Throwable, E> mapper, Collector<T, A, R> collector) {
		return new Fork<T, A, R, E>(executor, mapper, collector, Collections.emptyList());
	}

	public static <T, A, R, E extends Throwable> Fork<T, A, R, E> of(Function<Throwable, E> mapper, Collector<T, A, R> collector) {
		return of(defaultExecutor(), mapper, collector);
	}

	public static <T, A, R> Fork<T, A, R, Throwable> of(Collector<T, A, R> collector) {
		return of(defaultMapper(), collector);
	}

	private static ExecutorService defaultExecutor() {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	private static Function<Throwable, Throwable> defaultMapper() {
		return x -> x;
	}

	public Fork<T, A, R, E> submit(List<Callable<T>> tasks) {
		return new Fork<T, A, R, E>(executor, mapper, collector, merge(futures, tasks.stream().map(task -> executor.submit(() -> task.call())).collect(Collectors.toList())));
	}

	private List<Future<T>> merge(List<Future<T>> list1, List<Future<T>> list2) {
		ArrayList<Future<T>> list = new ArrayList<>();
		list.addAll(list1);
		list.addAll(list2);
		return list;
	}

	public Fork<T, A, R, E> submit(Callable<T> task) {
		return submit(Collections.singletonList(task));
	}

	public Try<R, E> join() {
		try {
			return Try.success(mapper, futures.stream().map(future -> joinSingle(future)).collect(collector));
		} catch (ForkException e) {
			return Try.failure(mapper, mapper.apply(e.getCause()));
		}
	}

	private T joinSingle(Future<T> future) {
		return Try.of(e -> new ForkException(e), () -> future.get()).getOrThrow(null);
	}
	
	private static class ForkException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ForkException(Throwable e) {
			super("Task failed", e);
		}
	}
}
