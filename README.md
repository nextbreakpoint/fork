# Fork 1.2.0

Fork implements a fluent API for executing tasks in parallel and collecting results

## Example

Given the program:

	public class ForkMain {
		private static final ServiceOK service1 = new ServiceOK("Y");
		private static final ServiceOK service2 = new ServiceOK("Z");
		private static final ServiceKO service3 = new ServiceKO();
		
		public static void main(String[] args) {
			ExecutorService executor = threadPoolExecutor();
			
			System.out.println(Fork.of(executor, String.class)
				.submit(() -> service1.doSomething())
				.submit(() -> service2.doSomething()) 
				.submit(() -> service3.doSomething())
				.collect(concatenate("X"), "E"));
	
			Fork.of(executor, String.class)
				.submit(() -> service1.doSomething())
				.submit(() -> service2.doSomething())
				.collectOrFail(concatenate("X"))
				.ifPresent(System.out::println);
	
			try {
				Fork.of(executor, String.class)
					.submit(() -> service1.doSomething())
					.submit(() -> service2.doSomething())
					.submit(() -> service3.doSomething())
					.collectOrFail(concatenate("X"))
					.ifPresentOrThrow(System.out::println);
			} catch (Throwable e) {
				System.out.println(e.getMessage());
			}
			
			executor.shutdown();
		}
	
		private static Collector<String, ?, String> concatenate(String identity) {
			return Collectors.reducing(identity, (a, t) -> a + t);
		}
	
		private static ExecutorService threadPoolExecutor() {
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

	XYZE
	XYZ
	java.lang.Exception: Error
