package com.nextbreakpoint;

public interface ForkPromise<T> {
	public ForkResult<T> join();
}
