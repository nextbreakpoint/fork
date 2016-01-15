package com.nextbreakpoint.fork;

import java.util.stream.Collectors;

public class ForkMain {
	private static final ServiceOK service1 = new ServiceOK("Y");
	private static final ServiceOK service2 = new ServiceOK("Z");
	private static final ServiceKO service3 = new ServiceKO();
	
	public static void main(String[] args) {
		Fork.of(Collectors.reducing("X", (a, t) -> a + t))
			.submit(() -> service1.doSomething())
			.submit(() -> service2.doSomething())
			.join().ifPresent(System.out::println);
		
		try {
			Fork.of(Collectors.reducing("X", (a, t) -> a + t))
				.submit(() -> service1.doSomething())
				.submit(() -> service3.doSomething())
				.join().ifPresentOrThrow(System.out::println);
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
		
		System.exit(0);
	}

	private static interface Service {
		String doSomething() throws Exception;
	}
	
	private static class ServiceOK implements Service {
		private String value;

		public ServiceOK(String value) {
			this.value = value;
		}

		public String doSomething() throws Exception {
			Thread.sleep((long) (Math.random() * 1000));
			return value;
		}
	}
	
	private static class ServiceKO implements Service {
		public String doSomething() throws Exception {
			throw new Exception("Error");
		}
	}
}
