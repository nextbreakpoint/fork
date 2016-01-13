package com.nextbreakpoint;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ForkPromise<T, E extends Exception> {
	private final Function<Exception, E> mapper;
	private final ForkCollector<T> collector;
	private final List<Future<T>> futures;

	public ForkPromise(Function<Exception, E> mapper, ForkCollector<T> collector, List<Future<T>> futures) {
		this.collector = collector;
		this.futures = futures;
		this.mapper = mapper;
	}

	public Try<List<T>, E> collect() {
		return Try.of(mapper, () -> futures.stream().map(future -> collector.accumulate(collectSingle(future))).collect(Collectors.toList()));
	}

	private T collectSingle(Future<T> future) {
		return Try.of(e -> new RuntimeException(e), () -> future.get()).getOrThrow();
	}
}
