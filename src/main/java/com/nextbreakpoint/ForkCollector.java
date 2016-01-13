package com.nextbreakpoint;

@FunctionalInterface
public interface ForkCollector<T> {
	public T accumulate(T value);
}
