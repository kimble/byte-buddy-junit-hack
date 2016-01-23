package com.developerb.testfactory;

/**
 * We need some sort of intermediary to allow access from the generated test class
 * to the generated tests (typically Java 8 Lambdas).
 */
public class TestDelegate {

    private final TestGenerator.GeneratedTest test;

    public TestDelegate(TestGenerator.GeneratedTest test) {
        this.test = test;
    }

    public void runTest() throws Throwable {
        test.run();
    }

}
