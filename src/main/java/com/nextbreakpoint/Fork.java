package com.nextbreakpoint;

public class Fork<T> {
	private Fork() {
	}
	
	public static <T> Fork<T> of(ForkCollector<T> collector) {
		return new Fork<T>();
	}

	public ForkPromise<T> sequence(ForkTask<T> task) {
		return null;
	}
}
