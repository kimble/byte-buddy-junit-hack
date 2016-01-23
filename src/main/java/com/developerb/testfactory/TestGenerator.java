package com.developerb.testfactory;

import java.util.function.BiConsumer;

/**
 * A test that is able to generate tests dynamically at runtime.
 */
public interface TestGenerator {

    /**
     * Register tests by invoking the consumer.
     * Remember that adding a test will create a Java method behind the
     * scenes to the name will have to adhere to the rules of the JVM.
     *
     * @param consumer Used to register tests
     */
    void generateTests(BiConsumer<String, GeneratedTest> consumer);


    /**
     * Convenience for Java 8. It would be convenient to use Runnable
     * instead of introducing a new interface, but Runnable does not
     * allow checked exceptions to be thrown.
     */
    @FunctionalInterface
    interface GeneratedTest {

        void run() throws Throwable;

    }

}
