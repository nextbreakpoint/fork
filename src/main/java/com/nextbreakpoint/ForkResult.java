package com.nextbreakpoint;

import java.util.Optional;

public interface ForkResult<T> {
	public Optional<T> value();
}
