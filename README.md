# Fork 1.3.0

Fork implements a fluent interface for executing parallel tasks and collecting results.

## Getting binaries

Fork is available in Maven Central Repository, Bintray and GitHub. 

If you are using Maven, add a dependency in your POM:

    <dependency>
        <groupId>com.nextbreakpoint</groupId>
        <artifactId>com.nextbreakpoint.fork</artifactId>
        <version>1.3.0</version>
    </dependency>

If you are using other tools, please consult tool's documentation.
    
## License

Fork is distributed under the terms of BSD 3-Clause License.

    Copyright (c) 2016, Andrea Medeghini
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:
    
    * Redistributions of source code must retain the above copyright notice, this
      list of conditions and the following disclaimer.
    
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    
    * Neither the name of Fork nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Complete example

Given the program:

    public class ForkMain {
        public static void main(String[] args) {
            ExecutorService executor = threadPoolExecutor();
    
            Fork.with(executor).type(String.class)
                .submit(() -> doSomething("A"))
                .submit(() -> doSomething("B"))
                .submit(() -> alwaysFail())
                .join()
                .filter(Try::isPresent)
                .map(Try::get)
                .reduce((a, t) -> a + t)
                .ifPresent(System.out::println);
    
            Fork.with(executor).type(String.class)
                .submit(() -> doSomething("A"))
                .submit(() -> doSomething("B"))
                .submit(() -> alwaysFail())
                .join()
                .map(result -> result.orElse("E"))
                .reduce((a, t) -> a + t)
                .ifPresent(System.out::println);
    
            Fork.with(executor).type(String.class)
                .submit(() -> doSomething("A"))
                .submit(() -> doSomething("B"))
                .submit(() -> alwaysFail())
                .join()
                .peek(result -> result.ifFailure(handleException()))
                .map(result -> result.map(s -> "Success").orElse("Failure"))
                .forEach(System.out::println);
    
            Fork.with(executor).type(String.class)
                .submit(() -> doSomething("A"))
                .submit(() -> doSomething("B"))
                .submit(() -> alwaysFail())
                .mapper(exceptionMapper())
                .join()
                .forEach(result -> result.ifFailure(handleIOException()));
    
            Fork.with(executor).type(String.class)
                .submit(() -> doSomething("A"))
                .submit(() -> doSomething("B"))
                .submit(() -> alwaysFail())
                .join()
                .filter(Try::isPresent)
                .map(result -> result.get())
                .filter(value -> "A".equals(value))
                .forEach(System.out::println);
    
            Fork.with(executor).type(String.class)
                .submit(() -> doSomething("A"), () -> doSomething("B"), () -> alwaysFail())
                .timeout(200L, TimeUnit.MILLISECONDS)
                .join()
                .filter(Try::isFailure)
                .mapToInt(result -> 1)
                .reduce((x, y) -> x + y)
                .ifPresent(System.out::println);
    
            executor.shutdown();
        }
    
        public static String doSomething(String value) throws Exception {
            sleep();
            return value;
        }
    
        public static String alwaysFail() throws Exception {
            sleep();
            throw new Exception("Error");
        }
    
        private static void sleep() throws InterruptedException {
            Thread.sleep((long) (Math.random() * 1000) + 500);
        }
    
        private static Function<Exception, IOException> exceptionMapper() {
            return e -> new IOException("IO Error", e);
        }
    
        private static Consumer<Exception> handleException() {
            return e -> System.out.println(e.getMessage());
        }
    
        private static Consumer<IOException> handleIOException() {
            return e -> System.out.println(e.getMessage());
        }
    
        private static ExecutorService threadPoolExecutor() {
            return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
    }

The output will be:

    AB
    ABE
    Success
    Success
    java.lang.Exception: Error
    Failure
    IO Error
    A
    3
