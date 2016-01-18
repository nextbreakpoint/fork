# Fork

Fork implements a functional API for dealing with threads in Java 8

## Example

Given the program:

	public class ForkMain {
		private static final ServiceOK service1 = new ServiceOK("Y");
		private static final ServiceOK service2 = new ServiceOK("Z");
		private static final ServiceKO service3 = new ServiceKO();
		
		public static void main(String[] args) {
			ExecutorService executor = defaultExecutor();
			
			System.out.println(Fork.of(executor, String.class)
				.submit(() -> service1.doSomething())
				.submit(() -> service2.doSomething())
				.submit(() -> service3.doSomething())
				.collect(Collectors.reducing("X", (a, t) -> a + t), ""));
	
			Fork.of(executor, String.class)
				.submit(() -> service1.doSomething())
				.submit(() -> service2.doSomething())
				.collectOrFail(Collectors.reducing("X", (a, t) -> a + t)).ifPresent(System.out::println);
	
			try {
				Fork.of(executor, String.class)
					.submit(() -> service1.doSomething())
					.submit(() -> service3.doSomething())
					.collectOrFail(Collectors.reducing("X", (a, t) -> a + t)).ifPresentOrThrow(System.out::println);
			} catch (Throwable e) {
				System.out.println(e.getMessage());
			}
			
			executor.shutdown();
		}
	
		private static ExecutorService defaultExecutor() {
			return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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

The output will be:

	XYZ
	XYZ
	java.util.concurrent.ExecutionException: java.lang.Exception: Error
