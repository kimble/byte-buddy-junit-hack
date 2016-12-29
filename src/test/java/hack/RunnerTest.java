package hack;

import com.developerb.testfactory.TestGenerator;
import com.developerb.testfactory.TestGeneratorRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;


@RunWith(TestGeneratorRunner.class)
public class RunnerTest implements TestGenerator {

    @Rule
    public TestName testName = new TestName();

    private static boolean beforeClassHookExecuted = false;

    private AtomicReference<Boolean> beforeHookExecuted = new AtomicReference<>(Boolean.FALSE);

    private boolean primitiveFieldUpdatedInBefore = false;


    @Before
    public void init() {
        beforeHookExecuted.set(Boolean.TRUE);
        primitiveFieldUpdatedInBefore = true;
    }

    @BeforeClass
    public static void beforeClass() {
        beforeClassHookExecuted = true;
    }

    @Override
    public void generateTests(BiConsumer<String, GeneratedTest> consumer) {
        consumer.accept("Method annotated with @BeforeClass has been executed", () -> assertTrue(beforeClassHookExecuted));
        consumer.accept("Can update primitive fields from @Before annotated method", () -> assertTrue(primitiveFieldUpdatedInBefore));
        consumer.accept("The @Before hook has been executed", () -> assertTrue(beforeHookExecuted.get()));

        consumer.accept("Generated tests can access rules like TestName", () -> {
            assertNotNull("Test name should not be null - " + testName.hashCode(), testName);
            assertNotNull("Test method name should not be null", testName.getMethodName());
            assertEquals("Test name should be as defined", "Generated tests can access rules like TestName", testName.getMethodName());
        });


        for (int i=0; i<50; i++) {
            consumer.accept(i + " + 1 = " + (i + 1), () -> {
                // Pretending to test..
            });
        }
    }

}
