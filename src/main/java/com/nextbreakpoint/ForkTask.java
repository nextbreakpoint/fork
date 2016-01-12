package com.nextbreakpoint;

@FunctionalInterface
public interface ForkTask<T> {
	public T supply();
}
