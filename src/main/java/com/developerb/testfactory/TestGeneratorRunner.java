package com.developerb.testfactory;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test runner allowing tests to be generated at runtime.
 */
public class TestGeneratorRunner extends BlockJUnit4ClassRunner {

    /**
     * Instead of passing the class to the default runner implementation we run it trough
     * Byte Buddy to create a new class inheriting this one, but with @Test annotated methods
     * generated at runtime.
     *
     * @param testClass The one annotated with @RunWith(TestGeneratorRunner.class)
     * @throws Exception
     */
    public TestGeneratorRunner(Class<?> testClass) throws Exception {
        super(wrap(testClass));
    }

    /**
     * This is a not-so elegant way of solving a tricky problem. The default JUnit behaviour
     * is to generate new instances of the test class before invoking any given test. This behaviour
     * causes problems when generated tests has references to fields in the TestGenerator class.
     * Any such test will keep a reference to the instance that generated the test and not the
     * instance initialized by JUnit when the test is run.
     *
     * In order to fix this problem I'm breaking JUnit semantics and force all generated tests
     * to run in the instance of TestGenerator that created the test in the first place.
     */
    private static final Map<Class<?>, Object> instanceCache = new ConcurrentHashMap<>();


    @SuppressWarnings("unchecked")
    private static Class<?> wrap(Class<?> testClass) throws IllegalAccessException, InstantiationException {
        if (!TestGenerator.class.isAssignableFrom(testClass)) {
            throw new InstantiationException(testClass + " must implement " + TestGenerator.class);
        }

        Class<TestGenerator> generatorClass = (Class<TestGenerator>) testClass;
        TestFactory factory = new TestFactory(generatorClass);
        TestGenerator instance = createInstance(generatorClass);

        instance.generateTests((name, generatedTest) -> factory.createTestMethod(name, generatedTest::run));

        return factory.buildClass();
    }

    private static TestGenerator createInstance(Class<TestGenerator> generatorClass) throws InstantiationException, IllegalAccessException {
        TestGenerator instance = generatorClass.newInstance();
        instanceCache.put(generatorClass, instance);

        return instance;
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            super.run(notifier);
        }
        finally {
            // The test class will be a dynamically generated class inheriting from
            // the TestGenerator implementation
            Class<?> key = getTestClass().getJavaClass().getSuperclass();
            instanceCache.remove(key);
        }
    }

    @Override
    protected Description describeChild(FrameworkMethod method) {
        TestName nameAnnotation = method.getAnnotation(TestName.class);
        return Description.createTestDescription(method.getDeclaringClass(), nameAnnotation.value());
    }

    /**
     * It would be really convenient if just could return the instance stored in
     * instanceCache directly, but that will blow up when in the Java reflection API
     * when it attempts to invoke the test methods on a different class.
     *
     * Remember that the instance stored in the static cache is an instance of the
     * unmodified class responsible for generating the tests. The test methods are
     * implemented in a dynamically generated class inheriting from the first.
     *
     * Instead we have to jump through some hoops and copy all fields from the first
     * instance over into the dynamically generated one. This works because the generated
     * class inherits the class implementing the TestGenerator interface.
     *
     * @return a test instance ready to be executed
     * @throws Exception
     */
    @Override
    protected Object createTest() throws Exception {
        Object testInstance = super.createTest();
        copyFields(testInstance);

        return testInstance;
    }


    /**
     * Any generating tests referring to fields in the @RunWith(FactoryRunner.class) annotated
     * class will keep referring to that instance when executed and not to the instance of the
     * generated class.
     *
     * This mega hack iterates all the fields and copy them over to the instance that actually
     * is executed by JUnit.
     */
    private void copyFields(Object testInstance) throws IllegalAccessException {

        // The test class will be a dynamically generated class inheriting from
        // the TestGenerator implementation
        Class<?> type = testInstance.getClass().getSuperclass();
        Object hack = instanceCache.get(type);

        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(testInstance, field.get(hack));
        }

    }


    /**
     * This is the fun part.. because @Before and @After annotated methods will run in
     * an instance of the generated class we have to copy any changes made to fields
     * in that instance back into the original instance before executing the test because
     * generated tests (typically Lambdas) will reference fields in the original instance.
     *
     * We accomplish this by decorating the Statement that is actually invoking the test
     * method (in our case an instance of TestDelegate).
     */
    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object testInstance) {
        Statement methodInvokerStatement = super.methodInvoker(method, testInstance);

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                copyFieldsBack(testInstance);
                methodInvokerStatement.evaluate();
            }

            private void copyFieldsBack(Object testInstance) throws IllegalAccessException {
                Class<?> key = testInstance.getClass().getSuperclass();
                Object originalInstance = instanceCache.get(key);

                for (Field field : key.getDeclaredFields()) {
                    field.setAccessible(true);
                    field.set(originalInstance, field.get(testInstance));
                }
            }

        };
    }

}
