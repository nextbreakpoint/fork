package com.nextbreakpoint;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Fork<T, E extends Exception> {
	private final Function<Exception, E> mapper;
	private final ForkCollector<T> collector;
	private final ExecutorService executor;

	private Fork(ExecutorService executor, Function<Exception, E> mapper, ForkCollector<T> collector) {
		this.collector = collector;
		this.executor = executor;
		this.mapper = mapper;
	}

	public static <T, E extends Exception> Fork<T, E> of(ExecutorService executor, Function<Exception, E> mapper, ForkCollector<T> collector) {
		return new Fork<T, E>(executor, mapper, collector);
	}

	public static <T, E extends Exception> Fork<T, E> of(Function<Exception, E> mapper, ForkCollector<T> collector) {
		return new Fork<T, E>(defaultExecutor(), mapper, collector);
	}

	public static <T> Fork<T, Exception> of(ForkCollector<T> collector) {
		return new Fork<T, Exception>(defaultExecutor(), defaultMapper(), collector);
	}

	public ForkPromise<T, E> execute(ForkTask<T, E> task) {
		return execute(Collections.singletonList(task));
	}

	public ForkPromise<T, E> execute(List<ForkTask<T, E>> tasks) {
		return new ForkPromise<T, E>(mapper, collector, tasks.stream().map(task -> executor.submit(() -> task.supply())).collect(Collectors.toList()));
	}

	private static ExecutorService defaultExecutor() {
		return Executors.newFixedThreadPool(10);
	}

	private static Function<Exception, Exception> defaultMapper() {
		return x -> x;
	}
}
