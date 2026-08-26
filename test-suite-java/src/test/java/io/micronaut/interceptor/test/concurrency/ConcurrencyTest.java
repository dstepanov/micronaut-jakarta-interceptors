package io.micronaut.interceptor.test.concurrency;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interception of one invocation belongs to that invocation: its context data, its parameters and its result
 * are its own, however many threads are intercepting at once.
 */
class ConcurrencyTest {

    private static final int THREADS = 16;
    private static final int PER_THREAD = 250;

    @Test
    void keepsConcurrentInvocationsOfOneBeanApart() throws Exception {
        reset();
        try (ApplicationContext context = ApplicationContext.run()) {
            ConcurrentSingleton bean = context.getBean(ConcurrentSingleton.class);

            run(thread -> () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    String value = thread + "-" + i;
                    assertEquals("echo " + value, bean.echo(value));
                }
                return null;
            });

            assertEquals(List.of(), List.copyOf(IsolationInterceptor.LEAKED));
            assertEquals(1, IsolationInterceptor.INSTANCES.size(),
                "one interceptor instance serves one intercepted object");
        }
    }

    @Test
    void createsAndInterceptsManyBeansAtOnce() throws Exception {
        reset();
        try (ApplicationContext context = ApplicationContext.run()) {
            run(thread -> () -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    String value = thread + "-" + i;
                    assertEquals("echo " + value, context.createBean(ConcurrentPrototype.class).echo(value));
                }
                return null;
            });

            assertEquals(List.of(), List.copyOf(IsolationInterceptor.LEAKED));
            int beans = THREADS * PER_THREAD;
            assertEquals(beans, IsolationInterceptor.CONSTRUCTED.get(), "every construction is intercepted once");
            assertEquals(beans, IsolationInterceptor.CREATED.get(), "every callback is intercepted once");
            assertTrue(IsolationInterceptor.INSTANCES.size() > 1,
                "each of the beans gets an interceptor instance of its own");
        }
    }

    private static void reset() {
        IsolationInterceptor.INSTANCES.clear();
        IsolationInterceptor.LEAKED.clear();
        IsolationInterceptor.CONSTRUCTED.set(0);
        IsolationInterceptor.CREATED.set(0);
    }

    private static void run(java.util.function.IntFunction<Callable<Void>> work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Void>> futures = new ArrayList<>(THREADS);
            for (int thread = 0; thread < THREADS; thread++) {
                futures.add(pool.submit(work.apply(thread)));
            }
            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
