package com.nextbreakpoint;

@FunctionalInterface
public interface ForkCollector<T> {
	public void accumulate(T value);
}
