/*
 * This file is part of Fork
 *
 * Copyright (c) 2016-2017, Andrea Medeghini
 * All rights reserved.
 */
package com.nextbreakpoint.fork;

import com.nextbreakpoint.Try;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fork implements a fluent interface for executing parallel tasks and collecting results.
 * 
 * @author Andrea Medeghini
 *
 * @param <T> the type of returned value
 * @param <E> the type of captured exception
 */
public class Fork<T, E extends Exception> {
	private final Function<Exception, E> mapper;
	private final ExecutorService executor;
	private final List<Try<Future<T>, E>> tasks;
	private final Long timeout;
	private final TimeUnit unit;

	/**
	 * Creates a new instance of given executor.
	 * The executor service will be used to execute the submitted tasks. 
	 * @param executor the executor
	 * @param <T> the result type
	 * @return new instance
	 */
	public static <T> Fork<T, Exception> with(ExecutorService executor) {
		return new Fork(executor, defaultMapper(), Collections.emptyList(), null, TimeUnit.SECONDS);
	}

	/**
	 * Submit a task.
	 * @param task the task
	 * @return new instance
	 */
	public Fork<T, E> submit(Callable<T> task) {
		return submit(Collections.singletonList(task));
	}

	/**
	 * Submit a list of tasks.
	 * @param tasks the list of task
	 * @return new instance
	 */
	public Fork<T, E> submit(Callable<T>... tasks) {
		return submit(Arrays.asList(tasks));
	}

	/**
	 * Submit a list of tasks.
	 * @param tasks the list of tasks
	 * @return new instance
	 */
	public Fork<T, E> submit(Collection<Callable<T>> tasks) {
		return new Fork<>(executor, mapper, mergeFutures(this.tasks, submitAll(tasks)), timeout, unit);
	}

	/**
	 * Creates a new instance of given timeout.
	 * @param timeout the timeout
	 * @param unit the unit
	 * @return new instance
	 */
	public Fork<T, E> timeout(Long timeout, TimeUnit unit) {
		return new Fork<>(executor, mapper, tasks, timeout, unit);
	}

	/**
	 * Creates a new instance of given mapper.
	 * @param mapper the mapper
	 * @param <X> the exception type
	 * @return new instance
	 */
	public <X extends Exception> Fork<T, X> mapper(Function<Exception, X> mapper) {
		return new Fork<>(executor, mapper, mapFutures(mapper), timeout, unit);
	}

	/**
	 * Returns a stream of uncompleted tasks.
	 * Elements are returned in order as submitted tasks.
	 * @return new stream
	 */
	public Stream<Try<T, E>> join() {
		return tasks.stream().map(task -> task.flatMap(future -> awaitFuture(future)));
	}

	/**
	 * Awaits for termination and then returns a stream of completed tasks.
	 * Elements are returned in order as submitted tasks.
	 * @return new stream
	 */
	public Stream<Try<T, E>> joinCompleted() {
		return join().map(task -> task.execute()).collect(Collectors.toList()).stream();
	}

	private Try<T, E> awaitFuture(Future<T> future) {
		return Try.of(callFuture(future)).mapper(mapper);
	}

	private Callable<T> callFuture(Future<T> future) {
		return Optional.ofNullable(timeout).<Callable<T>>map(timeout -> () -> future.get(timeout, unit)).orElseGet(() -> () -> future.get());
	}

	private List<Try<Future<T>, E>> submitAll(Collection<Callable<T>> tasks) {
		return tasks.stream().map(task -> Try.of(() -> executor.submit(task)).mapper(mapper).execute()).collect(Collectors.toList());
	}

	private List<Try<Future<T>, E>> mergeFutures(List<Try<Future<T>, E>> list1, List<Try<Future<T>, E>> list2) {
		List<Try<Future<T>, E>> list = new LinkedList<>();
		list.addAll(list1);
		list.addAll(list2);
		return list;
	}

	private <X extends Exception> List<Try<Future<T>, X>> mapFutures(Function<Exception, X> mapper) {
		return tasks.stream().map(result -> result.mapper(mapper)).collect(Collectors.toList());
	}

	private static Function<Throwable, Throwable> defaultMapper() {
		return x -> x;
	}

	private Fork(ExecutorService executor, Function<Exception, E> mapper, List<Try<Future<T>, E>> tasks, Long timeout, TimeUnit unit) {
		this.executor = Objects.requireNonNull(executor);
		this.tasks = Objects.requireNonNull(tasks);
		this.mapper = Objects.requireNonNull(mapper);
		this.unit = Objects.requireNonNull(unit);
		this.timeout = timeout;
	}
}
