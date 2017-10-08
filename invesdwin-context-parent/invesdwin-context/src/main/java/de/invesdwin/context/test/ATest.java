package de.invesdwin.context.test;

import java.util.List;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import de.invesdwin.context.beans.init.PreMergedContext;
import de.invesdwin.context.beans.init.locations.PositionedResource;
import de.invesdwin.context.log.Log;
import de.invesdwin.context.test.internal.ITestLifecycle;
import de.invesdwin.context.test.internal.LoadTimeWeavingExtension;
import de.invesdwin.context.test.stub.IStub;
import de.invesdwin.util.assertions.Assertions;
import de.invesdwin.util.time.Instant;

/**
 * If a BeanNotFoundException occurs during tests where EntityManagerFactory is not found, then it means that the test
 * should implement APersistenceTest instead of ATest. This is because the persistence module is in classpath.
 * 
 * <p>
 * If you have a test that cannot extend this class, but needs to initialize load time weaving properly, put the
 * following code into the test:
 * </p>
 * 
 * <pre>
 * static {
 *     //initialize load time weaving
 *     Assertions.assertThat(PreMergedContext.getInstance()).isNotNull();
 * }
 * </pre>
 * 
 * WARNING: Aspects will not be woven into this class due to bootstrap limitations. Since the class that initializes the
 * load time weaving is loaded before that, it cannot be woven afterwards. Use inner classes instead for tests of
 * aspects.
 * 
 * @author subes
 * 
 */
@RunWith(JUnitPlatform.class)
@ExtendWith(LoadTimeWeavingExtension.class)
@ContextConfiguration(locations = { TestContextLoader.CTX_DUMMY }, loader = TestContextLoader.class)
@ThreadSafe
//TransactionalTestExecutionListener collides with CTW transactions
@TestExecutionListeners({ DirtiesContextTestExecutionListener.class, DependencyInjectionTestExecutionListener.class })
public abstract class ATest implements ITestLifecycle {

    @GuardedBy("ATest.class")
    private static ATest lastTestClassInstance;
    @GuardedBy("ATest.class")
    private static Instant testClassTimeMeasurement;
    @GuardedBy("ATest.class")
    private static int testClassId;
    @GuardedBy("ATest.class")
    private static int testMethodId;

    protected final Log log = new Log(this);
    protected TestContext ctx;
    private Instant testMethodTimeMeasurement = new Instant();

    static {
        Assertions.assertThat(PreMergedContext.getInstance()).isNotNull();
    }

    @Inject
    private IStub[] hooks;

    public ATest() {
        if (TestContextLoader.getCurrentTest() == null) {
            TestContextLoader.setCurrentTest(this);
        }
    }

    @Override
    public void setUpContextLocations(final List<PositionedResource> contextLocations) throws Exception {
        //ignore
    }

    @Override
    public void setUpContext(final TestContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void setUpOnce() throws Exception {
        synchronized (ATest.class) {
            Assertions.assertThat(testClassTimeMeasurement).isNull();
            testClassTimeMeasurement = new Instant();
            testClassId++;
            log.info("%s) >> [%s] >> running", testClassId, getClass().getName());
        }
        for (final IStub hook : hooks) {
            hook.setUpOnce(this, ctx);
        }
    }

    @BeforeEach
    public final void before(final TestInfo testInfo) throws Exception {
        MockitoAnnotations.initMocks(this);
        synchronized (ATest.class) {
            if (lastTestClassInstance == null) {
                lastTestClassInstance = this;
                setUpOnce();
            }
            testMethodId++;
            log.info("%s.%s) ++ [%s.%s] ++ running", testClassId, testMethodId, getClass().getSimpleName(),
                    testInfo.getDisplayName());
        }
        setUp();
    }

    @Override
    public void setUp() throws Exception {
        for (final IStub hook : hooks) {
            hook.setUp(this, ctx);
        }
        testMethodTimeMeasurement = new Instant();
    }

    @Override
    public void tearDown() throws Exception {
        for (final IStub hook : hooks) {
            hook.tearDown(this, ctx);
        }
    }

    @AfterEach
    public final void after(final TestInfo testInfo) throws Exception {
        synchronized (ATest.class) {
            log.info("%s.%s) -- [%s.%s] -- finished after %s", testClassId, testMethodId, getClass().getSimpleName(),
                    testInfo.getDisplayName(), testMethodTimeMeasurement);
        }
        tearDown();
    }

    @Override
    public void tearDownOnce() throws Exception {
        for (final IStub hook : hooks) {
            hook.tearDownOnce(this);
        }
        TestContextLoader.setCurrentTest(null);
        synchronized (ATest.class) {
            Assertions.assertThat(testClassTimeMeasurement).isNotNull();
            log.info("%s) << [%s] << finished after %s", testClassId, getClass().getName(), testClassTimeMeasurement);
            testClassTimeMeasurement = null;
            testMethodId = 0;
        }
    }

    @AfterAll
    public static synchronized void tearDownOnceStatic() throws Exception {
        if (lastTestClassInstance != null) {
            lastTestClassInstance.tearDownOnce();
            TestContextLoader.setCurrentTest(null);
            lastTestClassInstance = null;
        }
    }

}
