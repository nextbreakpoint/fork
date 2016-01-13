package com.nextbreakpoint;

@FunctionalInterface
public interface ForkTask<T,E extends Exception> {
	public T supply() throws E;
}
